package com.anandu.studyquiz

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.anandu.studyquiz.Ui.dp
import kotlin.math.abs

/**
 * Exam-style PYQ run: one question at a time.
 * - Tapping an option records the pick and auto-advances (400ms subtle
 *   highlight first so the choice is visible).
 * - Bottom-fixed ◀ / ▶ arrow bar: Previous goes back (pick stays, can be
 *   changed); Next skips forward without answering ("no answer" in result).
 * - On the last question ▶ becomes ✓ Finish.
 * - Figure-option questions (optImgs) show the diagram in each option.
 * - Live elapsed-time ticker at the top; total time is passed to the result.
 */
class PyqExamActivity : Activity() {

    private lateinit var paper: PyqPaper
    private var idx = 0
    private val picked = mutableMapOf<Int, Int>()
    private var startTime = 0L
    /** Shuffled question indices — a fresh random order every exam attempt. */
    private lateinit var order: List<Int>

    private val handler = Handler(Looper.getMainLooper())
    private var timerView: TextView? = null
    /** Question-strip circles (rebuilt with each build(), states updated in place). */
    private val circleViews = mutableListOf<TextView>()
    private val tick = object : Runnable {
        override fun run() {
            timerView?.text = "🕐 " + fmtClock(SystemClock.elapsedRealtime() - startTime)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        val p = PyqData.paper(this, intent.getStringExtra("paper_id") ?: "")
        if (p == null || p.questions.isEmpty()) {
            finish()
            return
        }
        paper = p
        order = p.questions.indices.shuffled()
        startTime = SystemClock.elapsedRealtime()
        build()
        handler.post(tick)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun fmtClock(ms: Long): String {
        val s = ms / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec)
        else "%02d:%02d".format(m, sec)
    }

    private fun build() {
        val (scroll, col) = Ui.screen(this)
        // Swipe left/right anywhere on the question area → next / previous.
        // Returning false keeps normal vertical scrolling and taps working.
        scroll.setOnTouchListener { _, event ->
            swipeDetector.onTouchEvent(event)
            false
        }
        val total = paper.questions.size

        // Header row: Q x/y (left) | timer pill (center) | paper tag (right)
        val headRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headRow.addView(TextView(this).apply {
            text = Lang.t("Q %d / %d", "ചോദ്യം %d / %d").format(idx + 1, total)
            setTextColor(Theme.ink(this@PyqExamActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        timerView = TextView(this).apply {
            text = "🕐 " + fmtClock(SystemClock.elapsedRealtime() - startTime)
            gravity = Gravity.CENTER
            setTextColor(Theme.ink(this@PyqExamActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Theme.mint(this@PyqExamActivity))
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }
        headRow.addView(timerView)
        headRow.addView(TextView(this).apply {
            text = "PYQ • ${paper.categoryNo}"
            gravity = Gravity.CENTER
            setTextColor(Theme.green(this@PyqExamActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(dp(1), Theme.green(this@PyqExamActivity))
            }
            setPadding(dp(12), dp(6), dp(12), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(8) }
        })
        col.addView(headRow)

        // Thin progress bar under the header
        col.addView(Ui.progressBar(this, idx + 1, total))

        // Question card
        val q = paper.questions[order[idx]]
        val qCard = Ui.card(this)
        // Blank text (Q49): the figure image IS the question — no empty gap.
        if (q.text.isNotBlank()) {
            qCard.addView(Ui.styleBody(TextView(this).apply {
                text = Lang.t("Q%d. ", "ചോദ്യം %d. ").format(idx + 1) + q.text
                setTextColor(Theme.ink(this@PyqExamActivity))
                setTypeface(typeface, Typeface.BOLD)
            }, 17f))
        }
        // Figure image (e.g. Q43 triangles diagram), when the question has one
        q.img?.let { path ->
            Ui.assetImage(this, path)?.let { img -> qCard.addView(img) }
        }
        // 4 options — tap records pick and auto-advances.
        // Figure-options (optImgs) show the diagram instead of text.
        val letters = listOf("A", "B", "C", "D")
        val sel = picked[idx]
        val optImgs = q.optImgs?.takeIf { it.size == 4 }
        q.options.forEachIndexed { oi, opt ->
            val imgPath = optImgs?.getOrNull(oi)?.ifBlank { null }
            if (imgPath != null) {
                qCard.addView(
                    Ui.optionRow(this, letters[oi], null, imgPath, filled = (sel == oi)) { onPick(oi) }
                )
            } else {
                val state = if (sel == oi) Ui.OptState.SELECTED else Ui.OptState.NORMAL
                qCard.addView(
                    Ui.optionCard(this, letters[oi], opt, null, state) { card ->
                        onTapOption(oi, letters[oi], opt, qCard, card)
                    }
                )
            }
        }
        col.addView(qCard)

        // Bottom-fixed arrow nav bar (does not scroll with the question)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.bg(this@PyqExamActivity))
            addView(
                scroll,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }
        root.addView(View(this).apply {
            setBackgroundColor(Theme.divider(this@PyqExamActivity))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            )
        })
        // Question strip (v1.26): slim horizontally-scrollable numbered circles
        // above the nav bar. Answered = filled teal, unanswered = teal outline,
        // current = thicker ring + slight scale. Tap jumps to that question —
        // skipped questions are easy to spot and revisit.
        val stripScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Theme.card(this@PyqExamActivity))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val stripRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        circleViews.clear()
        for (i in 0 until total) {
            val n = i
            val tv = TextView(this).apply {
                text = "${n + 1}"
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
                val s = dp(40)
                layoutParams = LinearLayout.LayoutParams(s, s).apply {
                    leftMargin = dp(4); rightMargin = dp(4)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (!finished && !advancing) {
                        idx = n
                        build()
                    }
                }
            }
            circleViews.add(tv)
            stripRow.addView(tv)
        }
        stripScroll.addView(stripRow)
        root.addView(stripScroll)
        updateStrip()
        // Keep the current question's circle visible / centered.
        stripScroll.post {
            circleViews.getOrNull(idx)?.let { v ->
                val target = (v.left - (stripScroll.width - v.width) / 2).coerceAtLeast(0)
                stripScroll.smoothScrollTo(target, 0)
            }
        }
        val isLast = idx == total - 1
        val navBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Theme.card(this@PyqExamActivity))
            setPadding(dp(12), dp(10), dp(12), dp(14))
        }
        navBar.addView(arrowBtn("◀", filled = false).apply {
            isEnabled = idx > 0
            alpha = if (idx > 0) 1f else 0.35f
            setOnClickListener { onPrev() }
        })
        navBar.addView(arrowBtn(if (isLast) "✓" else "▶", filled = true).apply {
            setOnClickListener { onNext() }
        })
        root.addView(navBar)

        setContentView(root)
    }

    /** Circle background for the question strip: teal fill when answered. */
    private fun circleDrawable(answered: Boolean, current: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (answered) {
                setColor(Theme.green(this@PyqExamActivity))
                if (current) setStroke(dp(3), Theme.tealDark())
            } else {
                setColor(Color.TRANSPARENT)
                setStroke(dp(if (current) 3 else 2), Theme.green(this@PyqExamActivity))
            }
        }

    /** Refresh every strip circle's state from the current picks + index. */
    private fun updateStrip() {
        circleViews.forEachIndexed { i, tv ->
            val answered = picked.containsKey(i)
            val current = i == idx
            tv.background = circleDrawable(answered, current)
            tv.setTextColor(if (answered) Color.WHITE else Theme.ink(this))
            val s = if (current) 1.12f else 1f
            tv.scaleX = s
            tv.scaleY = s
        }
    }

    /** Big arrow button for the bottom-fixed exam nav bar. */
    private fun arrowBtn(text: String, filled: Boolean): Button = Button(this).apply {
        this.text = text
        isAllCaps = false
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (filled) Color.WHITE else Theme.green(this@PyqExamActivity))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(12).toFloat()
            if (filled) setColor(Theme.green(this@PyqExamActivity))
            else {
                setColor(Color.TRANSPARENT)
                setStroke(dp(2), Theme.green(this@PyqExamActivity))
            }
        }
        setPadding(dp(16), dp(6), dp(16), dp(6))
        layoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply { leftMargin = dp(6); rightMargin = dp(6) }
    }

    private fun onPick(oi: Int) {
        picked[idx] = oi
        if (idx >= paper.questions.size - 1) {
            finishExam()
        } else {
            idx++
            build()
        }
    }

    /** Guard against double-taps while the selection highlight is showing. */
    private var advancing = false
    /** Set once the exam is submitted — ignores any further taps. */
    private var finished = false

    /**
     * Swipe left/right on the question area → next / previous question.
     * Only true horizontal flings count (vertical scrolling is untouched),
     * and it respects the same guards as the ◀ / ▶ buttons.
     */
    private val swipeDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                val start = e1 ?: return false
                val dx = e2.x - start.x
                val dy = e2.y - start.y
                if (abs(dx) > abs(dy) * 1.5f &&
                    abs(dx) > dp(72) &&
                    abs(velocityX) > abs(velocityY) &&
                    abs(velocityX) > 1000
                ) {
                    if (advancing || finished) return true
                    if (dx < 0) onNext() else onPrev()
                    return true
                }
                return false
            }
        })
    }

    /**
     * Option tapped: swap the card for its subtle SELECTED rendering first so
     * the choice is visible, then auto-advance after a beat.
     */
    private fun onTapOption(
        oi: Int, letter: String, opt: String,
        container: LinearLayout, card: LinearLayout
    ) {
        if (advancing || finished) return
        advancing = true
        picked[idx] = oi
        val pos = container.indexOfChild(card)
        if (pos >= 0) {
            container.removeViewAt(pos)
            container.addView(
                Ui.optionCard(this, letter, opt, null, Ui.OptState.SELECTED, onClick = null),
                pos
            )
        }
        handler.postDelayed({
            advancing = false
            if (isFinishing) return@postDelayed
            if (idx >= paper.questions.size - 1) finishExam() else { idx++; build() }
        }, 400)
    }

    private fun onPrev() {
        if (idx > 0) {
            idx--
            build()
        }
    }

    private fun onNext() {
        if (finished || advancing) return
        if (idx >= paper.questions.size - 1) {
            finishExam()
        } else {
            // Skip forward without answering — stays "no answer".
            idx++
            build()
        }
    }

    private fun finishExam() {
        // Double-tap on ✓ (or a tap racing the highlight delay) must not
        // open two stacked result screens.
        if (finished) return
        finished = true
        val elapsed = SystemClock.elapsedRealtime() - startTime
        val arr = IntArray(paper.questions.size) { i -> picked[i] ?: -1 }
        startActivity(
            Intent(this, PyqResultActivity::class.java)
                .putExtra("paper_id", paper.id)
                .putExtra("picked", arr)
                .putExtra("order", order.toIntArray())
                .putExtra("elapsed_ms", elapsed)
        )
        finish()
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Leave exam?")
            .setMessage("Your progress in this exam will be lost.")
            .setPositiveButton("Yes") { d, _ ->
                d.dismiss()
                super.onBackPressed()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
