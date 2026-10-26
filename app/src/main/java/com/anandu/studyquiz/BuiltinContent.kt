package com.anandu.studyquiz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Read-only built-in content bundled inside the APK
 * (assets/builtin_content.json). Anandu supplies the content; every APK
 * release ships a fresh snapshot — users never edit it.
 *
 * Schema:
 *   {topics: [{id, name, notes,
 *     questions: [{q, options[4], answer (index), explanation}]}]}
 *
 * Ships empty until Anandu sends real content; the app shows a friendly
 * empty state in that case and never crashes.
 */
object BuiltinContent {
    private const val ASSET = "builtin_content.json"
    private var cache: Pair<List<Topic>, List<Question>>? = null

    fun load(ctx: Context): Pair<List<Topic>, List<Question>> {
        cache?.let { return it }
        val topics = mutableListOf<Topic>()
        val questions = mutableListOf<Question>()
        try {
            val text = ctx.assets.open(ASSET).bufferedReader().use { it.readText() }
            val arr = JSONObject(text).optJSONArray("topics") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id", "").ifBlank { "t$i" }
                val name = o.optString("name", "").ifBlank { "Topic ${i + 1}" }
                topics.add(Topic(id, name, o.optString("notes", "")))
                val qs = o.optJSONArray("questions") ?: JSONArray()
                for (j in 0 until qs.length()) {
                    val q = qs.getJSONObject(j)
                    val opts = mutableListOf<String>()
                    q.optJSONArray("options")?.let { oa ->
                        for (k in 0 until oa.length()) opts.add(oa.optString(k, ""))
                    }
                    while (opts.size < 4) opts.add("")
                    questions.add(
                        Question(
                            id = "$id-q$j",
                            topicId = id,
                            text = q.optString("q", ""),
                            options = opts,
                            correct = q.optInt("answer", 0).coerceIn(0, 3),
                            explanation = q.optString("explanation", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Bad/missing asset: fall through with empty content, never crash.
        }
        val result = topics to questions
        cache = result
        return result
    }
}
