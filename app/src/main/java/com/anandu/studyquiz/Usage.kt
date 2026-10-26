package com.anandu.studyquiz

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Lightweight app-usage tracking. Per-day totals live in SharedPreferences
 * ("studyquiz_usage", keys "usage_yyyy-MM-dd"); the running session is kept
 * in memory per process run. All reads are crash-safe (0 on failure).
 */
object Usage {
    private const val PREF = "studyquiz_usage"
    private const val PREFIX = "usage_"
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Process-run session start (ms); 0 = unknown (omit usage displays). */
    var sessionStartMs: Long = 0L
        private set

    /** Call once from MainActivity.onCreate — rotation-safe. */
    fun sessionStarted() {
        if (sessionStartMs == 0L) sessionStartMs = System.currentTimeMillis()
    }

    fun sessionElapsedMs(): Long =
        if (sessionStartMs == 0L) 0L
        else (System.currentTimeMillis() - sessionStartMs).coerceAtLeast(0L)

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun dayKey(offsetDays: Int = 0): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return PREFIX + dayFmt.format(cal.time)
    }

    private fun storedMs(ctx: Context, key: String): Long = try {
        prefs(ctx).getLong(key, 0L)
    } catch (_: Exception) {
        0L
    }

    /** Today's banked total (excludes the running session). */
    fun todayStoredMs(ctx: Context): Long = storedMs(ctx, dayKey())

    /** Today including the running session — for display. */
    fun todayTotalMs(ctx: Context): Long = todayStoredMs(ctx) + sessionElapsedMs()

    /** Last 7 days including the running session — for display. */
    fun weekMs(ctx: Context): Long {
        var sum = 0L
        for (i in 0..6) sum += storedMs(ctx, dayKey(-i))
        return sum + sessionElapsedMs()
    }

    /** All banked days including the running session — for display. */
    fun totalMs(ctx: Context): Long = try {
        var sum = 0L
        for ((k, v) in prefs(ctx).all) {
            if (k.startsWith(PREFIX) && v is Long) sum += v
        }
        sum + sessionElapsedMs()
    } catch (_: Exception) {
        sessionElapsedMs()
    }

    /**
     * Bank the elapsed session time into today's total and prune daily keys
     * older than 90 days. Resets the session clock so re-banking is harmless.
     */
    fun bankSession(ctx: Context) {
        try {
            val elapsed = sessionElapsedMs()
            val p = prefs(ctx)
            val ed = p.edit()
            if (elapsed > 0) {
                val key = dayKey()
                ed.putLong(key, p.getLong(key, 0L) + elapsed)
            }
            // ISO dates sort lexicographically — drop anything older than 90 days.
            val cutoff = dayKey(-90)
            for (k in p.all.keys) {
                if (k.startsWith(PREFIX) && k < cutoff) ed.remove(k)
            }
            ed.apply()
        } catch (_: Exception) {
        }
        sessionStartMs = System.currentTimeMillis()
    }
}
