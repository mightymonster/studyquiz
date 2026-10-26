package com.anandu.studyquiz

import android.content.Context

/**
 * Bilingual UI: every user-visible string goes through [t].
 * Language is stored in SharedPreferences and toggled in-app.
 */
object Lang {
    private const val PREF = "studyquiz_prefs"
    private const val KEY_ML = "ml"

    var ml: Boolean = false
        private set

    fun init(ctx: Context) {
        ml = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_ML, false)
    }

    fun setMalayalam(ctx: Context, value: Boolean) {
        ml = value
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_ML, value).apply()
    }

    /** Pick the string for the current language. */
    fun t(en: String, ml: String): String = if (Lang.ml) ml else en
}

/** Guest profile (local only — no login, no server). */
object Guest {
    private const val PREF = "studyquiz_prefs"
    private const val KEY_NAME = "guest_name"

    fun name(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_NAME, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun setName(ctx: Context, name: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY_NAME, name.trim()).apply()
    }

    fun initial(ctx: Context): String =
        name(ctx)?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}

/** Wiring points that need Anandu's Google account. Empty = disabled. */
object Config {
    /** Google Form link for anonymous feedback. Empty until Anandu creates the form. */
    const val FEEDBACK_FORM_URL =
        "https://docs.google.com/forms/d/e/1FAIpQLSdUieisN5IPUUZtumYNmEOGC_ZS3asbZN8AFt4CfNTg663T7A/viewform?usp=publish-editor"

    /** Apps Script endpoint for the anonymous install counter. Empty until set up. */
    const val COUNTER_URL =
        "https://script.google.com/macros/s/AKfycbyG_DIhBKKVHTIjDEZHB2NebCyLJBovHgfQjXizEvS4TlDe4mkqwP7FA-9XfGFxMyRH/exec"

    /** version.json for the in-app update check (GitHub raw). */
    const val UPDATE_CHECK_URL =
        "https://raw.githubusercontent.com/mightymonster/studyquiz/main/version.json"
}
