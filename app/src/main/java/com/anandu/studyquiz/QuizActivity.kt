package com.anandu.studyquiz

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.anandu.studyquiz.Ui.dp

class QuizActivity : Activity() {

    private lateinit var topicId: String
    private lateinit var quizQuestions: List<Question>
    private var index = 0
    private var correctCount = 0
    private val answeredCorrect = mutableListOf<Boolean>()
    /** Picked option index per answered question (parallel to [answeredCorrect]). */
    private val pickedIdx = mutableListOf<Int>()
    private var selected = -1
    private var locked = false
    private var mode = "normal"

    private lateinit var qNum: TextView
    private lateinit var timerPill: TextView
    private lateinit var starBtn: TextView
    private lateinit var qText: TextView
    private lateinit var qImgCol: LinearLayout
    private lateinit var optsCol: LinearLayout
    private lateinit var feedback: TextView
    private lateinit var nextBtn: Button
    private lateinit var progressWrap: LinearLayout
    private var optCards: List<LinearLayout> = emptyList()

    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private val tick = object : Runnable {
        override fun run() {
            timerPill.text = "🕐 " + fmtClock(SystemClock.elapsedRealtime() - startTime)
            handler.postDelayed(this, 1000)
        }
    }

    private fun fmtClock(ms: Long): String {
        val s = ms / 1000
        return "%02d:%02d".format(s / 60, s % 60)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        topicId = intent.getStringExtra("topicId") ?: Store.ALL
        mode = intent.getStringExtra("mode") ?: "normal"
        val singleId = intent.getStringExtra("singleId")
        val count = intent.getIntExtra("count", 10)

        val base = when (mode) {
            "bookmarks" -> Store.bookmarkedQuestions(this)
            "wrong" -> {
                // Attempt-scoped practice (from a result screen): only the
                // wrong question IDs passed via "questionIds". Falls back to
                // the global wrong list when the filter yields nothing.
                val all = Store.wrongQuestions(this)
                val scoped = intent.getStringArrayListExtra("questionIds")
                if (scoped != null) {
                    val f = all.filter { it.id in scoped }
                    if (f.isNotEmpty()) f else all
                } else all
            }
            else -> if (topicId == Store.ALL) Store.data(this).questions
            else Store.questionsFor(this, topicId)
        }
        if (base.isEmpty()) { finish(); return }
        quizQuestions = if (singleId != null) {
            base.filter { it.id == singleId }.ifEmpty { base }
        } else {
            base.shuffled().take(if (mode == "normal") count.coerceAtMost(base.size) else base.size)
        }

        startTime = SystemClock.elapsedRealtime()
        build()
        renderQuestion()
        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun build() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
        }
        val title = when (mode) {
            "bookmarks" -> Lang.t("⭐ Bookmarks", "⭐ ബുക്ക്മാർക്കുകൾ")
            "wrong" -> Lang.t("❌ Wrong only", "❌ തെറ്റിയത് മാത്രം")
            else -> Store.topicName(this, topicId)
        }
        val tag = when (mode) {
            "bookmarks" -> "⭐"
            "wrong" -> "❌"
            else -> Lang.t("Quiz", "ക്വിസ്")
        }

