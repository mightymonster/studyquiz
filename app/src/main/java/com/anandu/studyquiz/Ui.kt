package com.anandu.studyquiz

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Layout
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.anandu.studyquiz.Ui.dp

/** Shared colors + small programmatic-UI builders. All theme-aware via [Theme]. */
object Ui {
    const val TEAL = "#008F7A"
    const val TEAL_DARK = "#00695C"

    fun Context.dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    /** Returns (scrollView, innerColumn). Call setContentView(scroll). */
    fun screen(ctx: Context): Pair<ScrollView, LinearLayout> {
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(12), ctx.dp(14), ctx.dp(12), ctx.dp(28))
        }
        val scroll = ScrollView(ctx).apply {
            // Decorative theme background (v1.33) sits full-screen behind the scrollable content.
            background = DecorativeBackgroundDrawable(ctx)
            addView(col)
        }
        return scroll to col
    }

    /**
     * Returns (rootView, innerColumn): scrollable content + a 2-tab bottom
     * navigation bar (Home | Profile). Active tab icon+text in teal,
     * inactive in gray. Tab taps use a quick fade; the header avatar /
     * back-arrow transitions keep the book page-turn animation.
     */
    fun screenWithNav(act: Activity, selected: String): Pair<LinearLayout, LinearLayout> {
        val (scroll, col) = screen(act)
        val root = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.bg(act))
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            addView(bottomBar(act, selected))
        }
        return root to col
    }

    /** Bottom nav bar: top divider + two outline-icon tabs. */
    private fun bottomBar(act: Activity, selected: String): LinearLayout {
        val bar = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.navBg(act))
        }
        bar.addView(View(act).apply {
            setBackgroundColor(Theme.divider(act))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, act.dp(1)
            )
        })
        val tabs = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, act.dp(60)
            )
        }
        val homeActive = selected == "home"
        tabs.addView(navTab(act, Lang.t("Home", "ഹോം"), R.drawable.ic_nav_home, homeActive) {
            if (!homeActive) goTab(act, MainActivity::class.java)
        })
        tabs.addView(
            navTab(act, Lang.t("Profile", "പ്രൊഫൈൽ"), R.drawable.ic_nav_profile, !homeActive) {
                if (homeActive) goTab(act, ProfileActivity::class.java)
            }
        )
        bar.addView(tabs)
        return bar
    }

    /** One bottom-nav tab: outline icon + label, teal when active. */
    private fun navTab(
        act: Activity,
        label: String,
        iconRes: Int,
        active: Boolean,
        onTap: () -> Unit
    ): LinearLayout = LinearLayout(act).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f
        )
        setPadding(0, act.dp(8), 0, act.dp(8))
        val tint = if (active) Theme.green(act) else Theme.navInactive(act)
        addView(ImageView(act).apply {
            val d = act.getDrawable(iconRes)?.mutate()
            d?.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
            setImageDrawable(d)
            val s = act.dp(24)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { bottomMargin = act.dp(2) }
        })
        addView(TextView(act).apply {
            text = label
            setTextColor(tint)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        isClickable = true
        isFocusable = true
        setOnClickListener { onTap() }
    }

    /** Switch tabs: reuse the existing instance, quick fade. */
    private fun goTab(act: Activity, cls: Class<out Activity>) {
        act.startActivity(Intent(act, cls).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        act.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    /**
     * Word-level wrapping only — never break inside a word. Important for
     * Malayalam, where character-level breaks look broken. (API 23+; minSdk 24.)
     */
    fun wordWrap(tv: TextView): TextView {
        tv.breakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY
        tv.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
        return tv
    }

    /**
     * Malayalam-friendly body text: +2sp and 1.25x line spacing when the
     * Malayalam language is active, so text never looks crowded.
     * English sizing is unchanged.
     */
    fun styleBody(tv: TextView, sp: Float): TextView {
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (Lang.ml) sp + 2f else sp)
        if (Lang.ml) tv.setLineSpacing(0f, 1.25f)
        return tv
    }

    /** Thin progress bar: green fill on a mint track. Rebuild to update. */
    fun progressBar(ctx: Context, progress: Int, max: Int): LinearLayout {
        val track = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(3).toFloat()
                setColor(Theme.mint(ctx))
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ctx.dp(6)
            ).apply { topMargin = ctx.dp(8); bottomMargin = ctx.dp(4) }
        }
        val p = progress.coerceIn(0, max)
        track.addView(View(ctx).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(3).toFloat()
                setColor(Theme.green(ctx))
            }
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, p.toFloat()
            )
        })
        track.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT,
                (max - p).toFloat().coerceAtLeast(0.01f)
            )
        })
        return track
    }

    /** Option-card visual states: borders do the talking, never heavy fills. */
    enum class OptState { NORMAL, SELECTED, CORRECT, WRONG }

    /**
     * Quiz option as a separated card: mint letter badge + text (+ optional
     * asset image for PYQ figure options), state shown via border + subtle
     * tint only.
     */
    fun optionCard(
        ctx: Context,
        letter: String,
        text: String?,
        assetPath: String?,
        state: OptState,
        onClick: ((LinearLayout) -> Unit)? = null
    ): LinearLayout {
        val border = when (state) {
            OptState.NORMAL -> Theme.divider(ctx)
            OptState.SELECTED -> Theme.green(ctx)
            OptState.CORRECT -> Theme.correct(ctx)
            OptState.WRONG -> Theme.red(ctx)
        }
        val fill = when (state) {
            OptState.NORMAL -> Theme.card(ctx)
            OptState.SELECTED -> Theme.mint(ctx)
            OptState.CORRECT -> Theme.correctTint(ctx)
            OptState.WRONG -> Theme.redTint(ctx)
        }
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(14).toFloat()
                setColor(fill)
                setStroke(ctx.dp(if (state == OptState.NORMAL) 1 else 2), border)
            }
            setPadding(ctx.dp(12), ctx.dp(12), ctx.dp(12), ctx.dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(8) }
            if (onClick != null) {
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick(this) }
            }
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(ctx).apply {
            this.text = letter
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(
                when (state) {
                    OptState.CORRECT -> Theme.correct(ctx)
                    OptState.WRONG -> Theme.red(ctx)
                    else -> Theme.green(ctx)
                }
            )
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(
                    when (state) {
                        OptState.CORRECT -> Theme.correctTint(ctx)
                        OptState.WRONG -> Theme.redTint(ctx)
                        else -> Theme.mint(ctx)
                    }
                )
            }
            val s = ctx.dp(38)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { rightMargin = ctx.dp(12) }
        })
        if (text != null) {
            row.addView(styleBody(TextView(ctx).apply {
                this.text = text
                setTextColor(Theme.ink(ctx))
                if (state == OptState.CORRECT || state == OptState.WRONG) {
                    setTypeface(typeface, Typeface.BOLD)
                }
            }, 15f).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            })
        }
        if (state == OptState.CORRECT || state == OptState.WRONG) {
            row.addView(TextView(ctx).apply {
                this.text = if (state == OptState.CORRECT) "✅" else "❌"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { leftMargin = ctx.dp(8) }
            })
        }
        col.addView(row)
        if (assetPath != null) {
            assetImage(ctx, assetPath)?.let { col.addView(it) }
        }
        return col
    }

    /** Settings-style section label: small green icon + bold label. */
    fun sectionLabel(ctx: Context, parent: LinearLayout, icon: String, text: String) {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(18); bottomMargin = ctx.dp(2) }
        }
        row.addView(TextView(ctx).apply {
            this.text = icon
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Theme.green(ctx))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = ctx.dp(8) }
        })
        row.addView(TextView(ctx).apply {
            this.text = text
            setTextColor(Theme.ink(ctx))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        })
        parent.addView(row)
    }

    /** Small outlined pill button (e.g. "✎ Change name"). */
    fun smallOutlineButton(ctx: Context, text: String): Button =
        Button(ctx).apply {
            this.text = text
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Theme.green(ctx))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(20).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(ctx.dp(1), Theme.green(ctx))
            }
            setPadding(ctx.dp(14), ctx.dp(6), ctx.dp(14), ctx.dp(6))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(8) }
        }

    /** Static pill label (e.g. "All Time ▾", "Last 10 Quizzes ▾"). */
    fun staticPill(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(Theme.grey(ctx))
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = ctx.dp(14).toFloat()
            setColor(Theme.mint(ctx))
        }
        setPadding(ctx.dp(12), ctx.dp(6), ctx.dp(12), ctx.dp(6))
    }

    /**
     * A tappable settings "more" row: leading icon, title (+ right-side
     * value), chevron. [iconBg] tints the icon chip.
     */
    fun moreRow(
        ctx: Context,
        icon: String,
        title: String,
        value: String? = null,
        iconBg: Int = Theme.mint(ctx),
        onClick: () -> Unit
    ): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, ctx.dp(9), 0, ctx.dp(9))
        isClickable = true
        isFocusable = true
        addView(iconChip(ctx, icon, iconBg, boxDp = 40, emojiSp = 20f))
        val titleView = styleBody(TextView(ctx).apply {
            text = title
            setTextColor(Theme.ink(ctx))
        }, 15f)
        titleView.layoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply { leftMargin = ctx.dp(12) }
        addView(titleView)
        if (value != null) {
            addView(TextView(ctx).apply {
                text = value
                setTextColor(Theme.grey(ctx))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = ctx.dp(4) }
            })
        }
        addView(chevron(ctx))
        setOnClickListener { onClick() }
    }

    /**
     * Header row: title+subtitle on the left, "?" help and profile circle
     * buttons on the right. Returns the profile button so callers can
     * refresh its letter in onResume.
     */
    fun topBar(act: Activity, parent: LinearLayout, title: String, sub: String): TextView {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(act).apply {
            text = title
            setTextColor(Theme.ink(act))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(act).apply {
            text = sub
            setTextColor(Theme.grey(act))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = act.dp(2) }
        })
        row.addView(col)

        val (actions, profileBtn) = actionButtons(act)
        row.addView(actions)

        parent.addView(row)
        (row.layoutParams as? LinearLayout.LayoutParams)?.apply { bottomMargin = act.dp(4) }
        return profileBtn
    }

    /**
     * Notification bell (with unread badge), optional help "?" and settings
     * gear, plus profile avatar — the action cluster used in app headers.
     * Home shows bell + avatar only; Profile shows help + gear only;
     * other screens keep the full set.
     * [onProfile] lets a caller override the avatar tap (e.g. for a custom
     * transition). Returns the row and the profile button.
     */
    fun actionButtons(
        act: Activity,
        withHelp: Boolean = true,
        withGear: Boolean = true,
        withBell: Boolean = true,
        withAvatar: Boolean = true,
        onProfile: (() -> Unit)? = null
    ): Pair<LinearLayout, TextView> {
        val row = LinearLayout(act).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val unread = AppNotifications.unreadCount(act)
        if (withBell) {
            row.addView(bellButton(act, unread))
        }

        if (withHelp) {
            val helpBtn = circleButton(act, "?", filled = false)
            helpBtn.setOnClickListener { showHelp(act) }
            row.addView(helpBtn)
        }

        if (withGear) {
            val gearBtn = circleButton(act, "⚙", filled = false)
            gearBtn.setOnClickListener {
                act.startActivity(
                    Intent(act, SettingsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
            }
            row.addView(gearBtn)
        }

        val profileBtn = TextView(act)
        Avatar.styleButton(act, profileBtn)
        profileBtn.setOnClickListener {
            onProfile?.invoke() ?: act.startActivity(
                Intent(act, ProfileActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
        if (withAvatar) row.addView(profileBtn)
        return row to profileBtn
    }

    /** Gold bell in a pale, delicate circle — like the reference banner. Red dot iff [unread] > 0. */
    private fun bellButton(act: Activity, unread: Int): View {
        val wrap = FrameLayout(act).apply {
            val dark = Theme.isDark(act)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(if (dark) "#1E332C" else "#F4FAF7"))
                setStroke(act.dp(1), Color.parseColor(if (dark) "#2E4A43" else "#D6E7E1"))
            }
            layoutParams = LinearLayout.LayoutParams(act.dp(44), act.dp(44)).apply {
                leftMargin = act.dp(8)
            }
            isClickable = true
            isFocusable = true
            contentDescription = Lang.t("Notifications", "നോട്ടിഫിക്കേഷനുകൾ")
        }
        wrap.addView(ImageView(act).apply {
            setImageResource(R.drawable.ic_bell)
            val s = act.dp(27)
            layoutParams = FrameLayout.LayoutParams(s, s, Gravity.CENTER)
        })
        if (unread > 0) {
            wrap.addView(View(act).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#E53935"))
                }
                layoutParams = FrameLayout.LayoutParams(
                    act.dp(11), act.dp(11), Gravity.TOP or Gravity.END
                ).apply {
                    topMargin = act.dp(6)
                    rightMargin = act.dp(6)
                }
            })
        }
        wrap.setOnClickListener {
            act.startActivity(Intent(act, NotificationsActivity::class.java))
        }
        return wrap
    }

    private fun circleButton(act: Activity, text: String, filled: Boolean): TextView =
        TextView(act).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (filled) Color.WHITE else Theme.green(act))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (filled) setColor(Theme.green(act)) else {
                    setColor(Color.TRANSPARENT)
                    setStroke(act.dp(2), Theme.green(act))
                }
            }
            val s = act.dp(44)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { leftMargin = act.dp(8) }
            isClickable = true
            isFocusable = true
        }

    fun header(ctx: Context, parent: LinearLayout, title: String, sub: String) {
        parent.addView(TextView(ctx).apply {
            text = title
            setTextColor(Theme.ink(ctx))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        parent.addView(TextView(ctx).apply {
            text = sub
            setTextColor(Theme.grey(ctx))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(2); bottomMargin = ctx.dp(6) }
        })
    }

    fun section(ctx: Context, parent: LinearLayout, text: String) {
        parent.addView(TextView(ctx).apply {
            this.text = text.uppercase()
            setTextColor(Theme.teal())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(16); bottomMargin = ctx.dp(4) }
        })
    }

    /**
     * Premium section header: leading icon + dark caps title, optional green
     * action link on the right (e.g. "See All >").
     */
    fun sectionHeader(
        ctx: Context,
        icon: String,
        title: String,
        actionText: String? = null,
        onAction: (() -> Unit)? = null
    ): LinearLayout {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(18); bottomMargin = ctx.dp(2) }
        }
        row.addView(TextView(ctx).apply {
            text = "$icon  ${title.uppercase()}"
            setTextColor(Theme.ink(ctx))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        })
        if (actionText != null) {
            row.addView(TextView(ctx).apply {
                text = actionText
                setTextColor(Theme.green(ctx))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, Typeface.BOLD)
                isClickable = true
                isFocusable = true
                setOnClickListener { onAction?.invoke() }
            })
        }
        return row
    }

    /** Rounded-square tinted chip holding an emoji (e.g. 📋 on mint). */
    fun iconChip(
        ctx: Context,
        emoji: String,
        bg: Int,
        boxDp: Int = 54,
        emojiSp: Float = 26f,
        fg: Int? = null
    ): TextView = TextView(ctx).apply {
        text = emoji
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, emojiSp)
        if (fg != null) {
            setTextColor(fg)
            setTypeface(typeface, Typeface.BOLD)
        }
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = ctx.dp(14).toFloat()
            setColor(bg)
        }
        layoutParams = LinearLayout.LayoutParams(ctx.dp(boxDp), ctx.dp(boxDp))
    }

    /** Small "›" chevron in a soft circle, for tappable cards. */
    fun chevron(
        ctx: Context,
        bg: Int = Theme.arrowBg(ctx),
        fg: Int = Theme.ink(ctx),
        circleDp: Int = 36
    ): TextView = TextView(ctx).apply {
        text = "›"
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(fg)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(bg)
        }
        val s = ctx.dp(circleDp)
        layoutParams = LinearLayout.LayoutParams(s, s).apply { leftMargin = ctx.dp(10) }
    }

    /** A [card] recolored with a soft tint (e.g. amber/red tint cards). */
    fun tintedCard(ctx: Context, bg: Int): LinearLayout = card(ctx).apply {
        (background as? GradientDrawable)?.setColor(bg)
    }

    fun button(ctx: Context, text: String, filled: Boolean = true): Button =
        Button(ctx).apply {
            this.text = text
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (filled) Color.WHITE else Theme.green(ctx))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(16).toFloat()
                if (filled) setColor(Theme.green(ctx))
                else {
                    setColor(Color.TRANSPARENT)
                    setStroke(ctx.dp(2), Theme.green(ctx))
                }
            }
            if (filled) elevation = ctx.dp(2).toFloat()
            setPadding(ctx.dp(16), ctx.dp(12), ctx.dp(16), ctx.dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(8) }
        }

    /**
     * Hero CTA (e.g. Mock Test): subtle mint-green vertical gradient,
     * larger type, soft shadow — the single most prominent action.
     */
    fun cta(ctx: Context, text: String): Button =
        Button(ctx).apply {
            this.text = text
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Theme.green(ctx), Theme.tealDark())
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(18).toFloat()
            }
            elevation = ctx.dp(4).toFloat()
            setPadding(ctx.dp(16), ctx.dp(14), ctx.dp(16), ctx.dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(8) }
        }

    /**
     * Button-styled option row: "A." label + text, or an asset image when
     * [assetPath] is given (PYQ figure-options, e.g. Venn diagrams).
     * [filled] = selected state. Never crashes on a bad asset.
     */
    fun optionRow(
        ctx: Context, letter: String, text: String?, assetPath: String?,
        filled: Boolean, onClick: () -> Unit
    ): LinearLayout = optionCard(
        ctx, letter, text, assetPath,
        if (filled) OptState.SELECTED else OptState.NORMAL,
        onClick = { _ -> onClick() }
    )

    /** Small pill button used for segmented choices (theme, language...).
     * Selected = light green shade (mint fill, green text/border) — never a
     * dark filled block. */
    fun pill(ctx: Context, text: String, selected: Boolean): Button =
        Button(ctx).apply {
            this.text = text
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (selected) Theme.green(ctx) else Theme.grey(ctx))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(20).toFloat()
                if (selected) {
                    setColor(Theme.mint(ctx))
                    setStroke(ctx.dp(2), Theme.green(ctx))
                } else {
                    setColor(Color.TRANSPARENT)
                    setStroke(ctx.dp(1), Theme.divider(ctx))
                }
            }
            setPadding(ctx.dp(8), ctx.dp(8), ctx.dp(8), ctx.dp(8))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = ctx.dp(4); rightMargin = ctx.dp(4)
            }
        }

    fun card(ctx: Context): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = ctx.dp(16).toFloat()
            setColor(Theme.card(ctx))
        }
        // v1.44: flat cards — no drop shadow anywhere in the app.
        elevation = 0f
        setPadding(ctx.dp(14), ctx.dp(12), ctx.dp(14), ctx.dp(12))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dp(10) }
    }

    /**
     * ImageView loading a bitmap from assets (e.g. "pyq_img/q43.png"),
     * fit-width with capped height. Returns null when the asset can't
     * be decoded — callers should just skip the image then.
     */
    fun assetImage(ctx: Context, assetPath: String): android.widget.ImageView? {
        return try {
            val bmp = android.graphics.BitmapFactory.decodeStream(ctx.assets.open(assetPath))
                ?: return null
            android.widget.ImageView(ctx).apply {
                setImageBitmap(bmp)
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = ctx.dp(8)
                    bottomMargin = ctx.dp(4)
                }
                // Cap the height so tall scans don't push options off-screen.
                maxHeight = ctx.dp(300)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun field(ctx: Context, hint: String, singleLine: Boolean = true, minLines: Int = 1): EditText =
        EditText(ctx).apply {
            this.hint = hint
            setSingleLine(singleLine)
            if (!singleLine) {
                this.minLines = minLines
                gravity = Gravity.TOP
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(Theme.ink(ctx))
            setHintTextColor(Theme.grey(ctx))
            setPadding(ctx.dp(14), ctx.dp(10), ctx.dp(14), ctx.dp(10))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = ctx.dp(12).toFloat()
                setColor(Theme.fieldBg(ctx))
                setStroke(ctx.dp(1), Theme.divider(ctx))
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(8) }
        }

    fun label(ctx: Context, text: String): TextView = TextView(ctx).apply {
        this.text = text
        setTextColor(Theme.ink(ctx))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTypeface(typeface, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = ctx.dp(12); leftMargin = ctx.dp(4) }
    }

    fun emptyNote(ctx: Context, parent: LinearLayout, text: String) {
        parent.addView(TextView(ctx).apply {
            this.text = text
            setTextColor(Theme.grey(ctx))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(16); bottomMargin = ctx.dp(8) }
        })
    }

    // ---------- Bilingual help / about ----------

    private fun helpSectionTitle(ctx: Context, parent: LinearLayout, en: String, ml: String) {
        parent.addView(TextView(ctx).apply {
            text = Lang.t(en, ml)
            setTextColor(Theme.tealDark())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(14) }
        })
    }

    /** Adds a paragraph in both languages, Malayalam first. */
    private fun helpPara(ctx: Context, parent: LinearLayout, ml: String, en: String) {
        parent.addView(TextView(ctx).apply {
            text = ml
            setTextColor(Theme.ink(ctx))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(6) }
        })
        parent.addView(TextView(ctx).apply {
            text = en
            setTextColor(Theme.grey(ctx))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = ctx.dp(4) }
        })
    }

    /** The "?" dialog: aim, how-to-use, privacy + backup. Always bilingual. */
    fun showHelp(act: Activity) {
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(act.dp(20), act.dp(8), act.dp(20), act.dp(8))
        }

        helpSectionTitle(act, col, "Our aim", "നമ്മുടെ ഉദ്ദേശം")
        helpPara(
            act, col,
            "പഠനം രസകരവും ട്രാക്ക് ചെയ്യാവുന്നതുമാക്കുക എന്നതാണ് ഈ ആപ്പിന്റെ ഉദ്ദേശം. " +
                "തയ്യാറാക്കിയ നോട്ട്സും ചോദ്യങ്ങളും വായിച്ച് ക്വിസ് ചെയ്ത്, ഓരോ ദിവസവും നിന്റെ വളർച്ച കാണുക.",
            "This app exists to make studying fun and trackable. Read the curated notes " +
                "and questions, take quizzes, and watch your growth every single day."
        )

        helpSectionTitle(act, col, "How to use", "എങ്ങനെ ഉപയോഗിക്കാം")
        helpPara(
            act, col,
            "1) ഒരു ടോപ്പിക് തുറന്ന് നോട്ട്സ് വായിക്കൂ.\n" +
                "2) \"ക്വിസ് തുടങ്ങൂ\" അല്ലെങ്കിൽ \"മോക്ക് ടെസ്റ്റ്\" അമർത്തി സ്വയം പരീക്ഷിക്കൂ.\n" +
                "3) കടുപ്പമുള്ള ചോദ്യങ്ങൾ ⭐ ബുക്ക്മാർക്ക് ചെയ്യൂ; തെറ്റിയവ \"തെറ്റിയത് മാത്രം\" മോഡിൽ തിരുത്തൂ.\n" +
                "4) പ്രൊഫൈൽ ടാബിൽ സ്കോറും ഗ്രോത്തും കാണൂ. പുതിയ ഉള്ളടക്കം ആപ്പ് അപ്ഡേറ്റുകളിലൂടെ വരും.",
            "1) Open a topic and read the notes.\n" +
                "2) Tap \"Start Quiz\" or \"Mock Test\" to test yourself.\n" +
                "3) Star tough questions with ⭐; revise mistakes in \"Wrong only\" mode.\n" +
                "4) Check your scores and growth in the Profile tab. New content arrives with app updates."
        )

        helpSectionTitle(act, col, "Your data stays yours", "നിന്റെ ഡേറ്റ, നിന്റെ കൈയ്യിൽ")
        helpPara(
            act, col,
            "ഈ ആപ്പ് നിന്റെ ഒരു വിവരവും എവിടെയും സേവ് ചെയ്യുന്നില്ല — ഇത് ആരാണ് " +
                "ഉപയോഗിക്കുന്നതെന്ന് ഡെവലപ്പർ പോലും അറിയില്ല. നിന്റെ സ്കോറുകളും " +
                "ബുക്ക്മാർക്കുകളും എല്ലാം നിന്റെ ഫോണിൽ മാത്രമാണ് ഇരിക്കുന്നത്. " +
                "അതുകൊണ്ട് ആപ്പ് അൺഇൻസ്റ്റാൾ ചെയ്താൽ ഡേറ്റ പോകും — ഇടയ്ക്ക് ബാക്കപ്പ് എടുക്കൂ.\n\n" +
                "ഒരു കാര്യം മാത്രം: പുതിയ version ഉണ്ടോ എന്നറിയാൻ ആപ്പ് ഇടയ്ക്ക് " +
                "ഇന്റർനെറ്റിൽ check ചെയ്യും. ഇതിൽ നിന്റെ ഒരു വിവരവും അയക്കുന്നില്ല — " +
                "വെറും version check മാത്രം.",
            "This app doesn't save any of your details anywhere — not even the developer " +
                "knows who is using it. Your scores and bookmarks live only on " +
                "your phone. So if you uninstall the app, your data is gone — take a " +
                "backup once in a while.\n\n" +
                "One exception: the app occasionally checks the internet for a newer " +
                "version. It sends none of your data — just a version check."
        )

        helpSectionTitle(act, col, "Backup & restore", "ബാക്കപ്പ് & റീസ്റ്റോർ")
        helpPara(
            act, col,
            "ബാക്കപ്പ്: പ്രൊഫൈലിൽ \"ബാക്കപ്പ്\" അമർത്തൂ — കിട്ടുന്ന ഫയൽ WhatsApp-ൽ " +
                "സ്വന്തത്തിന് അയക്കൂ അല്ലെങ്കിൽ Drive-ൽ വെക്കൂ.\n" +
                "റീസ്റ്റോർ: ആപ്പ് വീണ്ടും ഇൻസ്റ്റാൾ ചെയ്ത ശേഷം \"റീസ്റ്റോർ\" അമർത്തി " +
                "ആ ഫയൽ തിരഞ്ഞെടുക്കൂ — എല്ലാം തിരിച്ചുകിട്ടും.",
            "Backup: in your profile tap \"Backup\" — send the file to yourself on " +
                "WhatsApp or keep it on Drive.\n" +
                "Restore: after reinstalling, tap \"Restore\" and pick that file — " +
                "everything comes back."
        )

        val scroll = ScrollView(act).apply { addView(col) }
        AlertDialog.Builder(act)
            .setTitle(Lang.t("Help & about", "സഹായം"))
            .setView(scroll)
            .setPositiveButton(Lang.t("Close", "അടയ്ക്കൂ"), null)
            .show()
    }

    /** First-launch dialog asking for the guest name + avatar. */
    fun askGuestName(act: Activity, onDone: () -> Unit) {
        val input = EditText(act).apply {
            hint = Lang.t("Your name", "നിന്റെ പേര്")
            setSingleLine(true)
            setTextColor(Theme.ink(act))
            setHintTextColor(Theme.grey(act))
            setPadding(act.dp(16), act.dp(12), act.dp(16), act.dp(12))
        }
        val (grid, getChosen) = Avatar.pickerGrid(act, 0)
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(act.dp(20), act.dp(12), act.dp(20), act.dp(4))
            addView(input)
            addView(TextView(act).apply {
                text = Lang.t("Pick your avatar", "അവതാർ തിരഞ്ഞെടുക്കൂ")
                setTextColor(Theme.ink(act))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = act.dp(12); bottomMargin = act.dp(4) }
            })
            addView(grid)
        }
        AlertDialog.Builder(act)
            .setTitle(Lang.t("Welcome to StudyQuiz!", "StudyQuiz-ലേക്ക് സ്വാഗതം!"))
            .setMessage(Lang.t("What should we call you?", "നിന്നെ എന്ത് വിളിക്കണം?"))
            .setView(col)
            .setCancelable(false)
            .setPositiveButton(Lang.t("Let's go", "തുടങ്ങാം")) { d, _ ->
                val n = input.text.toString().trim()
                if (n.isNotEmpty()) Guest.setName(act, n)
                Avatar.setIndex(act, getChosen())
                d.dismiss()
                onDone()
            }
            .show()
    }

    /** Shared anonymous-feedback action (button in Settings + exit nudge). */
    fun openFeedback(act: Activity) {
        if (Config.FEEDBACK_FORM_URL.isBlank()) {
            Toast.makeText(
                act,
                Lang.t("Feedback form coming soon!", "ഫീഡ്ബാക്ക് ഫോം ഉടൻ വരും!"),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Config.FEEDBACK_FORM_URL)))
        }
    }
}
