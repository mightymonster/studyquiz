package com.anandu.studyquiz

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.anandu.studyquiz.Ui.dp

/**
 * PYQ papers of one year (read-only). Each row is a "folder" keyed by the
 * category number (e.g. 1982/2021); tapping one opens the exam-style
 * one-question-at-a-time quiz.
 */
class PyqYearActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        build()
    }

    private fun build() {
        val year = intent.getStringExtra("year") ?: ""
        val (scroll, col) = Ui.screen(this)
        Ui.topBar(
            this, col,
            if (year.isBlank()) Lang.t("📁 Other", "📁 മറ്റുള്ളവ")
            else "📁 $year",
            Lang.t(
                "Previous year question papers — exam style.",
                "മുൻവർഷ ചോദ്യപേപ്പറുകൾ — പരീക്ഷാ രീതിയിൽ."
            )
        )

        val papers = PyqData.papersByYear(this).firstOrNull { it.first == year }?.second
            ?: emptyList()
        if (papers.isEmpty()) {
            Ui.emptyNote(
                this, col,
                Lang.t(
                    "No papers in this year yet.",
                    "ഈ വർഷത്തെ പേപ്പറുകൾ ഇതുവരെ ഇല്ല."
                )
            )
        } else {
            papers.forEach { p -> col.addView(paperCard(p)) }
        }
        setContentView(scroll)
    }

    private fun paperCard(p: PyqPaper): LinearLayout {
        val card = Ui.card(this)
        card.isClickable = true
        card.isFocusable = true

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // Prominent category number
        row.addView(TextView(this).apply {
            text = p.categoryNo
            setTextColor(Theme.tealDark())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        card.addView(row)

        card.addView(TextView(this).apply {
            text = Lang.t(p.titleEn, p.titleMl)
            setTextColor(Theme.ink(this@PyqYearActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        })
        card.addView(TextView(this).apply {
            text = Lang.t("%d questions", "%d ചോദ്യങ്ങൾ").format(p.questions.size)
            setTextColor(Theme.grey(this@PyqYearActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })

        card.setOnClickListener {
            if (p.questions.isEmpty()) {
                Toast.makeText(
                    this,
                    Lang.t("Questions coming soon!", "ചോദ്യങ്ങൾ ഉടൻ വരും!"),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startActivity(
                    Intent(this, PyqExamActivity::class.java).putExtra("paper_id", p.id)
                )
            }
        }
        return card
    }
}
