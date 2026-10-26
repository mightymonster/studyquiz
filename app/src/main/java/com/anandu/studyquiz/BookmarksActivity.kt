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

/** Lists bookmarked questions; tap one to revise it, or revise them all. */
class BookmarksActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        build()
    }

    override fun onResume() {
        super.onResume()
        build() // refresh after un-bookmarking inside a quiz
    }

    private fun build() {
        val (scroll, col) = Ui.screen(this)
        bookmarkHeader(col)

        val list = Store.bookmarkedQuestions(this)
        if (list.isEmpty()) {
            Ui.emptyNote(
                this, col,
                Lang.t(
                    "No bookmarks yet.\nTap ☆ on any question during a quiz to star it.",
                    "ബുക്ക്മാർക്കുകളില്ല.\nക്വിസിനിടെ ചോദ്യത്തിന് അടുത്തുള്ള ☆ അമർത്തി സ്റ്റാർ ചെയ്യൂ."
                )
            )
        } else {
            val reviseAll = Ui.button(
                this,
                Lang.t("▶ Revise all (%d)", "▶ എല്ലാം revise ചെയ്യൂ (%d)").format(list.size)
            )
            reviseAll.setOnClickListener {
                startActivity(
                    Intent(this, QuizActivity::class.java).putExtra("mode", "bookmarks")
                )
            }
            col.addView(reviseAll)

            val topicNames = Store.data(this).topics.associate { it.id to it.name }
            list.forEach { q ->
                val tname = topicNames[q.topicId]
                    ?: if (q.topicId == "pyq") Lang.t("📜 PYQ", "📜 PYQ") else null
                col.addView(bookmarkRow(q, tname))
            }
        }
        setContentView(scroll)
    }

    /**
     * Custom header: back arrow top-left (→ Home), NO icons top-right,
     * medium-weight title (not bulky bold).
     */
    private fun bookmarkHeader(col: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply {
            text = "‹"
            gravity = Gravity.CENTER
            setTextColor(Theme.ink(this@BookmarksActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(2), dp(10), dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener { finish() } // back to Home
        })
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = Lang.t("Bookmarks", "ബുക്ക്മാർക്കുകൾ")
            setTextColor(Theme.ink(this@BookmarksActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL) // medium, not bulky
        })
        titleCol.addView(TextView(this).apply {
            text = Lang.t(
                "Your starred questions — revise them here.",
                "സ്റ്റാർ ചെയ്ത ചോദ്യങ്ങൾ — ഇവിടെ revise ചെയ്യൂ."
            )
            setTextColor(Theme.grey(this@BookmarksActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })
        row.addView(titleCol)
        // top-right intentionally left empty — no icons
        col.addView(row)
        (row.layoutParams as? LinearLayout.LayoutParams)?.apply { bottomMargin = dp(4) }
    }

    private fun bookmarkRow(q: Question, topicName: String?): LinearLayout {
        val card = Ui.card(this)
        card.isClickable = true
        card.isFocusable = true
        card.setOnClickListener {
            startActivity(
                Intent(this, QuizActivity::class.java)
                    .putExtra("mode", "bookmarks")
                    .putExtra("singleId", q.id)
            )
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(this).apply {
            text = q.text
            setTextColor(Theme.ink(this@BookmarksActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val unstar = TextView(this).apply {
            text = "⭐"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setPadding(dp(8), dp(4), dp(4), dp(4))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                Store.removeBookmark(this@BookmarksActivity, q.id)
                build()
            }
        }
        row.addView(unstar)
        card.addView(row)

        if (!topicName.isNullOrBlank()) {
            card.addView(TextView(this).apply {
                text = topicName
                setTextColor(Theme.grey(this@BookmarksActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(4) }
            })
        }
        return card
    }
}
