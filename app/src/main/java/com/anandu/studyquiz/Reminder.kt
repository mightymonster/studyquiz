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

private const val PREF = "studyquiz_prefs"

/** Daily study reminder preferences. */
object ReminderPrefs {
    private const val KEY_ON = "reminder_on"
    private const val KEY_H = "reminder_hour"
    private const val KEY_M = "reminder_min"

    fun enabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_ON, false)

    fun setEnabled(ctx: Context, v: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_ON, v).apply()
    }

    fun hour(ctx: Context): Int =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_H, 21)

    fun minute(ctx: Context): Int =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_M, 0)

    fun setTime(ctx: Context, h: Int, m: Int) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt(KEY_H, h).putInt(KEY_M, m).apply()
    }
}

/** Schedules / cancels the daily reminder alarm. */
object Reminder {
    private const val REQ = 2001

    private fun pending(ctx: Context): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            ctx, REQ, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, ReminderPrefs.hour(ctx))
            set(Calendar.MINUTE, ReminderPrefs.minute(ctx))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
        }
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(ctx))
        } catch (_: SecurityException) {
            // No exact-alarm permission — fall back to an inexact alarm.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending(ctx))
        }
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx))
    }
}

/** Fires the daily study reminder notification, then schedules tomorrow's. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (!ReminderPrefs.enabled(ctx)) return
        Lang.init(ctx)
        showNotification(ctx)
        Reminder.schedule(ctx) // next day
    }

    private fun showNotification(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chId = "study_reminder"
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    chId,
                    "പഠന ഓർമ്മപ്പെടുത്തൽ / Study reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val open = PendingIntent.getActivity(
            ctx, 2002,
            Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, chId)
        else Notification.Builder(ctx)
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("പഠിക്കാൻ സമയമായി! 📚 / Time to study!")
            .setContentText("ദിവസം ഒരു ക്വിസ് — സ്ട്രീക്ക് നിലനിർത്തൂ. / One quiz a day keeps the streak alive.")
            .setContentIntent(open)
            .setAutoCancel(true)
        nm.notify(3001, b.build())
    }
}

/** Re-schedule the daily reminder + timetable alarms after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (ReminderPrefs.enabled(ctx)) Reminder.schedule(ctx)
            TimetableAlarms.scheduleAll(ctx)
        }
    }
}
