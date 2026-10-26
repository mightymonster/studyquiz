package com.anandu.studyquiz

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.anandu.studyquiz.Ui.dp

/**
 * Mock test mode: real-exam simulation.
 * 100 questions (or all available if the bank has fewer), 90-minute timer,
 * 1 mark each. OMR-style palette (answered / unanswered / current),
 * tap a number to jump. No per-question feedback — answers are locked in
 * at submit; the timer auto-submits.
 */
class MockTestActivity : Activity() {

    private lateinit var questions: List<Question>
    private lateinit var picked: IntArray // -1 = unanswered
    private var current = 0
    private var submitted = false
    private var timeLeftMs = TOTAL_MS
    private var timer: CountDownTimer? = null

    private lateinit var timerView: TextView
    private lateinit var palette: GridLayout
    private lateinit var qLabel: TextView
    private lateinit var starBtn: TextView
    private lateinit var qText: TextView
    private lateinit var optsCol: LinearLayout
    private lateinit var prevBtn: Button
    private lateinit var nextBtn: Button
    private var paletteBtns: List<TextView> = emptyList()
    private var optButtons: List<Button> = emptyList()

    companion object {
        const val TOTAL_MS = 90L * 60L * 1000L
        const val MAX_QS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)

        val pool = Store.data(this).questions
        if (pool.isEmpty()) { finish(); return }
        questions = pool.shuffled().take(MAX_QS.coerceAtMost(pool.size))
        picked = IntArray(questions.size) { -1 }

