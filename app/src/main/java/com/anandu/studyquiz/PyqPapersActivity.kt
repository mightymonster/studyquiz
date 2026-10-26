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
import com.anandu.studyquiz.Ui.dp

/**
 * PYQ year folders (read-only). Each row is a year folder (e.g. 📁 2026);
 * tapping one opens the papers of that year ([PyqYearActivity]).
 */
class PyqPapersActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        build()
    }

    private fun build() {
        val (scroll, col) = Ui.screen(this)
        Ui.topBar(
            this, col,
            Lang.t("📝 PYQ Papers", "📝 PYQ പേപ്പറുകൾ"),
            Lang.t(
                "Previous year question papers — choose a year.",
                "മുൻവർഷ ചോദ്യപേപ്പറുകൾ — വർഷം തിരഞ്ഞെടുക്കൂ."
            )
        )

        val groups = PyqData.papersByYear(this)
        if (groups.isEmpty()) {
            Ui.emptyNote(
                this, col,
                Lang.t(
                    "PYQ papers coming soon.\nThey'll appear here after the next update.",
                    "PYQ പേപ്പറുകൾ ഉടൻ വരും.\nഅടുത്ത അപ്ഡേറ്റിൽ ഇവിടെ വരും."
                )
            )
        } else {
            groups.forEach { (year, papers) -> col.addView(yearCard(year, papers.size)) }
        }
        setContentView(scroll)
    }

    private fun yearCard(year: String, count: Int): LinearLayout {
        val card = Ui.card(this)
        card.isClickable = true
        card.isFocusable = true

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        // Folder label
        row.addView(TextView(this).apply {
            text = if (year.isBlank()) Lang.t("📁 Other", "📁 മറ്റുള്ളവ")
            else "📁 $year"
            setTextColor(Theme.ink(this@PyqPapersActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        // Paper count
        row.addView(TextView(this).apply {
            text = Lang.t("%d papers", "%d പേപ്പറുകൾ").format(count)
            setTextColor(Theme.grey(this@PyqPapersActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        })
        card.addView(row)

        card.setOnClickListener {
            startActivity(
                Intent(this, PyqYearActivity::class.java).putExtra("year", year)
            )
        }
        return card
    }
}
