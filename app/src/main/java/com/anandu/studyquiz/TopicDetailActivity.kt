package com.anandu.studyquiz

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.anandu.studyquiz.Ui.dp

class TopicDetailActivity : Activity() {

    private lateinit var topicId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        topicId = intent.getStringExtra("topicId") ?: run { finish(); return }
        build()
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun topic(): Topic? = Store.data(this).topics.find { it.id == topicId }

    private fun build() {
        val t = topic() ?: run { finish(); return }
        val (scroll, col) = Ui.screen(this)
        Ui.topBar(this, col, t.name, Lang.t("Topic details", "ടോപ്പിക് വിവരങ്ങൾ"))

        // notes card
        Ui.section(this, col, Lang.t("Notes", "നോട്ട്സ്"))
        val notesCard = Ui.card(this)
        if (t.notes.isBlank()) {
            notesCard.addView(TextView(this).apply {
                text = Lang.t("No notes for this topic.", "ഈ ടോപ്പിക്കിന് നോട്ട്സില്ല.")
                setTextColor(Theme.grey(this@TopicDetailActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            })
        } else {
            notesCard.addView(TextView(this).apply {
                text = t.notes
                setTextColor(Theme.ink(this@TopicDetailActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            })
        }
        col.addView(notesCard)

        // actions
        val startQuiz = Ui.button(this, Lang.t("▶ Start Quiz", "▶ ക്വിസ് തുടങ്ങൂ"))
        startQuiz.setOnClickListener {
            val n = Store.questionCount(this, topicId)
            if (n == 0) {
                Toast.makeText(this, Lang.t("No questions yet — content coming soon.", "ചോദ്യങ്ങളില്ല — ഉള്ളടക്കം ഉടൻ വരും."), Toast.LENGTH_SHORT).show()
            } else {
                openCountDialog(n)
            }
        }
        col.addView(startQuiz)

        // questions
        val questions = Store.questionsFor(this, topicId)
        Ui.section(this, col, Lang.t("Questions (%d)", "ചോദ്യങ്ങൾ (%d)").format(questions.size))
        if (questions.isEmpty()) {
            Ui.emptyNote(
                this, col,
                Lang.t("No questions in this topic yet.", "ഈ ടോപ്പിക്കിൽ ചോദ്യങ്ങളില്ല.")
            )
        } else {
            questions.forEachIndexed { i, q ->
                col.addView(questionCard(i + 1, q))
            }
        }

        setContentView(scroll)
    }

    /** Count picker without reaching into MainActivity. */
    private fun openCountDialog(total: Int) {
        val options = mutableListOf<String>()
        if (total >= 10) options.add("10")
        if (total >= 20) options.add("20")
        options.add(Lang.t("All (%d)", "എല്ലാം (%d)").format(total))
        AlertDialog.Builder(this)
            .setTitle(Lang.t("How many questions?", "എത്ര ചോദ്യങ്ങൾ?"))
            .setItems(options.toTypedArray()) { d, which ->
                val count = if (which < options.size - 1) options[which].toInt() else total
                d.dismiss()
                startActivity(
                    Intent(this, QuizActivity::class.java)
                        .putExtra("topicId", topicId).putExtra("count", count)
                )
            }
            .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
            .show()
    }

    private fun questionCard(num: Int, q: Question): LinearLayout {
        val card = Ui.card(this)
        card.addView(TextView(this).apply {
            text = "$num. ${q.text}"
            setTextColor(Theme.ink(this@TopicDetailActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })
        q.options.forEachIndexed { i, opt ->
            val correct = i == q.correct
            card.addView(TextView(this).apply {
                text = "${'A' + i}) $opt"
                setTextColor(if (correct) Theme.green(this@TopicDetailActivity) else Theme.grey(this@TopicDetailActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                if (correct) setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(4), dp(3), dp(4), dp(3))
                background = if (correct) GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(8).toFloat()
                    setColor(Theme.tintBg(this@TopicDetailActivity))
                } else null
            })
        }
        card.addView(TextView(this).apply {
            text = Lang.t("Answer: %s", "ഉത്തരം: %s").format("${'A' + q.correct}")
            setTextColor(Theme.teal())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        })
        if (q.explanation.isNotBlank()) {
            card.addView(TextView(this).apply {
                text = "💡 ${q.explanation}"
                setTextColor(Theme.grey(this@TopicDetailActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(4) }
            })
        }
        return card
    }
}
