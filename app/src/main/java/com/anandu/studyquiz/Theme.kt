package com.anandu.studyquiz

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

/**
 * Light / Dark / System theme. All UI colors are computed here so the
 * whole app (including dialogs, via the activity base theme) follows it.
 *
 * Locked color system (v1.37 — user's EXACT dark-theme spec, recolor only):
 *  Light — Primary #008F7A, Dark Green #00695C, Background #F8FBFA,
 *           Surface #FFFFFF, Mint #E8F7F3, MintDark #D4F0E8,
 *           Text #102A43, Secondary #627D98,
 *           Correct #10B981, Wrong #FF6B6B, Bookmark #FFC928.
 *  Dark — Background #071716, Decorative waves #0B2421, Header backdrop #0D2925,
 *           Card #102522, Elevated card #122C28, Card border #1D4540,
 *           Primary #00A88F, Dark green #00695C, Mock Test card #008F7A,
 *           Text #F1F5F4, Secondary #A9C2BD, Muted #78958F,
 *           Arrow/icon circle bg #153A35,
 *           Bookmark card #4A3708 + accent #FFC928,
 *           Wrong card #421D20 + accent #FF6B6B,
 *           Bottom nav bg #0B201E, nav active #00A88F, nav inactive #8FA9A5.
 */
object Theme {
    const val LIGHT = "light"
    const val DARK = "dark"
    const val SYSTEM = "system"

    private const val PREF = "studyquiz_prefs"
    private const val KEY = "theme_mode"

    var mode: String = SYSTEM
        private set

    fun init(ctx: Context) {
        mode = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, SYSTEM) ?: SYSTEM
    }

    fun setMode(ctx: Context, newMode: String) {
        mode = newMode
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, newMode).apply()
    }

    fun isDark(ctx: Context): Boolean = when (mode) {
        DARK -> true
        LIGHT -> false
        else -> (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    /** Must be called before super.onCreate() so dialogs follow the theme. */
    fun applyTo(act: Activity) {
        init(act)
        act.setTheme(
            if (isDark(act)) android.R.style.Theme_Material_NoActionBar
            else android.R.style.Theme_Material_Light_NoActionBar
        )
    }

    fun bg(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#071716" else "#F8FBFA")
    fun card(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#102522" else "#FFFFFF")
    fun ink(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#F1F5F4" else "#102A43")
    fun grey(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#A9C2BD" else "#627D98")
    /** Muted tertiary text — dark #78958F. */
    fun muted(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#78958F" else "#8AA0AA")
    /** Subtle card border — dark #1D4540, no heavy shadows. */
    fun divider(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#1D4540" else "#E4ECEA")
    fun fieldBg(ctx: Context): Int = card(ctx)

    /** Elevated card surface (dark): #122C28 — light mode uses the normal card color. */
    fun elevatedCard(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#122C28" else "#FFFFFF")

    /** Arrow / chevron circle background — dark #153A35. */
    fun arrowBg(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#153A35" else "#E8F7F3")

    /** Bottom navigation bar background — dark #0B201E. */
    fun navBg(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#0B201E" else "#FFFFFF")

    /** Bottom nav inactive tab — dark #8FA9A5. */
    fun navInactive(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#8FA9A5" else "#627D98")

    /** Soft tinted background for highlighted cards (e.g. result card). */
    fun tintBg(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#123B35" else "#E8F7F3")

    /** Light mint surface — info cards, streak chips, supporting panels. */
    fun mint(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#123B35" else "#E8F7F3")

    /** Deeper mint accent — light #D4F0E8 / dark #16483F (mint glow). */
    fun mintDark(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#16483F" else "#D4F0E8")

    /** Secondary light blue — supporting accents only. */
    fun lightBlue(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#132B36" else "#E1F3F8")

    /** Legacy constant brand teal (light-mode value). Prefer [green] for theme-aware UI. */
    fun teal(): Int = Color.parseColor(Ui.TEAL)
    fun tealDark(): Int = Color.parseColor(Ui.TEAL_DARK)

    /** Theme-aware primary: #008F7A light / #00A88F dark. */
    fun green(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#00A88F" else "#008F7A")
    /** Wrong accent — #FF6B6B in both modes (v1.37 spec). */
    fun red(ctx: Context): Int = Color.parseColor("#FF6B6B")
    fun amber(ctx: Context): Int = Color.parseColor("#FFC928")

    /** Correct-answer green: #10B981 light / #34D399 dark. */
    fun correct(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#34D399" else "#10B981")

    /** Subtle correct tint — borders do the talking, never heavy fills. */
    fun correctTint(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#123B35" else "#E9FAF4")

    /** Bookmark card background — light cream / dark warm amber #4A3708. */
    fun amberTint(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#4A3708" else "#FFF6DE")

    /** Wrong-answer card background — light pink / dark muted red #421D20. */
    fun redTint(ctx: Context): Int = Color.parseColor(if (isDark(ctx)) "#421D20" else "#FFE9E9")
}
