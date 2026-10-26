package com.anandu.studyquiz

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Previous-year question papers bundled in the APK (assets/pyq.json).
 * Anandu sends PYQ question+answer PDFs; each release bundles a fresh
 * extraction. Read-only for users.
 *
 * Schema:
 *   {papers: [{id, category_no, title_en, title_ml?, year,
 *              questions: [{q, options[4], answer (index), img? (asset path),
 *                           optImgs? ([4] asset paths, per-option images)}]}]}
 *
 * Never crashes on a bad/missing asset: falls back to an empty list.
 */
data class PyqQuestion(
    val text: String,
    val options: List<String>,
    val answer: Int,
    val img: String? = null,
    val optImgs: List<String>? = null
)

data class PyqPaper(
    val id: String,
    val categoryNo: String,
    val titleEn: String,
    val titleMl: String,
    val year: String,
    val questions: List<PyqQuestion>
)

object PyqData {
    private const val ASSET = "pyq.json"
    private var cache: List<PyqPaper>? = null

    fun papers(ctx: Context): List<PyqPaper> {
        cache?.let { return it }
        val out = mutableListOf<PyqPaper>()
        try {
            val text = ctx.assets.open(ASSET).bufferedReader().use { it.readText() }
            val pa = JSONObject(text).optJSONArray("papers") ?: JSONArray()
            for (i in 0 until pa.length()) {
                val o = pa.getJSONObject(i)
                val titleEn = o.optString("title_en", "")
                val qs = mutableListOf<PyqQuestion>()
                val qa = o.optJSONArray("questions") ?: JSONArray()
                for (j in 0 until qa.length()) {
                    val qo = qa.getJSONObject(j)
                    val opts = mutableListOf<String>()
                    val oa = qo.optJSONArray("options") ?: JSONArray()
                    for (k in 0 until oa.length()) opts.add(oa.optString(k, ""))
                    while (opts.size < 4) opts.add("")
                    val ans = qo.optInt("answer", 0).coerceIn(0, 3)
                    val img = qo.optString("img", "").ifBlank { null }
                    val optImgs = qo.optJSONArray("optImgs")?.let { ia ->
                        val list = mutableListOf<String>()
                        for (k in 0 until ia.length()) list.add(ia.optString(k, ""))
                        if (list.size == 4 && list.any { it.isNotBlank() }) list else null
                    }
                    qs.add(PyqQuestion(qo.optString("q", ""), opts.take(4), ans, img, optImgs))
                }
                out.add(
                    PyqPaper(
                        id = o.optString("id", "pyq-$i"),
                        categoryNo = o.optString("category_no", ""),
                        titleEn = titleEn,
                        titleMl = o.optString("title_ml", "").ifBlank { titleEn },
                        year = o.optString("year", ""),
                        questions = qs
                    )
                )
            }
        } catch (_: Exception) {
            // Bad/missing asset: empty list, never crash.
        }
        cache = out
        return out
    }

    fun paper(ctx: Context, id: String): PyqPaper? =
        papers(ctx).firstOrNull { it.id == id }

    /** Stable ID for a PYQ question (index = position in the paper's question list). */
    fun questionId(paperId: String, index: Int): String = "pyq:$paperId:$index"

    /**
     * Papers grouped into year folders: newest year first; papers with a
     * missing/blank year go under a "" ("Other") group at the end.
     */
    fun papersByYear(ctx: Context): List<Pair<String, List<PyqPaper>>> {
        val byYear = papers(ctx).groupBy { it.year.trim() }
        val dated = byYear.keys.filter { it.isNotBlank() }.sortedDescending()
            .map { y -> y to (byYear[y] ?: emptyList()) }
        val other = byYear.filterKeys { it.isBlank() }.values.flatten()
        return if (other.isEmpty()) dated else dated + ("" to other)
    }
}
