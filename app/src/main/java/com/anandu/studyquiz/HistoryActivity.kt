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
 * Full attempt history. Reached from Profile → "View All".
 * Back arrow (top-left) returns to Profile.
 */
class HistoryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        build()
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun build() {
        val (root, col) = Ui.screenWithNav(this, "profile")
        val attempts = Store.data(this).attempts.sortedByDescending { it.whenMs }

        val headRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headRow.addView(TextView(this).apply {
            text = "‹"
            gravity = Gravity.CENTER
            setTextColor(Theme.ink(this@HistoryActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(2), dp(10), dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener { finish() } // back to Profile
        })
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = Lang.t("History", "ഹിസ്റ്ററി")
            setTextColor(Theme.ink(this@HistoryActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        titleCol.addView(TextView(this).apply {
            text = Lang.t("Every quiz & exam attempt", "എല്ലാ ക്വിസ്/പരീക്ഷാ ശ്രമങ്ങളും")
            setTextColor(Theme.grey(this@HistoryActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        })
        headRow.addView(titleCol)
        headRow.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(6) }
        col.addView(headRow)

        if (attempts.isEmpty()) {
            val c = Ui.card(this)
            Ui.emptyNote(
                this, c,
                Lang.t("No attempts yet.", "ഇതുവരെ ശ്രമങ്ങളില്ല.")
            )
            col.addView(c)
        } else {
            // Reuse the profile's row renderer via a lightweight host.
            val host = ProfileRowHost(this)
            val card = Ui.card(this)
            attempts.forEach { a -> card.addView(host.row(a) { build() }) }
            col.addView(card)
        }

        setContentView(root)
    }
}

/**
 * Builds history rows identical to Profile's, without needing a
 * ProfileActivity instance. Tap a PYQ entry → review; long-press → delete.
 */
private class ProfileRowHost(val act: Activity) {
    fun row(a: Attempt, onDeleted: () -> Unit): LinearLayout {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, act.dp(8), 0, act.dp(8))
            isClickable = true
            isFocusable = true
        }
        row.addView(Ui.iconChip(act, "📄", Theme.mint(act), boxDp = 44, emojiSp = 22f))
        val left = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = act.dp(12)
            }
        }
        left.addView(Ui.styleBody(android.widget.TextView(act).apply {
            text = a.topicName
            setTextColor(Theme.ink(act))
            setTypeface(typeface, Typeface.BOLD)
        }, 14f))
        left.addView(android.widget.TextView(act).apply {
            text = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault())
                .format(java.util.Date(a.whenMs))
            setTextColor(Theme.grey(act))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        row.addView(left)
        val scoreCol = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        scoreCol.addView(android.widget.TextView(act).apply {
            text = "${a.correct}/${a.total}"
            setTextColor(Theme.green(act))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.END
        })
        scoreCol.addView(android.widget.TextView(act).apply {
            text = "(${a.pct}%)"
            setTextColor(Theme.green(act))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.END
        })
        row.addView(scoreCol)
        if (a.hasReview) {
            row.addView(Ui.chevron(act, bg = Theme.mint(act), circleDp = 30))
            row.setOnClickListener {
                act.startActivity(
                    Intent(act, PyqResultActivity::class.java)
                        .putExtra("paper_id", a.pyqPaperId)
                        .putExtra("picked", a.pyqPicked.toIntArray())
                        .putExtra("order", a.pyqOrder.toIntArray())
                        .putExtra("elapsed_ms", a.elapsedMs)
                        .putExtra("history_mode", true)
                )
            }
        }
        row.setOnLongClickListener {
            android.app.AlertDialog.Builder(act)
                .setTitle(Lang.t("Delete attempt?", "ശ്രമം ഡിലീറ്റ് ചെയ്യണോ?"))
                .setMessage("${a.topicName} • ${a.correct}/${a.total}")
                .setPositiveButton(Lang.t("Delete", "ഡിലീറ്റ് ചെയ്യൂ")) { d, _ ->
                    Store.deleteAttempt(act, a.id); onDeleted(); d.dismiss()
                }
                .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
                .show()
            true
        }
        return row
    }
}
