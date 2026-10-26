package com.anandu.studyquiz

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Timetable slot alerts: a system notification when a slot starts, plus an
 * optional pre-alert a few minutes before (default 10 min). Reuses the
 * AlarmManager pattern from Reminder.kt. Alarms re-fire daily.
 */
object TimetableAlarms {
    private const val REQ_BASE = 4100
    const val EXTRA_SLOT_ID = "tt_slot_id"
    const val EXTRA_KIND = "tt_kind" // "start" | "pre"

    private fun pending(ctx: Context, reqCode: Int, slotId: String, kind: String): PendingIntent {
        val i = Intent(ctx, TimetableReceiver::class.java)
            .putExtra(EXTRA_SLOT_ID, slotId)
            .putExtra(EXTRA_KIND, kind)
        return PendingIntent.getBroadcast(
            ctx, reqCode, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reqCode(index: Int, kind: String): Int =
        REQ_BASE + index * 2 + if (kind == "pre") 1 else 0

    /** Cancel everything, then schedule today's remaining slots (+ tomorrow for passed ones). */
    fun scheduleAll(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Cancel a generous range of request codes.
        for (rc in REQ_BASE until REQ_BASE + 200) {
            am.cancel(
                PendingIntent.getBroadcast(
                    ctx, rc,
                    Intent(ctx, TimetableReceiver::class.java),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                ) ?: continue
            )
        }
        if (!TimetableStore.notifEnabled(ctx)) return
        val slots = TimetableStore.getSlots(ctx)
        val preMin = TimetableStore.preMinutes(ctx)
        slots.forEachIndexed { i, s ->
            scheduleAt(ctx, am, reqCode(i, "start"), s.id, "start", s.startMin)
            if (preMin > 0) scheduleAt(ctx, am, reqCode(i, "pre"), s.id, "pre", s.startMin - preMin)
        }
    }

    private fun scheduleAt(
        ctx: Context, am: AlarmManager, reqCode: Int,
        slotId: String, kind: String, triggerMin: Int
    ) {
        val mins = ((triggerMin % 1440) + 1440) % 1440
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, mins / 60)
            set(Calendar.MINUTE, mins % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        try {
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, cal.timeInMillis,
                pending(ctx, reqCode, slotId, kind)
            )
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, cal.timeInMillis,
                pending(ctx, reqCode, slotId, kind)
            )
        }
    }

    fun cancelAll(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (rc in REQ_BASE until REQ_BASE + 200) {
            PendingIntent.getBroadcast(
                ctx, rc,
                Intent(ctx, TimetableReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { am.cancel(it) }
        }
    }
}

/** Fires a timetable notification, then re-schedules all slots. */
class TimetableReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (!TimetableStore.notifEnabled(ctx)) return
        Lang.init(ctx)
        val slotId = intent.getStringExtra(TimetableAlarms.EXTRA_SLOT_ID) ?: return
        val kind = intent.getStringExtra(TimetableAlarms.EXTRA_KIND) ?: "start"
        val slot = TimetableStore.getSlots(ctx).find { it.id == slotId } ?: return
        showNotification(ctx, slot, kind)
        TimetableAlarms.scheduleAll(ctx) // roll this alarm to tomorrow
    }

    private fun showNotification(ctx: Context, slot: TSlot, kind: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chId = "timetable"
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    chId,
                    Lang.t("Timetable reminders", "ടൈംടേബിൾ ഓർമ്മപ്പെടുത്തൽ"),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val open = PendingIntent.getActivity(
            ctx, 4199,
            Intent(ctx, TimetableActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val (title, text) = if (kind == "pre") {
            val pre = TimetableStore.preMinutes(ctx)
            Lang.t(
                "⏰ ${slot.subject} in $pre min — get ready!",
                "⏰ $pre മിനിറ്റിനുള്ളിൽ ${slot.subject} — തയ്യാറാകൂ!"
            ) to Lang.t(
                "Slot: ${TimetableStore.fmtRange(slot)}",
                "സമയം: ${TimetableStore.fmtRange(slot)}"
            )
        } else {
            Lang.t(
                "📚 Time to study: ${slot.subject}",
                "📚 ഇപ്പോൾ ${slot.subject} പഠിക്കാൻ സമയമായി!"
            ) to Lang.t(
                "${TimetableStore.fmtDur(slot.durMin)} slot",
                "${TimetableStore.fmtDur(slot.durMin)} നേരം"
            )
        }
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, chId)
        else Notification.Builder(ctx)
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
        nm.notify(4200 + (slot.id.hashCode() and 0xFF) * 2 + if (kind == "pre") 1 else 0, b.build())
    }
}
