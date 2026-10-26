package com.anandu.studyquiz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * Full-screen decorative background (v1.33) — a theme-aware Drawable that
 * translates the user's approved DecorativeBackground / DarkDecorativeBackground
 * composables into classic Views. Paints the theme background first, then the
 * soft mint (light) or dark-green (dark) glow shapes.
 *
 * Set as the background of the content ScrollView in [Ui.screen] so it sits
 * behind all scrollable content, app-wide.
 */
class DecorativeBackgroundDrawable(private val ctx: Context) : Drawable() {

    private val dark = Theme.isDark(ctx)
    private val d = ctx.resources.displayMetrics.density
    private fun px(v: Float): Float = v * d

    private val bgPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Theme.bg(ctx)
    }
    private val circlePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor(if (dark) "#0C2623" else "#E8F7F3")
    }
    private val blobPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor(if (dark) "#0B2421" else "#DDF4ED")
    }
    private val wavePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.parseColor(if (dark) "#0B2421" else "#E8F7F3")
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        if (w <= 0 || h <= 0) return

        // Base theme background.
        canvas.drawRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), bgPaint)

        // Soft mint/dark glow circle, top-right.
        val radius = px(if (dark) 240f else 230f)
        canvas.drawCircle(
            b.left + w * 0.85f,
            b.top + h * 0.35f,
            radius,
            circlePaint
        )

        // Large soft blob, mid-left.
        val blobTop = b.top + h * 0.25f
        canvas.drawOval(
            RectF(
                b.left + px(-120f),
                blobTop,
                b.left + px(-120f + 430f),
                blobTop + px(180f)
            ),
            blobPaint
        )

        // Another wave, lower-right.
        val waveLeft = b.left + w * 0.35f
        val waveTop = b.top + h * 0.48f
        canvas.drawOval(
            RectF(
                waveLeft,
                waveTop,
                waveLeft + px(500f),
                waveTop + px(150f)
            ),
            wavePaint
        )
    }

    override fun setAlpha(alpha: Int) {
        bgPaint.alpha = alpha
        circlePaint.alpha = alpha
        blobPaint.alpha = alpha
        wavePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bgPaint.colorFilter = colorFilter
        circlePaint.colorFilter = colorFilter
        blobPaint.colorFilter = colorFilter
        wavePaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
