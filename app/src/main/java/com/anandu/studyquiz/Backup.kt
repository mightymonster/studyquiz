package com.anandu.studyquiz

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Export / Import of the whole studyquiz.json via the Storage Access
 * Framework — no extra permissions needed, user picks where the file goes.
 */
object Backup {
    const val REQ_EXPORT = 4101
    const val REQ_IMPORT = 4102

    fun fileName(): String {
        val ts = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "studyquiz-backup-$ts.json"
    }

    fun export(act: Activity) {
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName())
        }
        act.startActivityForResult(i, REQ_EXPORT)
    }

    fun import(act: Activity) {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        act.startActivityForResult(i, REQ_IMPORT)
    }

    /** Call from onActivityResult. Returns true if the request was ours. */
    fun onResult(act: Activity, requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQ_EXPORT && requestCode != REQ_IMPORT) return false
        if (resultCode != Activity.RESULT_OK) return true
        val uri: Uri = data?.data ?: return true
        try {
            when (requestCode) {
                REQ_EXPORT -> {
                    val json = Store.rawJson(act)
                    act.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(
                        act,
                        Lang.t("Backup saved — keep the file safe!", "ബാക്കപ്പ് സേവ് ചെയ്തു — ഫയൽ സൂക്ഷിച്ച് വെക്കൂ!"),
                        Toast.LENGTH_LONG
                    ).show()
                }
                REQ_IMPORT -> {
                    val json = act.contentResolver.openInputStream(uri)?.use { `in` ->
                        `in`.readBytes().toString(Charsets.UTF_8)
                    } ?: throw IllegalArgumentException("empty")
                    if (Store.importJson(act, json)) {
                        Toast.makeText(
                            act,
                            Lang.t("Restore complete!", "റീസ്റ്റോർ പൂർത്തിയായി!"),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        throw IllegalArgumentException("invalid")
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                act,
                Lang.t("Something went wrong with that file.", "ആ ഫയലിൽ എന്തോ പ്രശ്നമുണ്ട്."),
                Toast.LENGTH_LONG
            ).show()
        }
        return true
    }
}
