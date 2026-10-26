package com.anandu.studyquiz

import org.json.JSONObject

/** A study topic with free-text notes. */
data class Topic(val id: String, var name: String, var notes: String)

/** One MCQ belonging to a topic. options always has 4 entries. */
data class Question(
    val id: String,
    var topicId: String,
    var text: String,
    var options: MutableList<String>,
    var correct: Int,
    var explanation: String = "",
    /** Optional asset image (question figure). Used for converted PYQ questions. */
    val img: String? = null,
    /** Optional per-option asset images (PYQ figure options). */
    val optImgs: List<String>? = null
)

/** One finished quiz run. perTopic maps topicId -> [correct, total]. */
data class Attempt(
    val id: String,
    val topicId: String,
    val topicName: String,
    val whenMs: Long,
    val correct: Int,
    val total: Int,
    val perTopic: Map<String, IntArray> = emptyMap(),
    // PYQ review support: the paper + the user's picks in attempted order.
    val pyqPaperId: String? = null,
    val pyqPicked: List<Int> = emptyList(),
    val pyqOrder: List<Int> = emptyList(),
    val elapsedMs: Long = 0L
) {
    val pct: Int get() = if (total <= 0) 0 else (correct * 100) / total
    /** True when this attempt can open a full question-by-question review. */
    val hasReview: Boolean get() = pyqPaperId != null
}

/**
 * Rich per-test record for improvement tracking (v1.26).
 * JSON-serializable with flat ISO field names so it can be synced to a
 * server later. Stored separately from the legacy [Attempt] history —
 * old attempts keep working unchanged.
 */
data class TestRecord(
    val testId: String,
    /** "quiz" | "mock" | "pyq" */
    val testKind: String,
    val testName: String,
    val total: Int,
    val correct: Int,
    val wrong: Int,
    val skipped: Int,
    /** 0..100, one decimal. */
    val accuracy: Double,
    val timeTakenMs: Long,
    /** topic name -> accuracy % (one decimal). Single entry when not topic-split. */
    val topicAccuracy: Map<String, Double>,
    val dateMs: Long
) {
    fun toJson(): JSONObject = JSONObject()
        .put("testId", testId)
        .put("testKind", testKind)
        .put("testName", testName)
        .put("total", total)
        .put("correct", correct)
        .put("wrong", wrong)
        .put("skipped", skipped)
        .put("accuracy", accuracy)
        .put("timeTakenMs", timeTakenMs)
        .put("topicAccuracy", JSONObject().apply {
            topicAccuracy.forEach { (k, v) -> put(k, v) }
        })
        .put("dateMs", dateMs)

    companion object {
        fun fromJson(o: JSONObject): TestRecord? = try {
            val ta = linkedMapOf<String, Double>()
            o.optJSONObject("topicAccuracy")?.let { jo ->
                jo.keys().forEach { k -> ta[k] = jo.optDouble(k, 0.0) }
            }
            TestRecord(
                o.optString("testId", "T" + o.optLong("dateMs", 0L)),
                o.optString("testKind", "quiz"),
                o.optString("testName", ""),
                o.optInt("total", 0),
                o.optInt("correct", 0),
                o.optInt("wrong", 0),
                o.optInt("skipped", 0),
                o.optDouble("accuracy", 0.0),
                o.optLong("timeTakenMs", 0L),
                ta,
                o.optLong("dateMs", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }
}