        // Header row: Q x/y | timer pill | tag
        val headRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        qNum = TextView(this).apply {
            setTextColor(Theme.ink(this@QuizActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headRow.addView(qNum)
        timerPill = TextView(this).apply {
            text = "🕐 00:00"
            gravity = Gravity.CENTER
            setTextColor(Theme.ink(this@QuizActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Theme.mint(this@QuizActivity))
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }
        headRow.addView(timerPill)
        headRow.addView(TextView(this).apply {
            text = tag
            gravity = Gravity.CENTER
            setTextColor(Theme.green(this@QuizActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke(dp(1), Theme.green(this@QuizActivity))
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(8) }
        })
        col.addView(headRow)

        progressWrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(progressWrap)

        // Question card with star
        val qCard = Ui.card(this)
        val qHeadRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        qText = Ui.styleBody(TextView(this).apply {
            setTextColor(Theme.ink(this@QuizActivity))
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }, 17f)
        qHeadRow.addView(qText)
        starBtn = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(4), dp(4))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val q = quizQuestions[index]
                val now = Store.toggleBookmark(this@QuizActivity, q.id)
                text = if (now) "⭐" else "☆"
            }
        }
        qHeadRow.addView(starBtn)
        qCard.addView(qHeadRow)

        // Optional question figure (PYQ figure questions in wrong-only/bookmarks).
        qImgCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        qCard.addView(qImgCol)
        col.addView(qCard)

        optsCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(optsCol)

        feedback = TextView(this).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }
        col.addView(feedback)

        nextBtn = Ui.button(this, Lang.t("Next →", "അടുത്തത് →"))
        nextBtn.isEnabled = false
        nextBtn.alpha = 0.4f
        nextBtn.setOnClickListener {
            index++
            if (index >= quizQuestions.size) finishQuiz() else renderQuestion()
        }
        col.addView(nextBtn)

        // Screen title above the header (kept small — the Q x/y row is the focus)
        val titleView = TextView(this).apply {
            text = title
            setTextColor(Theme.grey(this@QuizActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        col.addView(titleView, 0)

        setContentView(ScrollView(this).apply {
            setBackgroundColor(Theme.bg(this@QuizActivity))
            addView(col)
        })
    }

    private fun renderQuestion() {
        val q = quizQuestions[index]
        selected = -1
        locked = false
        qNum.text = Lang.t("Q %d / %d", "ചോദ്യം %d / %d").format(index + 1, quizQuestions.size)
        starBtn.text = if (Store.isBookmarked(this, q.id)) "⭐" else "☆"
        qText.text = q.text
        feedback.text = ""
        nextBtn.isEnabled = false
        nextBtn.alpha = 0.4f
        nextBtn.text = if (index == quizQuestions.size - 1)
            Lang.t("See Result 🏁", "റിസൾട്ട് കാണൂ 🏁")
        else Lang.t("Next →", "അടുത്തത് →")

        progressWrap.removeAllViews()
        progressWrap.addView(Ui.progressBar(this, index + 1, quizQuestions.size))

        optsCol.removeAllViews()
        qImgCol.removeAllViews()
        q.img?.let { path ->
            Ui.assetImage(this, path)?.let { qImgCol.addView(it) }
        }
        val letters = listOf("A", "B", "C", "D")
        val optImgs = q.optImgs?.takeIf { it.size == 4 }
        optCards = q.options.mapIndexed { i, opt ->
            val imgPath = optImgs?.getOrNull(i)?.ifBlank { null }
            val card = Ui.optionCard(
                this, letters[i], opt, imgPath, Ui.OptState.NORMAL
            ) { if (!locked) selectOption(i) }
            optsCol.addView(card)
            card
        }
    }

    private fun selectOption(i: Int) {
        locked = true
        selected = i
        val q = quizQuestions[index]
        val ok = i == q.correct
        answeredCorrect.add(ok)
        pickedIdx.add(i)
        if (ok) {
            correctCount++
            Store.removeWrong(this, q.id) // learned it — drop from wrong-only list
        } else {
            Store.addWrong(this, q.id)
        }

        val letters = listOf("A", "B", "C", "D")
        val optImgs = q.optImgs?.takeIf { it.size == 4 }
        optsCol.removeAllViews()
        optCards = q.options.mapIndexed { j, opt ->
            val imgPath = optImgs?.getOrNull(j)?.ifBlank { null }
            val state = when {
                j == q.correct -> Ui.OptState.CORRECT
                j == i -> Ui.OptState.WRONG
                else -> Ui.OptState.NORMAL
            }
            val card = Ui.optionCard(this, letters[j], opt, imgPath, state, onClick = null)
            optsCol.addView(card)
            card
        }
        feedback.text = if (ok) Lang.t("✅ Correct!", "✅ ശരി!") else Lang.t("❌ Wrong — correct: %s", "❌ തെറ്റ് — ശരി: %s").format("${'A' + q.correct}")
        feedback.setTextColor(if (ok) Theme.correct(this) else Theme.red(this))
        nextBtn.isEnabled = true
        nextBtn.alpha = 1f
    }

    private fun finishQuiz() {
        // per-topic breakdown for true topic-wise analysis
        val perTopic = mutableMapOf<String, IntArray>()
        quizQuestions.forEachIndexed { i, q ->
            val e = perTopic.getOrPut(q.topicId) { intArrayOf(0, 0) }
            e[1]++
            if (answeredCorrect.getOrNull(i) == true) e[0]++
        }
        val (scopeId, scopeName) = when (mode) {
            "bookmarks" -> "bookmarks" to Lang.t("⭐ Bookmarks", "⭐ ബുക്ക്മാർക്കുകൾ")
            "wrong" -> "wrong" to Lang.t("❌ Wrong only", "❌ തെറ്റിയത് മാത്രം")
            else -> topicId to Store.topicName(this, topicId)
        }
        val attempt = Store.recordAttempt(
            this, scopeId, scopeName,
            correctCount, quizQuestions.size, perTopic
        )
        // Improvement tracking (v1.26): rich per-test record. The Attempt
        // history above is untouched — this is a separate, sync-ready log.
        val totalQ = quizQuestions.size
        val quizSkipped = (totalQ - answeredCorrect.size).coerceAtLeast(0)
        val quizWrong = (totalQ - correctCount - quizSkipped).coerceAtLeast(0)
        val quizTopicAcc = Store.topicAccuracyNamed(this, perTopic)
            .ifEmpty { mapOf(scopeName to Store.accuracy1dp(correctCount, totalQ)) }
        Store.recordTestRecord(
            this,
            TestRecord(
                testId = "T" + System.currentTimeMillis(),
                testKind = "quiz",
                testName = scopeName,
                total = totalQ,
                correct = correctCount,
                wrong = quizWrong,
                skipped = quizSkipped,
                accuracy = Store.accuracy1dp(correctCount, totalQ),
                timeTakenMs = SystemClock.elapsedRealtime() - startTime,
                topicAccuracy = quizTopicAcc,
                dateMs = System.currentTimeMillis()
            )
        )
        val wrongIds = quizQuestions.indices
            .filter { answeredCorrect.getOrNull(it) != true }
            .map { quizQuestions[it].id }
        startActivity(
            Intent(this, ResultActivity::class.java)
                .putExtra("attemptId", attempt.id)
                .putExtra("topicId", scopeId)
                .putExtra("qIds", ArrayList(quizQuestions.map { it.id }))
                .putExtra("qPicked", pickedIdx.toIntArray())
                .putExtra("wrongIds", ArrayList(wrongIds))
                .putExtra("elapsed_ms", SystemClock.elapsedRealtime() - startTime)
        )
        finish()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(Lang.t("Quit quiz?", "ക്വിസ് നിർത്തണോ?"))
            .setMessage(Lang.t("This attempt won't be saved.", "ഈ ശ്രമം സേവ് ആകില്ല."))
            .setPositiveButton(Lang.t("Quit", "നിർത്തൂ")) { d, _ -> d.dismiss(); super.onBackPressed() }
            .setNegativeButton(Lang.t("Continue", "തുടരൂ"), null)
            .show()
    }
}
