package com.anandu.studyquiz

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.anandu.studyquiz.Ui.dp

/**
 * Branded splash: mint → teal gradient, centered app icon, two-tone
 * "StudyQuiz" wordmark in Poppins Bold, tagline in Poppins Regular.
 * Logo follows the app's manual theme (light/dark variant).
 * Shows once per launch (~1.2s), then hands off to MainActivity.
 */
class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // NOTE: no Theme.applyTo here — the manifest SplashTheme already gives
        // this activity its fixed branded look.
        super.onCreate(savedInstanceState)
        Lang.init(this)
        // v1.44 root-cause fix: Theme.init must run before isDark() — this
        // activity never called Theme.applyTo. Manual theme (SharedPreferences)
        // drives the logo choice, not the system -night qualifier.
        Theme.init(this)

        val poppinsBold = loadFont("fonts/Poppins-Bold.ttf", Typeface.BOLD)
        val poppinsRegular = loadFont("fonts/Poppins-Regular.ttf", Typeface.NORMAL)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.splash_bg)
            // Let the gradient flow under a transparent status bar.
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        window.statusBarColor = Color.TRANSPARENT

        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        center.addView(ImageView(this).apply {
            // v1.44: manual theme drives the logo — not the -night qualifier.
            setImageResource(if (Theme.isDark(this@SplashActivity)) R.drawable.splash_logo_dark else R.drawable.splash_logo) // emblem only, no white box
            layoutParams = LinearLayout.LayoutParams(dp(112), dp(112))
        })

        center.addView(TextView(this).apply {
            text = styledBrand()
            typeface = poppinsBold
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 38f)
            isSingleLine = true
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
        })

        center.addView(TextView(this).apply {
            text = Lang.t(
                "Read notes, quiz yourself, track growth.",
                "നോട്ട്സ് വായിക്കൂ, ക്വിസ് ചെയ്യൂ, ഗ്രോത്ത് ട്രാക്ക് ചെയ്യൂ."
            )
            typeface = poppinsRegular
            setTextColor(Color.parseColor("#BFFFFFFF")) // white at ~75%
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            maxLines = 2
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
                leftMargin = dp(32)
                rightMargin = dp(32)
            }
        })

        root.addView(center)
        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1200)
    }

    /** Two-tone wordmark: "Study" dark navy, "Quiz" white. */
    private fun styledBrand(): SpannableString {
        val s = SpannableString("StudyQuiz")
        s.setSpan(
            ForegroundColorSpan(Color.parseColor("#102A43")), 0, 5,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        s.setSpan(
            ForegroundColorSpan(Color.WHITE), 5, 9,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return s
    }

    /** Poppins via assets — works on every API level (minSdk 24 included). */
    private fun loadFont(path: String, fallbackStyle: Int): Typeface {
        return try {
            Typeface.createFromAsset(assets, path)
        } catch (e: Exception) {
            Typeface.create(Typeface.DEFAULT, fallbackStyle)
        }
    }
}
