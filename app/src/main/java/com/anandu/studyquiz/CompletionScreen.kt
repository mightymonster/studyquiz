package com.anandu.studyquiz

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.anandu.studyquiz.Ui.dp

/**
 * Shared "Test Completed!" card for the quiz / mock-test / PYQ result
 * screens (v1.25). Centered card, mint/teal theme, compact.
 *
 * Layout (Anandu's spec):
 *  🎉 Test Completed!
 *  Score  →  78 / 97
 *  Accuracy → 80.4% (color-coded)
 *  Correct N | Wrong N (| Skipped N)
 *  ⏱ Time → 42 min / 1h 05m
 *  [ Review Answers ] [ Practice Wrong Answers ] [ Back to Home ]
 */
object CompletionScreen {

    data class Stats(
        val correct: Int,
        val wrong: Int,
        val skipped: Int,
        val elapsedMs: Long
    ) {
        val total: Int get() = correct + wrong + skipped
        val accuracy: Double get() = if (total <= 0) 0.0 else correct * 100.0 / total
    }

    /** <60 min -> "42 min"; >=60 min -> "1h 05m" (bilingual). */
    fun formatDuration(ms: Long): String {
        val mins = (ms / 60000).toInt()
        return if (mins < 60) Lang.t("%d min", "%d മിനിറ്റ്").format(mins)
        else Lang.t("%dh %02dm", "%d മണിക്കൂർ %02d മിനിറ്റ്").format(mins / 60, mins % 60)
    }

    /** Accuracy color band: >=70% green, 40-69% amber, <40% red. */
    fun accuracyColor(ctx: android.content.Context, acc: Double): Int = when {
        acc >= 70 -> Theme.correct(ctx)
        acc >= 40 -> Color.parseColor("#F59E0B")
        else -> Theme.red(ctx)
    }

    /**
     * Builds the centered completion card.
     * @param subtitle small grey line under the header (topic / paper name).
     * @param onReview null hides the Review button (no review data).
     * @param onPracticeWrong null hides the button (nothing wrong).
     */
    fun card(
        act: Activity,
        subtitle: String,
        stats: Stats,
        onReview: (() -> Unit)?,
        onPracticeWrong: (() -> Unit)?,
        onHome: () -> Unit
    ): LinearLayout {
        val card = Ui.card(act).apply {
            setPadding(act.dp(18), act.dp(26), act.dp(18), act.dp(26))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = act.dp(16) }
        }

        // Header
        card.addView(TextView(act).apply {
            text = Lang.t("🎉 Test Completed!", "🎉 ടെസ്റ്റ് പൂർത്തിയായി!")
            setTextColor(Theme.ink(act))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 27f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        if (subtitle.isNotBlank()) {
            card.addView(TextView(act).apply {
                text = subtitle
                setTextColor(Theme.grey(act))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                layoutParams = lp().apply { topMargin = act.dp(4) }
            })
        }

        // Score
        card.addView(label(act, Lang.t("Score", "സ്കോർ"), top = 16))
        card.addView(TextView(act).apply {
            text = Lang.t("%d / %d", "%d / %d").format(stats.correct, stats.total)
            setTextColor(Theme.teal())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        })

        // Accuracy
        card.addView(label(act, Lang.t("Accuracy", "കൃത്യത"), top = 12))
        card.addView(TextView(act).apply {
            text = "%.1f%%".format(stats.accuracy)
            setTextColor(accuracyColor(act, stats.accuracy))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        })

        // Stats row: Correct | Wrong (| Skipped)
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = lp().apply { topMargin = act.dp(14) }
        }
        row.addView(statCol(act, stats.correct.toString(), Lang.t("Correct", "ശരി"), Theme.correct(act)))
        row.addView(statCol(act, stats.wrong.toString(), Lang.t("Wrong", "തെറ്റ്"), Theme.red(act)))
        if (stats.skipped > 0) {
            row.addView(statCol(act, stats.skipped.toString(), Lang.t("Skipped", "ഒഴിവാക്കിയത്"), Theme.grey(act)))
        }
        card.addView(row)

        // Time
        card.addView(label(act, Lang.t("⏱ Time", "⏱ സമയം"), top = 12))
        card.addView(TextView(act).apply {
            text = formatDuration(stats.elapsedMs)
            setTextColor(Theme.ink(act))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = lp().apply { topMargin = act.dp(2) }
        })

        // Buttons
        val btnCol = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = lp().apply { topMargin = act.dp(16) }
        }
        if (onReview != null) {
            btnCol.addView(Ui.button(act, Lang.t("Review Answers", "ഉത്തരങ്ങൾ പരിശോധിക്കൂ")).apply {
                setOnClickListener { onReview() }
            })
        }
        if (onPracticeWrong != null) {
            btnCol.addView(mintButton(act, Lang.t("Practice Wrong Answers", "തെറ്റിയവ പരിശീലിക്കൂ")).apply {
                setOnClickListener { onPracticeWrong() }
            })
        }
        btnCol.addView(ghostButton(act, Lang.t("Back to Home", "ഹോമിലേക്ക്")).apply {
            setOnClickListener { onHome() }
        })
        card.addView(btnCol)

