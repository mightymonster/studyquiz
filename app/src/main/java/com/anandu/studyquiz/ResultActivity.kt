package com.anandu.studyquiz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.anandu.studyquiz.Ui.dp

/**
 * Quiz / mock-test completion screen (v1.25): centered "Test Completed!"
 * card with score, color-coded accuracy, correct/wrong/skipped stats,
 * time taken, and Review / Practice Wrong / Back to Home actions.
 *
 * Review + wrong-ID data arrive as intent extras from QuizActivity /
 * MockTestActivity (fresh completions only — quiz attempts have no
 * history-mode replay).
 */
class ResultActivity : Activity() {

    private lateinit var attempt: Attempt
    private var qIds: List<String> = emptyList()
    private var qPicked: IntArray = IntArray(0)
    private var wrongIds: List<String> = emptyList()
    private var elapsedMs = 0L
    private var showReview = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)

        val attemptId = intent.getStringExtra("attemptId") ?: run { finish(); return }
        attempt = Store.data(this).attempts.find { it.id == attemptId } ?: run { finish(); return }
        qIds = intent.getStringArrayListExtra("qIds") ?: emptyList()
        qPicked = intent.getIntArrayExtra("qPicked") ?: IntArray(0)
        wrongIds = intent.getStringArrayListExtra("wrongIds") ?: emptyList()
        elapsedMs = intent.getLongExtra("elapsed_ms", 0L)
        build()
    }

    private fun build() {
        val (scroll, col) = Ui.screen(this)

        val skipped = qPicked.count { it == -1 }
        val correct = attempt.correct
        val wrong = (attempt.total - correct - skipped).coerceAtLeast(0)
        val stats = CompletionScreen.Stats(correct, wrong, skipped, elapsedMs)
        val canReview = qIds.isNotEmpty() && qPicked.size == qIds.size

        col.addView(
            CompletionScreen.card(
                this,
                attempt.topicName,
                stats,
                onReview = if (canReview) ({
                    showReview = true
                    build()
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                }) else null,
                onPracticeWrong = if (wrongIds.isNotEmpty()) ({
                    // Attempt-scoped wrong-only practice: only this run's misses.
                    startActivity(
                        Intent(this, QuizActivity::class.java)
                            .putExtra("mode", "wrong")
                            .putExtra("questionIds", ArrayList(wrongIds))
                    )
                    finish()
                }) else null,
                onHome = {
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    )
                    finish()
                }
            )
        )

        if (showReview && canReview) {
            Ui.section(this, col, Lang.t("Answers", "ഉത്തരങ്ങൾ"))
            qIds.forEachIndexed { i, qid ->
                Store.questionByIdAny(this, qid)?.let { q ->
                    col.addView(CompletionScreen.reviewCard(this, i, q, qPicked[i]))
                }
            }
        }

        setContentView(scroll)
    }
}
