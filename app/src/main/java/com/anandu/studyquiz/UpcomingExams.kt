package com.anandu.studyquiz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Built-in upcoming exams bundled in the APK (assets/upcoming_exams.json).
 * Anandu supplies the list + notification PDFs; every APK release ships a
 * fresh snapshot. Read-only for users — no add/edit/delete.
 *
 * Schema:
 *   {sections: [{title_en, title_ml,
 *     exams: [{id, notif_no, post_en, post_ml?, date ("yyyy-MM-dd"|null),
 *              pdf (asset path like "notifications/17-2026.pdf"|null)}]}]}
 *
 * Never crashes on a bad/missing asset: falls back to empty sections.
 */
data class BuiltinExam(
    val id: String,
    val notifNo: String,
    val postEn: String,
    val postMl: String,
    val dateMs: Long?,   // midnight ms, null = not announced
    val pdf: String?     // asset path, null = coming soon
)

data class ExamSection(
    val titleEn: String,
    val titleMl: String,
    val exams: List<BuiltinExam>
)

object UpcomingExams {
    private const val ASSET = "upcoming_exams.json"
    private var cache: List<ExamSection>? = null

    fun sections(ctx: Context): List<ExamSection> {
        cache?.let { return it }
        val out = mutableListOf<ExamSection>()
        try {
            val text = ctx.assets.open(ASSET).bufferedReader().use { it.readText() }
            val secs = JSONObject(text).optJSONArray("sections") ?: JSONArray()
            for (s in 0 until secs.length()) {
                val so = secs.getJSONObject(s)
                val exams = mutableListOf<BuiltinExam>()
                val ea = so.optJSONArray("exams") ?: JSONArray()
                for (i in 0 until ea.length()) {
                    val o = ea.getJSONObject(i)
                    val postEn = o.optString("post_en", "")
                    exams.add(
                        BuiltinExam(
                            id = o.optString("id", "exam-$s-$i"),
                            notifNo = o.optString("notif_no", ""),
                            postEn = postEn,
                            postMl = o.optString("post_ml", "").ifBlank { postEn },
                            dateMs = parseDate(o.optString("date", "").takeIf { it.isNotBlank() }),
                            pdf = o.optString("pdf", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
                out.add(
                    ExamSection(
                        titleEn = so.optString("title_en", ""),
                        titleMl = so.optString("title_ml", "").ifBlank { so.optString("title_en", "") },
                        exams = exams
                    )
                )
            }
        } catch (_: Exception) {
            // Bad/missing asset: empty list, never crash.
        }
        cache = out
        return out
    }

    fun totalCount(ctx: Context): Int = sections(ctx).sumOf { it.exams.size }

    /** "yyyy-MM-dd" -> midnight ms, null on bad input. */
    private fun parseDate(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s) ?: return null
            Calendar.getInstance().apply {
                time = d
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (_: Exception) {
            null
        }
    }

    /** Whole days from today (local) to the date. Negative = passed. */
    fun daysToGo(dateMs: Long): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ((dateMs - today) / 86_400_000L).toInt()
    }

    fun countdownText(dateMs: Long?): String = when {
        dateMs == null -> Lang.t("Date not announced", "തീയതി പ്രഖ്യാപിച്ചിട്ടില്ല")
        else -> {
            val days = daysToGo(dateMs)
            when {
                days > 1 -> Lang.t("%d days to go", "%d ദിവസം ബാക്കി").format(days)
                days == 1 -> Lang.t("1 day to go! 🔥", "1 ദിവസം ബാക്കി! 🔥")
                days == 0 -> Lang.t("TODAY! 🎯", "ഇന്നാണ്! 🎯")
                else -> Lang.t("Done ✅", "കഴിഞ്ഞു ✅")
            }
        }
    }
}
