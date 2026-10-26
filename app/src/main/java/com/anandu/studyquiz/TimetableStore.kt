package com.anandu.studyquiz

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** One study slot in the daily timetable. Times are minutes since midnight. */
data class TSlot(
    val id: String,
    val subject: String,
    val startMin: Int,
    val durMin: Int
) {
    val endMin: Int get() = startMin + durMin
}

/** Timetable persistence: slots, per-day completion, notification prefs. */
object TimetableStore {
    private const val PREF = "studyquiz_prefs"
    private const val KEY_SLOTS = "tt_slots"
    private const val KEY_NOTIF = "tt_notif"
    private const val KEY_PREMIN = "tt_premin"

    /** (English, Malayalam) subject names offered in setup. */
    val SUBJECTS: List<Pair<String, String>> = listOf(
        "Act & Rules" to "ആക്ട് & റൂൾസ്",
        "Co-operation" to "സഹകരണം",
        "Banking" to "ബാങ്കിംഗ്",
        "Accounting" to "അക്കൗണ്ടൻസി",
        "Reasoning" to "റീസണിംഗ്",
        "English" to "ഇംഗ്ലീഷ്",
        "GK" to "പൊതുവിജ്ഞാനം",
        "Daily Revision" to "ഡെയിലി റിവിഷൻ",
        "PYQ" to "PYQ",
        "Practice Quiz" to "പ്രാക്ടീസ് ക്വിസ്",
        "Mock Test" to "മോക്ക് ടെസ്റ്റ്"
    )

    fun subjectName(en: String, ml: String): String = Lang.t(en, ml)

    /** Map a stored subject name to the current locale's display name. */
    fun displayName(stored: String): String {
        SUBJECTS.forEach { (en, ml) ->
            if (stored == en || stored == ml) return Lang.t(en, ml)
        }
        return stored
    }

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun getSlots(ctx: Context): List<TSlot> {
        val raw = prefs(ctx).getString(KEY_SLOTS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { part ->
            val f = part.split("|")
            if (f.size != 4) return@mapNotNull null
            val start = f[2].toIntOrNull() ?: return@mapNotNull null
            val dur = f[3].toIntOrNull() ?: return@mapNotNull null
            TSlot(f[0], f[1], start, dur)
        }.sortedWith(compareBy({ it.startMin }, { it.id }))
    }

    fun saveSlots(ctx: Context, slots: List<TSlot>) {
        val raw = slots.sortedWith(compareBy({ it.startMin }, { it.id }))
            .joinToString(";") { "${it.id}|${it.subject}|${it.startMin}|${it.durMin}" }
        prefs(ctx).edit().putString(KEY_SLOTS, raw).apply()
    }

    fun hasTimetable(ctx: Context): Boolean = getSlots(ctx).isNotEmpty()

    fun newId(): String = UUID.randomUUID().toString().take(8)

    // ---------- per-day completion ----------
    private fun doneKey(): String =
        "tt_done_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun doneIds(ctx: Context): MutableSet<String> =
        prefs(ctx).getStringSet(doneKey(), emptySet())?.toMutableSet() ?: mutableSetOf()

    fun setDone(ctx: Context, slotId: String, done: Boolean) {
        val s = doneIds(ctx)
        if (done) s.add(slotId) else s.remove(slotId)
        prefs(ctx).edit().putStringSet(doneKey(), s).apply()
    }

    // ---------- notification prefs ----------
    fun notifEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_NOTIF, true)

    fun setNotifEnabled(ctx: Context, v: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_NOTIF, v).apply()
    }

    /** Minutes before a slot to fire the pre-alert. 0 = off. */
    fun preMinutes(ctx: Context): Int = prefs(ctx).getInt(KEY_PREMIN, 10)

    fun setPreMinutes(ctx: Context, v: Int) {
        prefs(ctx).edit().putInt(KEY_PREMIN, v).apply()
    }

    /** Default timetable (7:30 AM – 11 PM) used to pre-fill first setup. */
    fun defaultSlots(): List<TSlot> = listOf(
        TSlot(newId(), "Act & Rules", 7 * 60 + 30, 90),
        TSlot(newId(), "Co-operation", 9 * 60 + 30, 90),
        TSlot(newId(), "Banking", 11 * 60 + 15, 90),
        TSlot(newId(), "Accounting", 14 * 60, 90),
        TSlot(newId(), "Reasoning", 16 * 60, 90),
        TSlot(newId(), "English", 18 * 60 + 30, 60),
        TSlot(newId(), "GK", 20 * 60, 60),
        TSlot(newId(), "Daily Revision", 21 * 60 + 15, 60),
        TSlot(newId(), "Practice Quiz", 22 * 60 + 15, 45)
    )

    /** "7:30 AM" style. */
    fun fmtTime(mins: Int): String {
        val h = (mins / 60) % 24
        val m = mins % 60
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = when (h % 12) { 0 -> 12; else -> h % 12 }
        return "%d:%02d %s".format(h12, m, ampm)
    }

    fun fmtRange(s: TSlot): String = "${fmtTime(s.startMin)}–${fmtTime(s.endMin)}"

    fun fmtDur(mins: Int): String = when {
        mins < 60 -> Lang.t("$mins min", "$mins മിനിറ്റ്")
        mins % 60 == 0 -> Lang.t("${mins / 60} hr", "${mins / 60} മണിക്കൂർ")
        else -> Lang.t("${mins / 60}h ${mins % 60}m", "${mins / 60} മണിക്കൂർ ${mins % 60} മിനിറ്റ്")
    }
}