        build()
        renderQuestion()
        startTimer()
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    override fun onResume() {
        super.onResume()
        if (!submitted) startTimer()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.bg(this@MockTestActivity))
        }

        // ---- timer row ----
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(4))
        }
        timerView = TextView(this).apply {
            setTextColor(Theme.ink(this@MockTestActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(timerView)
        val submitBtn = Ui.button(this, Lang.t("Submit 🏁", "സബ്മിറ്റ് 🏁")).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 0 }
            setOnClickListener { confirmSubmit() }
        }
        topRow.addView(submitBtn)
        root.addView(topRow)

        // ---- OMR palette ----
        palette = GridLayout(this).apply { columnCount = 8 }
        paletteBtns = questions.indices.map { i ->
            TextView(this).apply {
                text = "${i + 1}"
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, Typeface.BOLD)
                setPadding(dp(2), dp(6), dp(2), dp(6))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(i % 8, 1f)
                    setMargins(dp(3), dp(3), dp(3), dp(3))
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { current = i; renderQuestion() }
            }.also { palette.addView(it) }
        }
        root.addView(ScrollView(this).apply {
            addView(palette)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(168)
            ).apply { topMargin = dp(4); bottomMargin = dp(2) }
            setPadding(dp(12), 0, dp(12), 0)
        })

        // ---- question area ----
        val qScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val qCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(6), dp(16), dp(8))
        }
        val labelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        qLabel = TextView(this).apply {
            setTextColor(Theme.teal())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        labelRow.addView(qLabel)
        starBtn = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(4))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val q = questions[current]
                text = if (Store.toggleBookmark(this@MockTestActivity, q.id)) "⭐" else "☆"
            }
        }
        labelRow.addView(starBtn)
        qCol.addView(labelRow)

        qText = TextView(this).apply {
            setTextColor(Theme.ink(this@MockTestActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6); bottomMargin = dp(6) }
        }
        qCol.addView(qText)
        optsCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        qCol.addView(optsCol)
        qScroll.addView(qCol)
        root.addView(qScroll)

        // ---- prev / next ----
        val navRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(4), dp(16), dp(14))
        }
        prevBtn = Ui.button(this, Lang.t("← Prev", "← മുൻപ്"), filled = false).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(4); topMargin = 0
            }
            setOnClickListener { if (current > 0) { current--; renderQuestion() } }
        }
        nextBtn = Ui.button(this, Lang.t("Next →", "അടുത്തത് →")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(4); topMargin = 0
            }
            setOnClickListener { if (current < questions.size - 1) { current++; renderQuestion() } }
        }
        navRow.addView(prevBtn); navRow.addView(nextBtn)
        root.addView(navRow)

        setContentView(root)
    }

    private fun startTimer() {
        timer?.cancel()
        updateTimerText()
        timer = object : CountDownTimer(timeLeftMs, 1000) {
            override fun onTick(ms: Long) {
                timeLeftMs = ms
                updateTimerText()
            }

            override fun onFinish() {
                timeLeftMs = 0
                submit(auto = true)
            }
        }.start()
    }

    private fun updateTimerText() {
        val s = (timeLeftMs / 1000).toInt()
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        timerView.text = "⏱ %d:%02d:%02d".format(h, m, sec)
        timerView.setTextColor(
            if (timeLeftMs < 5 * 60 * 1000) Theme.red(this) else Theme.ink(this)
        )
    }

    private fun paletteBg(state: Int): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(8).toFloat()
        when (state) {
            0 -> { // current
                setColor(Theme.teal())
            }
            1 -> { // answered
                setColor(Theme.tintBg(this@MockTestActivity))
                setStroke(dp(1), Theme.teal())
            }
            else -> { // unanswered
                setColor(Theme.card(this@MockTestActivity))
                setStroke(dp(1), Theme.divider(this@MockTestActivity))
            }
        }
    }

    private fun renderQuestion() {
        val q = questions[current]
        qLabel.text = Lang.t("Question %d of %d", "%d / %d ചോദ്യം").format(current + 1, questions.size)
        starBtn.text = if (Store.isBookmarked(this, q.id)) "⭐" else "☆"
        qText.text = q.text

        optsCol.removeAllViews()
        optButtons = q.options.mapIndexed { i, opt ->
            Button(this).apply {
                text = "${'A' + i})  $opt"
                isAllCaps = false
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                val sel = picked[current] == i
                setTextColor(if (sel) Theme.teal() else Theme.ink(this@MockTestActivity))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(12).toFloat()
                    setColor(if (sel) Theme.tintBg(this@MockTestActivity) else Theme.card(this@MockTestActivity))
                    setStroke(dp(if (sel) 2 else 1), if (sel) Theme.teal() else Theme.divider(this@MockTestActivity))
                }
                setTypeface(typeface, if (sel) Typeface.BOLD else Typeface.NORMAL)
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                setOnClickListener {
                    picked[current] = i
                    renderQuestion()
                }
            }.also { optsCol.addView(it) }
        }

        paletteBtns.forEachIndexed { i, b ->
            val state = when {
                i == current -> 0
                picked[i] != -1 -> 1
                else -> 2
            }
            b.background = paletteBg(state)
            b.setTextColor(
                when (state) {
                    0 -> Color.WHITE
                    1 -> Theme.teal()
                    else -> Theme.grey(this@MockTestActivity)
                }
            )
        }

        prevBtn.alpha = if (current == 0) 0.4f else 1f
        nextBtn.alpha = if (current == questions.size - 1) 0.4f else 1f
    }

    private fun confirmSubmit() {
        val unanswered = picked.count { it == -1 }
        val msg = if (unanswered == 0)
            Lang.t("Submit the mock test now?", "മോക്ക് ടെസ്റ്റ് സബ്മിറ്റ് ചെയ്യണോ?")
        else
            Lang.t("%d question(s) unanswered. Submit anyway?", "%d ചോദ്യം ഉത്തരം പറഞ്ഞിട്ടില്ല. എന്നാലും സബ്മിറ്റ് ചെയ്യണോ?").format(unanswered)
        AlertDialog.Builder(this)
            .setTitle(Lang.t("Submit mock test", "മോക്ക് ടെസ്റ്റ് സബ്മിറ്റ്"))
            .setMessage(msg)
            .setPositiveButton(Lang.t("Submit 🏁", "സബ്മിറ്റ് 🏁")) { d, _ -> d.dismiss(); submit(auto = false) }
            .setNegativeButton(Lang.t("Keep writing", "തുടരൂ"), null)
            .show()
    }

    private fun submit(auto: Boolean) {
        if (submitted) return
        submitted = true
        timer?.cancel()

        var correct = 0
        val perTopic = mutableMapOf<String, IntArray>()
        questions.forEachIndexed { i, q ->
            val ok = picked[i] == q.correct
            if (ok) {
                correct++
                Store.removeWrong(this, q.id)
            } else {
                Store.addWrong(this, q.id)
            }
            val e = perTopic.getOrPut(q.topicId) { intArrayOf(0, 0) }
            e[1]++
            if (ok) e[0]++
        }
        val mockName =
            Lang.t("📝 Mock Test (%d Q)", "📝 മോക്ക് ടെസ്റ്റ് (%d ചോദ്യം)").format(questions.size)
        val attempt = Store.recordAttempt(
            this, Store.ALL, mockName,
            correct, questions.size, perTopic
        )
        // Improvement tracking (v1.26): rich per-test record (Attempt history untouched).
        val mockTotal = questions.size
        val mockSkipped = picked.count { it == -1 }
        val mockWrong = (mockTotal - correct - mockSkipped).coerceAtLeast(0)
        val mockTopicAcc = Store.topicAccuracyNamed(this, perTopic)
            .ifEmpty { mapOf(mockName to Store.accuracy1dp(correct, mockTotal)) }
        Store.recordTestRecord(
            this,
            TestRecord(
                testId = "T" + System.currentTimeMillis(),
                testKind = "mock",
                testName = mockName,
                total = mockTotal,
                correct = correct,
                wrong = mockWrong,
                skipped = mockSkipped,
                accuracy = Store.accuracy1dp(correct, mockTotal),
                timeTakenMs = TOTAL_MS - timeLeftMs,
                topicAccuracy = mockTopicAcc,
                dateMs = System.currentTimeMillis()
            )
        )
        if (auto) {
            Toast.makeText(
                this,
                Lang.t("Time's up! Auto-submitted.", "സമയം കഴിഞ്ഞു! ഓട്ടോ സബ്മിറ്റ് ചെയ്തു."),
                Toast.LENGTH_LONG
            ).show()
        }
        val wrongIds = questions.indices
            .filter { picked[it] != questions[it].correct }
            .map { questions[it].id }
        startActivity(
            Intent(this, ResultActivity::class.java)
                .putExtra("attemptId", attempt.id)
                .putExtra("topicId", Store.ALL)
                .putExtra("qIds", ArrayList(questions.map { it.id }))
                .putExtra("qPicked", picked)
                .putExtra("wrongIds", ArrayList(wrongIds))
                .putExtra("elapsed_ms", TOTAL_MS - timeLeftMs)
        )
        finish()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(Lang.t("Quit mock test?", "മോക്ക് ടെസ്റ്റ് നിർത്തണോ?"))
            .setMessage(Lang.t("This attempt won't be saved.", "ഈ ശ്രമം സേവ് ആകില്ല."))
            .setPositiveButton(Lang.t("Quit", "നിർത്തൂ")) { d, _ ->
                d.dismiss()
                submitted = true // don't auto-submit on the way out
                timer?.cancel()
                super.onBackPressed()
            }
            .setNegativeButton(Lang.t("Continue", "തുടരൂ"), null)
            .show()
    }
}
