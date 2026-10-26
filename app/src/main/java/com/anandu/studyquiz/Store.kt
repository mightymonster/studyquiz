package com.anandu.studyquiz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Tiny JSON-file database. READ-ONLY content (topics + questions) comes from
 * the APK's built-in asset (BuiltinContent); only USER data — attempts,
 * bookmarks, wrongIds — lives in studyquiz.json in internal storage.
 * Loaded once per process and written on every mutation.
 */
object Store {
    private const val FILE = "studyquiz.json"
    const val ALL = "all"

    class Data {
        val topics = mutableListOf<Topic>()
        val questions = mutableListOf<Question>()
        val attempts = mutableListOf<Attempt>()
        val bookmarks = mutableSetOf<String>()
        val wrongIds = mutableSetOf<String>()
    }

    private var cache: Data? = null

    fun data(ctx: Context): Data {
        cache?.let { return it }
        val d = Data()
        // Built-in read-only content from the APK.
        val (topics, questions) = BuiltinContent.load(ctx)
        d.topics.addAll(topics)
        d.questions.addAll(questions)
        // User data from internal storage.
        try {
            val f = ctx.getFileStreamPath(FILE)
            if (f.exists()) {
                val root = JSONObject(f.readText())
                root.optJSONArray("attempts")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val perTopic = mutableMapOf<String, IntArray>()
                        o.optJSONObject("perTopic")?.let { pt ->
                            pt.keys().forEach { k ->
                                val a = pt.optJSONArray(k)
                                if (a != null && a.length() >= 2) {
                                    perTopic[k] = intArrayOf(a.optInt(0), a.optInt(1))
                                }
                            }
                        }
                        d.attempts.add(
                            Attempt(
                                o.getString("id"), o.getString("topicId"),
                                o.optString("topicName", ""), o.optLong("whenMs", 0L),
                                o.optInt("correct", 0), o.optInt("total", 0), perTopic,
                                o.optString("pyqPaperId").ifEmpty { null },
                                o.optJSONArray("pyqPicked")?.let { a ->
                                    List(a.length()) { a.optInt(it, -1) }
                                } ?: emptyList(),
                                o.optJSONArray("pyqOrder")?.let { a ->
                                    List(a.length()) { a.optInt(it, -1) }
                                } ?: emptyList(),
                                o.optLong("elapsedMs", 0L)
                            )
                        )
                    }
                }
                root.optJSONArray("bookmarks")?.let { arr ->
                    for (i in 0 until arr.length()) d.bookmarks.add(arr.optString(i))
                }
                root.optJSONArray("wrongIds")?.let { arr ->
                    for (i in 0 until arr.length()) d.wrongIds.add(arr.optString(i))
                }
            }
        } catch (_: Exception) {
            // Corrupt file: start fresh rather than crash.
        }
        cache = d
        return d
    }

    fun save(ctx: Context) {
        val d = cache ?: return
        try {
            val root = JSONObject()
            // User data only — topics/questions are read-only from the APK.
            root.put("attempts", JSONArray().apply {
                d.attempts.forEach { a ->
                    val pt = JSONObject()
                    a.perTopic.forEach { (k, v) ->
                        pt.put(k, JSONArray().put(v.getOrElse(0) { 0 }).put(v.getOrElse(1) { 0 }))
                    }
                    put(
                        JSONObject().put("id", a.id).put("topicId", a.topicId)
                            .put("topicName", a.topicName).put("whenMs", a.whenMs)
                            .put("correct", a.correct).put("total", a.total)
                            .put("perTopic", pt)
                            .put("pyqPaperId", a.pyqPaperId)
                            .put("pyqPicked", JSONArray(a.pyqPicked))
                            .put("pyqOrder", JSONArray(a.pyqOrder))
                            .put("elapsedMs", a.elapsedMs)
                    )
                }
            })
            root.put("bookmarks", JSONArray().apply { d.bookmarks.forEach { put(it) } })
            root.put("wrongIds", JSONArray().apply { d.wrongIds.forEach { put(it) } })
            ctx.openFileOutput(FILE, Context.MODE_PRIVATE).use {
                it.write(root.toString().toByteArray())
            }
        } catch (_: Exception) {
        }
    }

    fun newId(): String = UUID.randomUUID().toString()

    private fun readText(ctx: Context): String =
        ctx.getFileStreamPath(FILE).let { if (it.exists()) it.readText() else "{}" }

    private fun writeText(ctx: Context, s: String) {
        try {
            ctx.openFileOutput(FILE, Context.MODE_PRIVATE).use {
                it.write(s.toByteArray())
            }
        } catch (_: Exception) {
        }
    }

    // NOTE: topics/questions are read-only built-in content (BuiltinContent).
    // There are intentionally no add/update/delete methods for them.

    // ---------- bookmarks ----------
    /** Toggle bookmark. Returns true if now bookmarked. */
    fun toggleBookmark(ctx: Context, qid: String): Boolean {
        val s = data(ctx).bookmarks
        val now = if (s.contains(qid)) { s.remove(qid); false } else { s.add(qid); true }
        save(ctx)
        return now
    }

    fun isBookmarked(ctx: Context, qid: String): Boolean = data(ctx).bookmarks.contains(qid)

    fun bookmarkCount(ctx: Context): Int = data(ctx).bookmarks.size

    fun bookmarkedQuestions(ctx: Context): List<Question> {
        val ids = data(ctx).bookmarks
        return data(ctx).questions.filter { it.id in ids } + pyqConverted(ctx, ids)
    }

    /**
     * PYQ questions converted to [Question] so bookmarks / wrong-only flows
     * (QuizActivity, BookmarksActivity) can revise them like builtin ones.
     * topicId "pyq" marks the origin; img/optImgs carry the figures.
     */
    private fun pyqConverted(ctx: Context, ids: Set<String>): List<Question> {
        if (ids.none { it.startsWith("pyq:") }) return emptyList()
        val out = mutableListOf<Question>()
        PyqData.papers(ctx).forEach { paper ->
            paper.questions.forEachIndexed { i, pq ->
                val id = PyqData.questionId(paper.id, i)
                if (id in ids) {
                    val label = "📜 ${paper.categoryNo}"
                    val text = if (pq.text.isNotBlank()) "$label • ${pq.text}" else label
                    out.add(
                        Question(
                            id, "pyq", text, pq.options.toMutableList(),
                            pq.answer, "", pq.img, pq.optImgs
                        )
                    )
                }
            }
        }
        return out
    }

    fun removeBookmark(ctx: Context, qid: String) {
        if (data(ctx).bookmarks.remove(qid)) save(ctx)
    }

    // ---------- wrong answers ----------
    fun addWrong(ctx: Context, qid: String) {
        if (data(ctx).wrongIds.add(qid)) save(ctx)
    }

    fun removeWrong(ctx: Context, qid: String) {
        if (data(ctx).wrongIds.remove(qid)) save(ctx)
    }

    fun wrongCount(ctx: Context): Int = data(ctx).wrongIds.size

    fun wrongQuestions(ctx: Context): List<Question> {
        val ids = data(ctx).wrongIds
        return data(ctx).questions.filter { it.id in ids } + pyqConverted(ctx, ids)
    }

    fun questionById(ctx: Context, qid: String): Question? =
        data(ctx).questions.find { it.id == qid }

    /**
     * Like [questionById] but also resolves PYQ-converted questions
     * ("pyq:..." ids) — for reviewing wrong-only / bookmark sessions that
     * mix builtin and PYQ questions.
     */
    fun questionByIdAny(ctx: Context, qid: String): Question? =
        data(ctx).questions.find { it.id == qid }
            ?: pyqConverted(ctx, setOf(qid)).firstOrNull()

    // ---------- improvement tracking (v1.26) ----------
    private const val RECORDS_PREFS = "studyquiz_records"
    private const val RECORDS_KEY = "test_records"

    /** One-decimal accuracy %, never throws. */
    fun accuracy1dp(correct: Int, total: Int): Double =
        if (total <= 0) 0.0 else (correct * 1000.0 / total).roundToInt() / 10.0

    /** All rich test records, oldest first. Never throws — corrupt data yields []. */
    fun testRecords(ctx: Context): List<TestRecord> {
        return try {
            val raw = ctx.getSharedPreferences(RECORDS_PREFS, Context.MODE_PRIVATE)
                .getString(RECORDS_KEY, null) ?: return emptyList()
            val arr = JSONArray(raw)
            val out = mutableListOf<TestRecord>()
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { TestRecord.fromJson(it)?.let { r -> out.add(r) } }
            }
            out.sortedBy { it.dateMs }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Append one rich test record. The legacy Attempt history is NOT touched,
     * so old data and backups keep working exactly as before.
     */
    fun recordTestRecord(ctx: Context, r: TestRecord) {
        try {
            val prefs = ctx.getSharedPreferences(RECORDS_PREFS, Context.MODE_PRIVATE)
            val arr = JSONArray(prefs.getString(RECORDS_KEY, null) ?: "[]")
            arr.put(r.toJson())
            while (arr.length() > 500) arr.remove(0) // bound storage
            prefs.edit().putString(RECORDS_KEY, arr.toString()).apply()
        } catch (_: Exception) {
        }
    }

    /**
     * perTopic (topicId -> [correct, total]) converted to display-name ->
     * accuracy % for a TestRecord. Never throws; falls back to raw ids.
     */
    fun topicAccuracyNamed(ctx: Context, perTopic: Map<String, IntArray>): Map<String, Double> {
        val out = linkedMapOf<String, Double>()
        try {
            perTopic.forEach { (tid, v) ->
                val t = v.getOrElse(1) { 0 }
                val c = v.getOrElse(0) { 0 }
                val name = try { topicName(ctx, tid) } catch (_: Exception) { tid }
                out[name] = accuracy1dp(c, t)
            }
        } catch (_: Exception) {
        }
        return out
    }

    // ---------- attempts ----------
    fun recordAttempt(
        ctx: Context, topicId: String, topicName: String,
        correct: Int, total: Int,
        perTopic: Map<String, IntArray> = emptyMap(),
        pyqPaperId: String? = null,
        pyqPicked: List<Int> = emptyList(),
        pyqOrder: List<Int> = emptyList(),
        elapsedMs: Long = 0L
    ): Attempt {
        val a = Attempt(
            newId(), topicId, topicName, System.currentTimeMillis(), correct, total, perTopic,
            pyqPaperId, pyqPicked, pyqOrder, elapsedMs
        )
        data(ctx).attempts.add(a)
        save(ctx)
        return a
    }

    fun deleteAttempt(ctx: Context, id: String) {
        data(ctx).attempts.removeAll { it.id == id }
        save(ctx)
    }

    fun questionsFor(ctx: Context, topicId: String): List<Question> {
        val all = data(ctx).questions
        return if (topicId == ALL) all.toList() else all.filter { it.topicId == topicId }
    }

    fun questionCount(ctx: Context, topicId: String): Int = questionsFor(ctx, topicId).size

    fun topicName(ctx: Context, topicId: String): String =
        if (topicId == ALL) Lang.t("All topics", "എല്ലാ ടോപ്പിക്കുകളും")
        else data(ctx).topics.find { it.id == topicId }?.name ?: Lang.t("Topic", "ടോപ്പിക്")

    /** Most recent attempt for a scope, or null. */
    fun lastAttempt(ctx: Context, topicId: String): Attempt? =
        data(ctx).attempts.filter { it.topicId == topicId }.maxByOrNull { it.whenMs }

    /** Raw JSON string of the whole database (for backup export). */
    fun rawJson(ctx: Context): String = readText(ctx)

    /**
     * Replace the user data from an imported JSON string. Only user-data keys
     * (attempts/bookmarks/wrongIds) are restored — topics/questions always
     * come from the APK's built-in content. Old backups (which also contain
     * topics/questions) are accepted; their content keys are ignored.
     * Returns false if the JSON is not a valid StudyQuiz backup.
     */
    fun importJson(ctx: Context, json: String): Boolean {
        return try {
            val root = JSONObject(json)
            val filtered = JSONObject()
            listOf("attempts", "bookmarks", "wrongIds").forEach { k ->
                if (root.has(k)) filtered.put(k, root.get(k))
            }
            if (!filtered.has("attempts") && !filtered.has("bookmarks") && !filtered.has("wrongIds")) return false
            writeText(ctx, filtered.toString())
            cache = null
            true
        } catch (e: Exception) {
            false
        }
    }
}
