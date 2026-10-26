package com.anandu.studyquiz

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.anandu.studyquiz.Ui.dp

/** Bundled update feed: new notifications, exam dates, last dates to apply. */
class NotificationsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)

        val (scroll, col) = Ui.screen(this)
        Ui.header(
            this, col,
            Lang.t("🔔 Notifications", "🔔 അറിയിപ്പുകൾ"),
            Lang.t("Latest updates.", "പുതിയ അറിയിപ്പുകൾ.")
        )

        val list = AppNotifications.all(this)
        if (list.isEmpty()) {
            Ui.emptyNote(
                this, col,
                Lang.t(
                    "No notifications yet.\nNew updates will appear here.",
                    "അറിയിപ്പുകളില്ല.\nപുതിയ updates ഇവിടെ വരും."
                )
            )
        } else {
            list.forEach { col.addView(notifCard(it)) }
        }

        AppNotifications.markAllSeen(this)
        setContentView(scroll)
    }

    private fun notifCard(n: AppNotification): LinearLayout {
        val card = Ui.card(this)
        card.addView(TextView(this).apply {
            text = n.title()
            setTextColor(Theme.ink(this@NotificationsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })
        if (n.date.isNotBlank()) {
            card.addView(TextView(this).apply {
                text = n.date
                setTextColor(Theme.grey(this@NotificationsActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(2) }
            })
        }
        if (n.body().isNotBlank()) {
            card.addView(TextView(this).apply {
                text = n.body()
                setTextColor(Theme.ink(this@NotificationsActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
            })
        }
        return card
    }
}
