package com.anandu.studyquiz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

/** One upcoming exam: user-given name + date (midnight ms of the chosen day). */
data class Exam(val id: String, val name: String, val dateMs: Long)

private const val PREF = "studyquiz_prefs"

/**
 * Upcoming-exams list, persisted in SharedPreferences as JSON.
 * Replaces the old single-exam ExamPrefs; migrates the old date once.
 */
object Exams {
    private const val KEY_LIST = "exam_list_v1"
    private const val KEY_OLD = "exam_date_ms"
    private const val KEY_MIGRATED = "exam_list_migrated"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun all(ctx: Context): List<Exam> {
        val out = mutableListOf<Exam>()
        try {
            val arr = JSONArray(prefs(ctx).getString(KEY_LIST, "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Exam(
                        o.optString("id", UUID.randomUUID().toString()),
                        o.optString("name", ""),
                        o.optLong("dateMs", 0L)
                    )
                )
            }
        } catch (_: Exception) { /* corrupt -> treat as empty */ }
        return out.filter { it.name.isNotBlank() && it.dateMs != 0L }
    }

    private fun save(ctx: Context, exams: List<Exam>) {
        val arr = JSONArray()
        exams.forEach { e ->
            arr.put(JSONObject().put("id", e.id).put("name", e.name).put("dateMs", e.dateMs))
        }
        prefs(ctx).edit().putString(KEY_LIST, arr.toString()).apply()
    }

    fun add(ctx: Context, name: String, dateMs: Long): Exam {
        val e = Exam(UUID.randomUUID().toString(), name.trim(), dateMs)
        save(ctx, all(ctx) + e)
        return e
    }

    fun update(ctx: Context, id: String, name: String, dateMs: Long) {
        save(ctx, all(ctx).map { if (it.id == id) it.copy(name = name.trim(), dateMs = dateMs) else it })
    }

    fun delete(ctx: Context, id: String) {
        save(ctx, all(ctx).filter { it.id != id })
    }

    /**
     * One-time migration: if the old single exam date exists, import it as
     * one list entry and drop the old key. Call after Lang.init().
     */
    fun migrateIfNeeded(ctx: Context) {
        val p = prefs(ctx)
        if (p.getBoolean(KEY_MIGRATED, false)) return
        p.edit().putBoolean(KEY_MIGRATED, true).apply()
        val old = p.getLong(KEY_OLD, 0L)
        if (old != 0L) {
            add(ctx, Lang.t("CSEB Exam", "CSEB പരീക്ഷ"), old)
            p.edit().remove(KEY_OLD).apply()
        }
    }

    /** Whole days from today (local) to the exam date. Negative = passed. */
    fun daysToGo(dateMs: Long): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ((dateMs - today) / 86_400_000L).toInt()
    }

    /** Upcoming first (nearest first), then past (most recent first). */
    fun sorted(ctx: Context): List<Exam> {
        val (past, upcoming) = all(ctx).partition { daysToGo(it.dateMs) < 0 }
        return upcoming.sortedBy { it.dateMs } + past.sortedByDescending { it.dateMs }
    }
}
