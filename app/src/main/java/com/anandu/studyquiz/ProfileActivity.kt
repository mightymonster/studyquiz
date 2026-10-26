package com.anandu.studyquiz

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.anandu.studyquiz.Ui.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Profile = My Analysis (stats, trend chart, topic-wise, streak, history).
 * Header has a back arrow (top-left) → Home, and help / gear actions.
 */
class ProfileActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        build()
    }

    override fun onResume() {
        super.onResume()
        build() // refresh avatar / stats
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        overridePendingTransition(R.anim.book_close_in, R.anim.book_close_out)
        finish()
    }

    @Deprecated("Use goHome() for the book animation")
    override fun onBackPressed() {
        goHome()
    }

    private fun build() {
        val (root, col) = Ui.screenWithNav(this, "profile")
        val d = Store.data(this)
        val attempts = d.attempts.sortedByDescending { it.whenMs }
        val name = Guest.name(this)

        // ---- header: back arrow + title + help/gear (no bell, no avatar here) ----
        val headRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headRow.addView(TextView(this).apply {
            text = "‹"
            gravity = Gravity.CENTER
            setTextColor(Theme.ink(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(2), dp(10), dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener { goHome() }
        })
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(Ui.styleBody(TextView(this).apply {
            text = Lang.t("My Profile", "എന്റെ പ്രൊഫൈൽ")
            setTextColor(Theme.ink(this@ProfileActivity))
            setTypeface(typeface, Typeface.BOLD)
        }, 24f).apply {
            // styleBody sets size; force the 24sp headline (no +2 bump needed for Malayalam here is fine)
        })
        titleCol.addView(TextView(this).apply {
            text = Lang.t("Your study journey at a glance", "നിന്റെ പഠനയാത്ര ഒറ്റനോട്ടത്തിൽ")
            setTextColor(Theme.grey(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })
        headRow.addView(titleCol)
        val (actions, _) = Ui.actionButtons(this, withBell = false, withAvatar = false)
        headRow.addView(actions)
        headRow.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(6) }
        col.addView(headRow)

        // ---- greeting card (taps through to Settings for name/avatar) ----
        val greetCard = Ui.card(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Theme.tintBg(this@ProfileActivity), Theme.card(this@ProfileActivity))
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(
                    Intent(this@ProfileActivity, SettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
            }
        }
        val greetRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        greetRow.addView(Avatar.circle(this, Avatar.index(this), 56, 28f))
        val nameCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(12)
            }
        }
        nameCol.addView(Ui.styleBody(TextView(this).apply {
            text = if (name != null) Lang.t("Hi, $name! 👋", "ഹായ്, $name! 👋")
            else Lang.t("Hi there! 👋", "ഹായ്! 👋")
            setTextColor(Theme.ink(this@ProfileActivity))
            setTypeface(typeface, Typeface.BOLD)
        }, 20f))
        nameCol.addView(TextView(this).apply {
            text = Lang.t("Keep learning, keep growing", "പഠനം തുടരൂ, വളരൂ")
            setTextColor(Theme.grey(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        })
        greetRow.addView(nameCol)
        greetRow.addView(Ui.chevron(this))
        greetCard.addView(greetRow)
        col.addView(greetCard)

        // ---- YOUR IMPROVEMENT (v1.26): last 3 test records + delta ----
        val impRecords = Store.testRecords(this).takeLast(3)
        if (impRecords.isNotEmpty()) {
            val impCard = Ui.card(this)
            impCard.addView(TextView(this).apply {
                text = "📈  " + Lang.t("YOUR IMPROVEMENT", "നിന്റെ മുന്നേറ്റം")
                setTextColor(Theme.ink(this@ProfileActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(4) }
            })
            val impLabels = listOf(
                Lang.t("Previous Test", "അതിനുമുമ്പത്തെ ടെസ്റ്റ്"),
                Lang.t("Last Test", "കഴിഞ്ഞ ടെസ്റ്റ്"),
                Lang.t("Current Test", "ഇപ്പോഴത്തെ ടെസ്റ്റ്")
            )
            // Newest first: newest -> "Current Test", then "Last Test", then "Previous Test".
            val newestFirst = impRecords.reversed()
            newestFirst.forEachIndexed { i, r ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(6), 0, dp(6))
                }
                row.addView(TextView(this@ProfileActivity).apply {
                    text = impLabels[impLabels.size - newestFirst.size + i]
                    setTextColor(Theme.grey(this@ProfileActivity))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    layoutParams = LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                    )
                })
                row.addView(TextView(this@ProfileActivity).apply {
                    text = "%.1f%%".format(r.accuracy)
                    setTextColor(
                        CompletionScreen.accuracyColor(this@ProfileActivity, r.accuracy)
                    )
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                    setTypeface(typeface, Typeface.BOLD)
                })
                impCard.addView(row)
            }
            // Delta vs the oldest shown record (needs at least 2).
            if (newestFirst.size >= 2) {
                val delta = newestFirst.first().accuracy - newestFirst.last().accuracy
                val arrow = when {
                    delta > 0 -> "↗"
                    delta < 0 -> "↘"
                    else -> "→"
                }
                val dColor = when {
                    delta > 0 -> Theme.correct(this)
                    delta < 0 -> Theme.red(this)
                    else -> Theme.grey(this)
                }
                impCard.addView(TextView(this).apply {
                    text = "$arrow %+.1f%%".format(delta)
                    setTextColor(dColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(8) }
                })
            }
            col.addView(impCard)
        }

        // ---- APP USAGE (v1.27): today / this week / total ----
        val useCard = Ui.card(this)
        useCard.addView(TextView(this).apply {
            text = "⏱  " + Lang.t("APP USAGE", "ആപ്പ് ഉപയോഗം")
            setTextColor(Theme.ink(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        })
        val usageRows = listOf(
            Lang.t("Today", "ഇന്ന്") to Usage.todayTotalMs(this),
            Lang.t("This week", "ഈ ആഴ്ച") to Usage.weekMs(this),
            Lang.t("Total", "ആകെ") to Usage.totalMs(this)
        )
        usageRows.forEach { (label, ms) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            row.addView(TextView(this@ProfileActivity).apply {
                text = label
                setTextColor(Theme.grey(this@ProfileActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            })
            row.addView(TextView(this@ProfileActivity).apply {
                text = CompletionScreen.formatDuration(ms)
                setTextColor(Theme.green(this@ProfileActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
            })
            useCard.addView(row)
        }
        col.addView(useCard)

        // ---- Overview + All Time pill ----
        val overRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18); bottomMargin = dp(2) }
        }
        overRow.addView(TextView(this).apply {
            text = Lang.t("Overview", "അവലോകനം")
            setTextColor(Theme.ink(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        overRow.addView(Ui.staticPill(this, Lang.t("All Time ▾", "എല്ലാം ▾")))
        col.addView(overRow)

        if (attempts.isEmpty()) {
            val emptyCard = Ui.card(this)
            Ui.emptyNote(
                this, emptyCard,
                Lang.t(
                    "No attempts yet — finish a quiz to start tracking.",
                    "ഇതവരെ ക്വിസ് ചെയ്തിട്ടില്ല — ഒരെണ്ണം പൂർത്തിയാക്കൂ."
                )
            )
            col.addView(emptyCard)
        } else {
            val avg = attempts.map { it.pct }.average().roundToInt()
            val best = attempts.maxOf { it.pct }
            val statsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            }
            statsRow.addView(
                statItem(attempts.size.toString(),
                    Lang.t("Quizzes", "ക്വിസുകൾ"), Lang.t("Attempted", "ചെയ്തത്"))
            )
            statsRow.addView(
                statItem("$avg%",
                    Lang.t("Average", "ശരാശരി"), Lang.t("Score", "സ്കോർ")).apply {
                    (layoutParams as LinearLayout.LayoutParams).leftMargin = dp(10)
                }
            )
            statsRow.addView(
                statItem("$best%",
                    Lang.t("Best", "ബെസ്റ്റ്"), Lang.t("Score", "സ്കോർ")).apply {
                    (layoutParams as LinearLayout.LayoutParams).leftMargin = dp(10)
                }
            )
            col.addView(statsRow)

            // ---- Performance Trend ----
            val trendCard = Ui.card(this)
            val trendHead = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            trendHead.addView(TextView(this).apply {
                text = "📈  ${Lang.t("Performance Trend", "പ്രകടന ട്രെൻഡ്")}"
                setTextColor(Theme.ink(this@ProfileActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            trendHead.addView(Ui.staticPill(this, Lang.t("Last 10 Quizzes ▾", "അവസാന 10 ▾")))
            trendCard.addView(trendHead)
            trendCard.addView(TrendChartView(this, attempts.take(10).reversed()))
            col.addView(trendCard)

            // ---- Topic-wise Performance row ----
            col.addView(infoRowCard(
                "◔", Theme.mint(this),
                Lang.t("Topic-wise Performance", "ടോപ്പിക് തിരിച്ചുള്ള പ്രകടനം"),
                Lang.t("Topic-wise data starts from new quizzes.", "പുതിയ ക്വിസുകൾ മുതൽ ടോപ്പിക് തിരിച്ചുള്ള കണക്ക് കിട്ടും.")
            ) { showTopicDialog(d, attempts) })

            // ---- Study Streak row ----
            val streak = streakDays(attempts)
            col.addView(infoRowCard(
                "🔥", Theme.amberTint(this),
                Lang.t("Study Streak", "പഠന സ്ട്രീക്ക്"),
                Lang.t(
                    "%d day(s) in a row — do at least one quiz a day to keep it burning.",
                    "%d ദിവസം തുടർച്ചയായി — സ്ട്രീക്ക് നിലനിർത്താൻ ദിവസം ഒരു ക്വിസെങ്കിലും ചെയ്യൂ."
                ).format(streak)
            ) { showStreakDialog(streak) })

            // ---- Recent Activity + View All ----
            val recentHead = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(18); bottomMargin = dp(2) }
            }
            recentHead.addView(TextView(this).apply {
                text = Lang.t("Recent Activity", "സമീപകാല ആക്ടിവിറ്റി")
                setTextColor(Theme.ink(this@ProfileActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            recentHead.addView(TextView(this).apply {
                text = Lang.t("View All", "എല്ലാം കാണൂ")
                setTextColor(Theme.green(this@ProfileActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(Intent(this@ProfileActivity, HistoryActivity::class.java))
                }
            })
            col.addView(recentHead)
            val recentCard = Ui.card(this)
            attempts.take(5).forEach { a -> recentCard.addView(historyRow(a)) }
            col.addView(recentCard)
        }

        setContentView(root)
    }

    /** Plain stat — no card background/border/shadow: bold value + small label. */
    private fun statItem(value: String, line1: String, line2: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(12), dp(4), dp(12))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@ProfileActivity).apply {
                text = value
                setTextColor(Theme.ink(this@ProfileActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(Ui.wordWrap(TextView(this@ProfileActivity).apply {
                text = "$line1\n$line2"
                setTextColor(Theme.grey(this@ProfileActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(4) }
            }))
        }

    /** Tappable info row card: icon chip + title/subtitle + chevron. */
    private fun infoRowCard(
        icon: String, iconBg: Int, title: String, sub: String, onTap: () -> Unit
    ): LinearLayout {
        val card = Ui.card(this)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            setOnClickListener { onTap() }
        }
        row.addView(Ui.iconChip(this, icon, iconBg, boxDp = 46, emojiSp = 23f))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(12)
            }
        }
        col.addView(Ui.styleBody(TextView(this).apply {
            text = title
            setTextColor(Theme.ink(this@ProfileActivity))
            setTypeface(typeface, Typeface.BOLD)
        }, 14f))
        col.addView(TextView(this).apply {
            text = sub
            setTextColor(Theme.grey(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })
        row.addView(col)
        row.addView(Ui.chevron(this))
        card.addView(row)
        return card
    }

    /** Recent-activity row: icon chip, title + date, green score, chevron. */
    fun historyRow(a: Attempt): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            isClickable = true
            isFocusable = true
        }
        row.addView(Ui.iconChip(this, "📄", Theme.mint(this), boxDp = 44, emojiSp = 22f))
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(12)
            }
        }
        left.addView(Ui.styleBody(TextView(this).apply {
            text = a.topicName
            setTextColor(Theme.ink(this@ProfileActivity))
            setTypeface(typeface, Typeface.BOLD)
        }, 14f))
        left.addView(TextView(this).apply {
            text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(a.whenMs))
            setTextColor(Theme.grey(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        row.addView(left)
        val scoreCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        scoreCol.addView(TextView(this).apply {
            text = "${a.correct}/${a.total}"
            setTextColor(Theme.green(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.END
        })
        scoreCol.addView(TextView(this).apply {
            text = "(${a.pct}%)"
            setTextColor(Theme.green(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.END
        })
        row.addView(scoreCol)
        if (a.hasReview) {
            row.addView(Ui.chevron(this, bg = Theme.mint(this), circleDp = 30))
            row.setOnClickListener {
                startActivity(
                    Intent(this, PyqResultActivity::class.java)
                        .putExtra("paper_id", a.pyqPaperId)
                        .putExtra("picked", a.pyqPicked.toIntArray())
                        .putExtra("order", a.pyqOrder.toIntArray())
                        .putExtra("elapsed_ms", a.elapsedMs)
                        .putExtra("history_mode", true)
                )
            }
        }
        row.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle(Lang.t("Delete attempt?", "ശ്രമം ഡിലീറ്റ് ചെയ്യണോ?"))
                .setMessage("${a.topicName} • ${a.correct}/${a.total}")
                .setPositiveButton(Lang.t("Delete", "ഡിലീറ്റ് ചെയ്യൂ")) { d, _ ->
                    Store.deleteAttempt(this, a.id); build(); d.dismiss()
                }
                .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
                .show()
            true
        }
        return row
    }

    private fun showTopicDialog(d: Store.Data, attempts: List<Attempt>) {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        val agg = linkedMapOf<String, IntArray>()
        attempts.forEach { a ->
            a.perTopic.forEach { (tid, v) ->
                val e = agg.getOrPut(tid) { intArrayOf(0, 0) }
                e[0] += v.getOrElse(0) { 0 }; e[1] += v.getOrElse(1) { 0 }
            }
        }
        if (agg.isEmpty()) {
            col.addView(TextView(this).apply {
                text = Lang.t(
                    "Topic-wise data starts from new quizzes.",
                    "പുതിയ ക്വിസുകൾ മുതൽ ടോപ്പിക് തിരിച്ചുള്ള കണക്ക് കിട്ടും."
                )
                setTextColor(Theme.grey(this@ProfileActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            })
        } else {
            val topicNames = d.topics.associate { it.id to it.name }
            agg.entries.sortedByDescending { (_, v) ->
                if (v[1] == 0) 0 else (v[0] * 100) / v[1]
            }.forEach { (tid, v) ->
                val pct = if (v[1] == 0) 0 else (v[0] * 100) / v[1]
                val tname = topicNames[tid]
                    ?: if (tid == "pyq") Lang.t("📜 PYQ", "📜 PYQ")
                    else attempts.firstOrNull { it.perTopic.containsKey(tid) }?.topicName
                    ?: Lang.t("Deleted topic", "ഡിലീറ്റ് ചെയ്ത ടോപ്പിക്")
                col.addView(topicRow(tname, pct, v[0], v[1]))
            }
        }
        AlertDialog.Builder(this)
            .setTitle(Lang.t("Topic-wise Performance", "ടോപ്പിക് തിരിച്ചുള്ള പ്രകടനം"))
            .setView(ScrollView(this).apply { addView(col) })
            .setPositiveButton(Lang.t("Close", "അടയ്ക്കൂ"), null)
            .show()
    }

    private fun topicRow(tname: String, pct: Int, correct: Int, total: Int): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(TextView(this).apply {
            text = tname
            setTextColor(Theme.ink(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = "$correct/$total"
            setTextColor(Theme.grey(this@ProfileActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = dp(8) }
        })
        val color = when {
            pct >= 70 -> Theme.correct(this@ProfileActivity)
            pct >= 40 -> Theme.amber(this@ProfileActivity)
            else -> Theme.red(this@ProfileActivity)
        }
        row.addView(TextView(this).apply {
            text = "$pct%"
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })
        return row
    }

    private fun showStreakDialog(streak: Int) {
        AlertDialog.Builder(this)
            .setTitle(Lang.t("🔥 Study Streak", "🔥 പഠന സ്ട്രീക്ക്"))
            .setMessage(
                Lang.t(
                    "%d day(s) in a row — amazing! Do at least one quiz every day to keep it burning.",
                    "%d ദിവസം തുടർച്ചയായി — അടിപൊളി! സ്ട്രീക്ക് നിലനിർത്താൻ ദിവസവും ഒരു ക്വിസെങ്കിലും ചെയ്യൂ."
                ).format(streak)
            )
            .setPositiveButton(Lang.t("OK", "ശരി"), null)
            .show()
    }

    private fun streakDays(attempts: List<Attempt>): Int {
        if (attempts.isEmpty()) return 0
        val days = attempts.map { it.whenMs / 86_400_000L }.toSortedSet()
        val today = System.currentTimeMillis() / 86_400_000L
        var cursor = if (days.contains(today)) today else today - 1
        var streak = 0
        while (days.contains(cursor)) { streak++; cursor-- }
        return streak
    }
}

/** Hand-drawn trend line chart with a soft filled area, theme-aware. */
class TrendChartView(ctx: Context, private val attempts: List<Attempt>) : View(ctx) {
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Theme.green(ctx); strokeWidth = 5f; style = Paint.Style.STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Theme.green(ctx); alpha = 36; style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Theme.green(ctx); style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Theme.divider(ctx); strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Theme.grey(ctx); textSize = 26f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (170 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val padL = 64f; val padR = 16f; val padT = 16f; val padB = 42f
        val cw = w - padL - padR; val ch = h - padT - padB
        val baseY = padT + ch

        listOf(0, 25, 50, 75, 100).forEach { v ->
            val y = padT + ch * (1 - v / 100f)
            canvas.drawLine(padL, y, padL + cw, y, gridPaint)
            canvas.drawText("$v", 6f, y + 9f, textPaint)
        }
        if (attempts.size < 2) {
            canvas.drawText(
                Lang.t("Do one more quiz to see your trend.", "ഒരു ക്വിസ് കൂടി ചെയ്താൽ ട്രെൻഡ് കാണാം."),
                padL, padT + ch / 2, textPaint
            )
            return
        }
        val stepX = cw / (attempts.size - 1)
        val xs = FloatArray(attempts.size) { i -> padL + i * stepX }
        val ys = FloatArray(attempts.size) { i -> padT + ch * (1 - attempts[i].pct / 100f) }

        // Soft filled area under the line.
        val area = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until xs.size) lineTo(xs[i], ys[i])
            lineTo(xs.last(), baseY)
            lineTo(xs[0], baseY)
            close()
        }
        canvas.drawPath(area, fillPaint)

        for (i in 1 until xs.size) canvas.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i], linePaint)
        for (i in xs.indices) canvas.drawCircle(xs[i], ys[i], 8f, dotPaint)

        val fmt = SimpleDateFormat("dd/MM", Locale.getDefault())
        listOf(0, attempts.size / 2, attempts.size - 1).distinct().forEach { i ->
            canvas.drawText(fmt.format(Date(attempts[i].whenMs)), xs[i] - 28f, h - 8f, textPaint)
        }
    }
}