        return card
    }

    private fun lp() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun label(act: Activity, text: String, top: Int): TextView =
        TextView(act).apply {
            this.text = text
            setTextColor(Theme.grey(act))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            layoutParams = lp().apply { topMargin = act.dp(top) }
        }

    private fun statCol(act: Activity, value: String, labelText: String, color: Int): LinearLayout =
        LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(act).apply {
                text = value
                setTextColor(color)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(TextView(act).apply {
                text = labelText
                setTextColor(Theme.grey(act))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.CENTER
            })
        }

    /** Mint-filled secondary button (outlined feel) for "Practice Wrong Answers". */
    private fun mintButton(act: Activity, text: String): Button =
        Button(act).apply {
            this.text = text
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Theme.green(act))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = act.dp(16).toFloat()
                setColor(Theme.mint(act))
                setStroke(act.dp(2), Theme.green(act))
            }
            setPadding(act.dp(16), act.dp(12), act.dp(16), act.dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = act.dp(10) }
        }

    /** Ghost/text button for "Back to Home". */
    private fun ghostButton(act: Activity, text: String): TextView =
        TextView(act).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Theme.grey(act))
            gravity = Gravity.CENTER
            setPadding(act.dp(16), act.dp(12), act.dp(16), act.dp(12))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = act.dp(6) }
        }

    /**
     * Question-by-question review card for quiz/mock attempts:
     * question text + bookmark star + options with correct (green) /
     * user's wrong pick (red). Mirrors the PYQ review cards.
     */
    fun reviewCard(act: Activity, index: Int, q: Question, userPick: Int): LinearLayout {
        val card = Ui.card(act)
        val headRow = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headRow.addView(TextView(act).apply {
            text = Lang.t("Q%d. ", "ചോദ്യം %d. ").format(index + 1) + q.text
            setTextColor(Theme.ink(act))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        headRow.addView(TextView(act).apply {
            text = if (Store.isBookmarked(act, q.id)) "⭐" else "☆"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setPadding(act.dp(8), act.dp(2), act.dp(2), act.dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val now = Store.toggleBookmark(act, q.id)
                text = if (now) "⭐" else "☆"
            }
        })
        card.addView(headRow)
        q.img?.let { path ->
            Ui.assetImage(act, path)?.let { img -> card.addView(img) }
        }
        val letters = listOf("A", "B", "C", "D")
        val optImgs = q.optImgs?.takeIf { it.size == 4 }
        q.options.forEachIndexed { oi, opt ->
            val imgPath = optImgs?.getOrNull(oi)?.ifBlank { null }
            val state = when {
                oi == q.correct -> Ui.OptState.CORRECT
                oi == userPick -> Ui.OptState.WRONG
                else -> Ui.OptState.NORMAL
            }
            card.addView(Ui.optionCard(act, letters[oi], opt, imgPath, state, onClick = null))
        }
        return card
    }
}
