package com.anandu.studyquiz

import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.anandu.studyquiz.Ui.dp
import java.io.File
import kotlin.concurrent.thread

/**
 * In-app PDF viewer for bundled notification PDFs (no new dependencies —
 * uses android.graphics.pdf.PdfRenderer). Pages render to bitmaps in a
 * vertical scroll. Toolbar: exam title + Download button (saves to the
 * device Downloads folder via MediaStore on API 29+, public Downloads dir
 * with runtime permission on API 24-28).
 *
 * Extras: "pdf_asset" (asset path), "title", "notif_no".
 */
class PdfViewerActivity : Activity() {

    private var renderer: PdfRenderer? = null
    private var cacheFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)

        val asset = intent.getStringExtra("pdf_asset").orEmpty()
        val title = intent.getStringExtra("title").orEmpty()
        if (asset.isBlank()) {
            Toast.makeText(
                this,
                Lang.t("Coming soon!", "ഉടൻ വരും!"),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }
        build(title, asset)
    }

    private fun build(title: String, asset: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.bg(this@PdfViewerActivity))
        }

        // Toolbar: back + title + download
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Theme.card(this@PdfViewerActivity))
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        val back = TextView(this).apply {
            text = "‹"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
            setTextColor(Theme.teal())
            setPadding(dp(8), dp(0), dp(12), dp(0))
            isClickable = true
            isFocusable = true
            setOnClickListener { finish() }
        }
        bar.addView(back)
        bar.addView(TextView(this).apply {
            text = title.ifBlank { Lang.t("Notification", "നോട്ടിഫിക്കേഷൻ") }
            setTextColor(Theme.ink(this@PdfViewerActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            maxLines = 2
        })
        val dl = TextView(this).apply {
            text = Lang.t("⬇ Download", "⬇ ഡൗൺലോഡ്")
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Theme.teal())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(10), dp(8), dp(10), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { download(asset) }
        }
        bar.addView(dl)
        root.addView(bar)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(16))
        }
        val progress = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = dp(40) }
        }
        col.addView(progress)
        root.addView(
            ScrollView(this).apply {
                addView(col)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                )
            }
        )
        setContentView(root)

        thread {
            try {
                val f = copyAssetToCache(asset)
                cacheFile = f
                val pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
                val r = PdfRenderer(pfd)
                renderer = r
                val w = (resources.displayMetrics.widthPixels * 1.5f).toInt().coerceAtMost(1600)
                runOnUiThread { col.removeView(progress) }
                for (i in 0 until r.pageCount) {
                    r.openPage(i).use { page ->
                        val h = (w * page.height / page.width.toFloat()).toInt()
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        runOnUiThread {
                            col.addView(ImageView(this).apply {
                                setImageBitmap(bmp)
                                adjustViewBounds = true
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                ).apply { bottomMargin = dp(8) }
                            })
                        }
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        Lang.t("Couldn't open the PDF.", "PDF തുറക്കാൻ കഴിഞ്ഞില്ല."),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    /** PdfRenderer needs a seekable file — copy the asset into cache first. */
    private fun copyAssetToCache(asset: String): File {
        val f = File(cacheDir, "notif_${System.currentTimeMillis()}.pdf")
        assets.open(asset).use { inp ->
            f.outputStream().use { out -> inp.copyTo(out) }
        }
        return f
    }

    private fun downloadName(): String {
        val no = intent.getStringExtra("notif_no").orEmpty()
            .replace("/", "-").replace("\\s+".toRegex(), "")
        return if (no.isNotBlank()) "Notification-$no.pdf" else "Notification.pdf"
    }

    private fun download(asset: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 41
            )
            return
        }
        thread {
            try {
                val name = downloadName()
                val bytes = assets.open(asset).use { it.readBytes() }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                    ) ?: throw IllegalStateException("insert failed")
                    contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                } else {
                    @Suppress("DEPRECATION")
                    val dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    if (!dir.exists()) dir.mkdirs()
                    File(dir, name).writeBytes(bytes)
                }
                runOnUiThread {
                    Toast.makeText(
                        this,
                        Lang.t("Saved to Downloads ✓", "Downloads-ൽ സേവ് ചെയ്തു ✓"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        Lang.t("Download failed.", "ഡൗൺലോഡ് പരാജയപ്പെട്ടു."),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            intent.getStringExtra("pdf_asset")?.let { download(it) }
        } else if (requestCode == 41) {
            Toast.makeText(
                this,
                Lang.t("Storage permission needed to download.", "ഡൗൺലോഡിന് സ്റ്റോറേജ് അനുമതി വേണം."),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        try {
            cacheFile?.delete()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
