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
 * PYQ result: right/wrong/unanswered counts + percentage + time taken,
 * with a "Review with answers" button that expands the full question list —
 * correct option green ✅, the user's wrong picks red ❌.
 */
class PyqResultActivity : Activity() {

    private lateinit var paper: PyqPaper
    private lateinit var picked: IntArray
    /** Questions in the order they were attempted (shuffled in the exam). */
    private lateinit var questions: List<PyqQuestion>
    /** Original paper indices matching [questions] — for stable bookmark/wrong IDs. */
    private var orderList: List<Int> = emptyList()
    private var elapsedMs = 0L
    private var showReview = false
    /** Opened from Profile history: replay a past attempt, never record again. */
    private var historyMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        val p = PyqData.paper(this, intent.getStringExtra("paper_id") ?: "")
        val pk = intent.getIntArrayExtra("picked")
        if (p == null || pk == null) {
            if (intent.getBooleanExtra("history_mode", false)) {
                Toast.makeText(
                    this,
                    Lang.t("Paper not found", "പേപ്പർ കണ്ടില്ല"),
                    Toast.LENGTH_SHORT
                ).show()
            }
            finish()
            return
        }
        paper = p
        picked = pk
        historyMode = intent.getBooleanExtra("history_mode", false)
        val order = intent.getIntArrayExtra("order")
        orderList = if (order != null && order.size == p.questions.size) order.toList()
        else p.questions.indices.toList()
        questions = orderList.map { p.questions[it] }
        elapsedMs = intent.getLongExtra("elapsed_ms", 0L)
        // Record the PYQ score into Profile → My Analysis (like quizzes & mock tests).
        // savedInstanceState == null => first creation only (no double-record on rotation).
        // History mode never records — it only replays a past attempt.
        if (savedInstanceState == null && !historyMode) {
            val right = questions.indices.count { i ->
                (if (i < picked.size) picked[i] else -1) == questions[i].answer
            }
            val pyqName = Lang.t("📜 PYQ • %s %s", "📜 PYQ • %s %s")
                .format(paper.categoryNo, Lang.t(paper.titleEn, paper.titleMl))
            Store.recordAttempt(
                this, "pyq", pyqName,
                right, questions.size,
                pyqPaperId = paper.id,
                pyqPicked = picked.toList(),
                pyqOrder = orderList,
                elapsedMs = elapsedMs
            )
            // Improvement tracking (v1.26): rich per-test record (Attempt history untouched).
            val pyqTotal = questions.size
            val pyqSkipped = questions.indices.count { i ->
                (if (i < picked.size) picked[i] else -1) == -1
            }
            val pyqWrong = (pyqTotal - right - pyqSkipped).coerceAtLeast(0)
            val pyqAcc = Store.accuracy1dp(right, pyqTotal)
            Store.recordTestRecord(
                this,
                TestRecord(
                    testId = "T" + System.currentTimeMillis(),
                    testKind = "pyq",
                    testName = pyqName,
                    total = pyqTotal,
                    correct = right,
                    wrong = pyqWrong,
                    skipped = pyqSkipped,
                    accuracy = pyqAcc,
                    timeTakenMs = elapsedMs,
                    topicAccuracy = mapOf(
                        Lang.t("📜 PYQ • %s", "📜 PYQ • %s").format(paper.categoryNo) to pyqAcc
                    ),
                    dateMs = System.currentTimeMillis()
                )
            )
            // Feed wrong-only revision: wrong/skipped PYQ questions join the
            // wrong list; correctly answered ones leave it (like quizzes).
            questions.forEachIndexed { i, q ->
                val qid = PyqData.questionId(paper.id, orderList.getOrElse(i) { i })
                val pick = if (i < picked.size) picked[i] else -1
                if (pick == q.answer) Store.removeWrong(this, qid)
                else Store.addWrong(this, qid)
            }
        }
        showReview = historyMode
        build()
    }

    /** "1h 2m 34s" / "1 മണിക്കൂർ 2 മിനിറ്റ് 34 സെക്കൻഡ്" — zero units dropped. */
    private fun formatElapsed(ms: Long): String {
        var s = ms / 1000
        val h = s / 3600; s %= 3600
        val m = s / 60; s %= 60
        val parts = mutableListOf<String>()
        if (h > 0) parts.add(Lang.t("%dh", "%d മണിക്കൂർ").format(h))
        if (m > 0) parts.add(Lang.t("%dm", "%d മിനിറ്റ്").format(m))
        if (s > 0 || parts.isEmpty()) parts.add(Lang.t("%ds", "%d സെക്കൻഡ്").format(s))
        return parts.joinToString(" ")
    }

    private fun build() {
        val (scroll, col) = Ui.screen(this)
        Ui.topBar(
            this, col,
            Lang.t("📊 PYQ Result", "📊 PYQ ഫലം"),
            paper.categoryNo + " • " + Lang.t(paper.titleEn, paper.titleMl)
        )

        var right = 0
        var skipped = 0
        questions.forEachIndexed { i, q ->
            val pick = if (i < picked.size) picked[i] else -1
            when {
                pick == -1 -> skipped++
                pick == q.answer -> right++
            }
        }
        val total = questions.size
        val wrong = total - right - skipped
        val stats = CompletionScreen.Stats(right, wrong, skipped, elapsedMs)

        // This attempt's wrong question IDs (stable paper indices) for
        // attempt-scoped wrong-only practice. Works in history mode too —
        // the attempt's picks/order are persisted.
        val wrongIds = questions.indices
            .filter { i -> (if (i < picked.size) picked[i] else -1) != questions[i].answer }
            .map { i -> PyqData.questionId(paper.id, orderList.getOrElse(i) { i }) }

        col.addView(
            CompletionScreen.card(
                this,
                Lang.t("📜 PYQ • %s %s", "📜 PYQ • %s %s")
                    .format(paper.categoryNo, Lang.t(paper.titleEn, paper.titleMl)),
                stats,
                onReview = {
                    showReview = true
                    build()
                    scroll.post { scroll.fullScroll(android.view.View.FOCUS_DOWN) }
                },
                onPracticeWrong = if (wrongIds.isNotEmpty()) ({
                    startActivity(
                        Intent(this, QuizActivity::class.java)
                            .putExtra("mode", "wrong")
                            .putExtra("questionIds", ArrayList(wrongIds))
                    )
                    finish()
                }) else null,
                onHome = {
                    if (historyMode) {
                        // Came from Profile history — just go back there.
                        finish()
                    } else {
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                        finish()
                    }
                }
            )
        )

        if (showReview) {
            Ui.section(this, col, Lang.t("Answers", "ഉത്തരങ്ങൾ"))
            questions.forEachIndexed { i, q -> col.addView(reviewCard(i, q)) }
        }

        setContentView(scroll)
    }

    private fun reviewCard(i: Int, q: PyqQuestion): LinearLayout {
        val card = Ui.card(this)
        val qid = PyqData.questionId(paper.id, orderList.getOrElse(i) { i })
        // Header: question text + bookmark star.
        val headRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headRow.addView(TextView(this).apply {
            text = Lang.t("Q%d. ", "ചോദ്യം %d. ").format(i + 1) + q.text
            setTextColor(Theme.ink(this@PyqResultActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        headRow.addView(TextView(this).apply {
            text = if (Store.isBookmarked(this@PyqResultActivity, qid)) "⭐" else "☆"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setPadding(dp(8), dp(2), dp(2), dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val now = Store.toggleBookmark(this@PyqResultActivity, qid)
                text = if (now) "⭐" else "☆"
            }
        })
        card.addView(headRow)
        q.img?.let { path ->
            Ui.assetImage(this, path)?.let { img -> card.addView(img) }
        }
        val letters = listOf("A", "B", "C", "D")
        val userPick = if (i < picked.size) picked[i] else -1
        val optImgs = q.optImgs?.takeIf { it.size == 4 }
        q.options.forEachIndexed { oi, opt ->
            val imgPath = optImgs?.getOrNull(oi)?.ifBlank { null }
            val state = when {
                oi == q.answer -> Ui.OptState.CORRECT
                oi == userPick -> Ui.OptState.WRONG
                else -> Ui.OptState.NORMAL
            }
            // Correct/wrong shown via border + subtle tint only — no heavy fills.
            card.addView(Ui.optionCard(this, letters[oi], opt, imgPath, state, onClick = null))
        }
        return card
    }
}
