package com.anandu.studyquiz

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.anandu.studyquiz.Ui.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Built-in upcoming exams (read-only). Sections with headers; each exam row
 * shows notification number, post name, date/countdown and a
 * "Notification Details" button that opens the bundled notification PDF
 * in-app (with Download). PDFs not yet sent by Anandu show "Coming soon".
 */
class UpcomingExamsActivity : Activity() {

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
            Lang.t("📋 Upcoming Exams", "📋 വരാനിരിക്കുന്ന പരീക്ഷകൾ"),
            Lang.t(
                "Latest notifications from CSEB & PSC.",
                "CSEB-യുടെയും PSC-യുടെയും ഏറ്റവും പുതിയ നോട്ടിഫിക്കേഷനുകൾ."
            )
        )

        val sections = UpcomingExams.sections(this)
        if (sections.isEmpty()) {
            Ui.emptyNote(
                this, col,
                Lang.t(
                    "No upcoming exams listed yet.\nThey'll appear here after the next update.",
                    "വരാനിരിക്കുന്ന പരീക്ഷകളൊന്നും ഇതുവരെ ചേർത്തിട്ടില്ല.\nഅടുത്ത അപ്ഡേറ്റിൽ ഇവിടെ വരും."
                )
            )
        } else {
            sections.forEach { sec ->
                Ui.section(this, col, Lang.t(sec.titleEn, sec.titleMl))
                sec.exams.forEach { e -> col.addView(examCard(e)) }
            }
        }
        setContentView(scroll)
    }

    private fun examCard(e: BuiltinExam): LinearLayout {
        val card = Ui.card(this)

        // Notification no + countdown
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this).apply {
            text = Lang.t("Notification %s", "നോട്ടിഫിക്കേഷൻ %s").format(e.notifNo)
            setTextColor(Theme.ink(this@UpcomingExamsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val cd = UpcomingExams.countdownText(e.dateMs)
        top.addView(TextView(this).apply {
            text = cd
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(
                if (e.dateMs != null && UpcomingExams.daysToGo(e.dateMs) < 0)
                    Theme.grey(this@UpcomingExamsActivity)
                else Theme.tealDark()
            )
        })
        card.addView(top)

        // Post name + date
        card.addView(TextView(this).apply {
            text = Lang.t(e.postEn, e.postMl)
            setTextColor(Theme.grey(this@UpcomingExamsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        })
        if (e.dateMs != null) {
            card.addView(TextView(this).apply {
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(e.dateMs))
                setTextColor(Theme.grey(this@UpcomingExamsActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2) }
            })
        }

        // Notification Details button
        val btn = Ui.button(
            this,
            Lang.t("📄 Notification Details", "📄 നോട്ടിഫിക്കേഷൻ വിശദാംശങ്ങൾ"),
            filled = false
        )
        btn.setOnClickListener {
            if (e.pdf.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    Lang.t("Coming soon!", "ഉടൻ വരും!"),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                startActivity(
                    Intent(this, PdfViewerActivity::class.java)
                        .putExtra("pdf_asset", e.pdf)
                        .putExtra("title", Lang.t(e.postEn, e.postMl))
                        .putExtra("notif_no", e.notifNo)
                )
            }
        }
        card.addView(btn)
        return card
    }
}
