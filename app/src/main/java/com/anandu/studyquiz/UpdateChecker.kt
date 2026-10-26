package com.anandu.studyquiz

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * In-app update check: fetches version.json from the GitHub repo and, if a
 * newer versionCode exists, shows a dialog with a Download button.
 * Silent on any failure (no internet, bad json, ...). No personal data sent.
 */
object UpdateChecker {
    private const val PREFS = "studyquiz_prefs"
    private const val KEY_LAST_CHECK = "last_update_check"
    private const val CHECK_INTERVAL_MS = 12L * 60 * 60 * 1000 // twice a day

    fun check(activity: Activity) {
        val url = Config.UPDATE_CHECK_URL
        if (url.isBlank()) return
        val prefs = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_CHECK, 0)
        if (System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return
        prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

        thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "StudyQuiz-Android")
                if (conn.responseCode != 200) return@thread
                val json = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                val remoteCode = Regex(""""versionCode"\s*:\s*(\d+)""")
                    .find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                if (remoteCode <= AppVersion.CODE) return@thread

                val apkUrl = Regex(""""apkUrl"\s*:\s*"([^"]+)"""")
                    .find(json)?.groupValues?.get(1).orEmpty()
                if (apkUrl.isBlank()) return@thread

                val notesKey = if (Lang.ml) "notesMl" else "notesEn"
                val notes = Regex(""""$notesKey"\s*:\s*"((?:[^"\\]|\\.)*)"""")
                    .find(json)?.groupValues?.get(1).orEmpty()
                    .replace("\\n", "\n").replace("\\\"", "\"")

                activity.runOnUiThread { showDialog(activity, apkUrl, notes) }
            } catch (_: Exception) {
                // silent — update check must never break the app
            }
        }
    }

    private fun showDialog(activity: Activity, apkUrl: String, notes: String) {
        if (activity.isFinishing) return
        val msg = buildString {
            append(
                Lang.t(
                    "A new version of StudyQuiz is available.",
                    "StudyQuiz-ന്റെ പുതിയ version വന്നിട്ടുണ്ട്."
                )
            )
            if (notes.isNotBlank()) append("\n\n").append(notes)
        }
        AlertDialog.Builder(activity)
            .setTitle(Lang.t("Update available", "അപ്ഡേറ്റ് ഉണ്ട്"))
            .setMessage(msg)
            .setPositiveButton(Lang.t("Download", "ഡൗൺലോഡ്")) { _, _ ->
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)))
                } catch (_: Exception) {
                }
            }
            .setNegativeButton(Lang.t("Later", "പിന്നീട്"), null)
            .show()
    }
}
