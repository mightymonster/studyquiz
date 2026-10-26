package com.anandu.studyquiz

import android.content.Context
import org.json.JSONObject

/** One bundled notification item (newest first in the asset). */
data class AppNotification(
    val id: String,
    val date: String,
    val titleEn: String,
    val titleMl: String,
    val bodyEn: String,
    val bodyMl: String
) {
    fun title(): String = Lang.t(titleEn, titleMl)
    fun body(): String = Lang.t(bodyEn, bodyMl)
}

/** Loads assets/app_notifications.json; tracks seen ids for the bell badge. */
object AppNotifications {
    private const val PREFS = "studyquiz_prefs"
    private const val SEEN_KEY = "notif_seen_ids"

    fun all(ctx: Context): List<AppNotification> {
        val list = mutableListOf<AppNotification>()
        try {
            val text = ctx.assets.open("app_notifications.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(text).optJSONArray("notifications") ?: return emptyList()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id")
                if (id.isBlank()) continue
                list.add(
                    AppNotification(
                        id,
                        o.optString("date"),
                        o.optString("title_en"),
                        o.optString("title_ml"),
                        o.optString("body_en"),
                        o.optString("body_ml")
                    )
                )
            }
        } catch (_: Exception) {
            // bad/missing asset -> empty list, never crash
        }
        return list
    }

    fun unreadCount(ctx: Context): Int {
        val seen = seenIds(ctx)
        return all(ctx).count { it.id !in seen }
    }

    fun markAllSeen(ctx: Context) {
        val ids = all(ctx).joinToString(",") { it.id }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(SEEN_KEY, ids).apply()
    }

    private fun seenIds(ctx: Context): Set<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(SEEN_KEY, "").orEmpty()
            .split(",").filter { it.isNotBlank() }.toSet()
}
