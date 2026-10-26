package com.anandu.studyquiz

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.anandu.studyquiz.Ui.dp
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.random.Random

class MainActivity : Activity() {

    private lateinit var profileBtn: TextView
    private var welcomePopupShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        Usage.sessionStarted()

        if (Guest.name(this) == null) {
            Ui.askGuestName(this) { build() }
        } else {
            build()
        }
        pingCounterOnce()
        UpdateChecker.check(this)
    }

    override fun onResume() {
        super.onResume()
        if (Guest.name(this) != null) {
            build()
            if (::profileBtn.isInitialized) Avatar.styleButton(this, profileBtn)
        }
    }

    /** Double-back to exit; the second press occasionally nudges for feedback. */
    override fun onBackPressed() {
        showExitConfirmation()
    }

    /** Every exit banks the session's usage time into today's total. */
    override fun finish() {
        Usage.bankSession(this)
        super.finish()
    }

    /** Back-press always asks first — the app never exits instantly. */
    private fun showExitConfirmation() {
        // Warm goodnight dialog for night-time exits (21:00–05:00 device time).
        if (isNightNow()) {
            showGoodnightAndExit()
            return
        }
        val dayDialog = AlertDialog.Builder(this)
            .setTitle(Lang.t("Exit StudyQuiz?", "StudyQuiz-ൽ നിന്ന് പുറത്തുകടക്കണോ?"))
            .setPositiveButton(Lang.t("Exit", "പുറത്തുകടക്കൂ")) { d, _ ->
                d.dismiss(); maybeNudgeAndExit()
            }
            .setNegativeButton(Lang.t("Stay", "തുടരൂ")) { d, _ -> d.dismiss() }
        usageLine()?.let { dayDialog.setMessage(it) }
        dayDialog.show()
    }

    private fun maybeNudgeAndExit() {
        val prefs = getSharedPreferences("studyquiz_prefs", MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val last = prefs.getString("nudge_date", "")
        if (last != today && Random.nextInt(3) == 0) {
            prefs.edit().putString("nudge_date", today).apply()
            AlertDialog.Builder(this)
                .setTitle(Lang.t("Enjoying StudyQuiz?", "StudyQuiz ഇഷ്ടപ്പെട്ടോ?"))
                .setMessage(
                    Lang.t(
                        "Have a topic request or an idea? Send anonymous feedback — it shapes what comes next.",
                        "ഏതെങ്കിലും ടോപ്പിക് വേണോ, എന്തെങ്കിലും idea ഉണ്ടോ? Anonymous feedback അയക്കൂ — അതാണ് അടുത്തത് എന്തെന്ന് തീരുമാനിക്കുന്നത്."
                    )
                )
                .setPositiveButton(Lang.t("💬 Send feedback", "💬 ഫീഡ്ബാക്ക്")) { d, _ ->
                    d.dismiss(); Ui.openFeedback(this); finish()
                }
                .setNegativeButton(Lang.t("Not now", "ഇപ്പോൾ വേണ്ട")) { d, _ ->
                    d.dismiss(); finish()
                }
                .setOnCancelListener { finish() }
                .show()
        } else {
            finish()
        }
    }

    /** Night window for the goodnight exit dialog: 21:00–05:00 device time. */
    private fun isNightNow(): Boolean {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return h >= 21 || h < 5
    }

    /** "⏱ This session: 25 min • Today: 1h 10m" — null when untracked. */
    private fun usageLine(): String? {
        if (Usage.sessionStartMs == 0L) return null
        val s = Lang.t("This session", "ഈ സെഷൻ")
        val t = Lang.t("Today", "ഇന്ന്")
        return "⏱ $s: ${CompletionScreen.formatDuration(Usage.sessionElapsedMs())} • " +
            "$t: ${CompletionScreen.formatDuration(Usage.todayTotalMs(this))}"
    }

    /** Warm exit dialog shown instead of the plain exit during night hours. */
    private fun showGoodnightAndExit() {
        val name = Guest.name(this)
        val warm = Lang.t(
            "Sleep well — see you tomorrow!",
            "നന്നായി ഉറങ്ങൂ — നാളെ കാണാം!"
        )
        val usage = usageLine()
        AlertDialog.Builder(this)
            .setTitle(
                if (name != null) Lang.t("Good night, $name! 🌙", "ശുഭരാത്രി, $name! 🌙")
                else Lang.t("Good night! 🌙", "ശുഭരാത്രി! 🌙")
            )
            .setMessage(if (usage != null) "$warm\n$usage" else warm)
            .setPositiveButton(Lang.t("Exit", "പുറത്തുകടക്കൂ")) { d, _ ->
                d.dismiss(); finish()
            }
            .setNegativeButton(Lang.t("Stay", "തുടരൂ")) { d, _ -> d.dismiss() }
            .show()
    }

    private fun build() {
        val (root, col) = Ui.screenWithNav(this, "home")
        profileBtn = banner(col)

        // ---- Upcoming exams (no section header; the two cards speak for themselves) ----
        col.addView(examsSection())

        // ---- Practice modes ----
        col.addView(practiceModes())

        // ---- Study timetable ----
        col.addView(timetableSection())

        // ---- Topics ----
        col.addView(Ui.sectionHeader(this, "📖", Lang.t("Topics", "ടോപ്പിക്കുകൾ")))
        val topics = Store.data(this).topics
        if (topics.isEmpty()) {
            col.addView(TextView(this).apply {
                text = Lang.t("Content coming soon.", "ഉള്ളടക്കം ഉടൻ വരും.")
                setTextColor(Theme.ink(this@MainActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            })
            col.addView(TextView(this).apply {
                text = Lang.t(
                    "New notes & questions will appear here after the next update.",
                    "അടുത്ത അപ്ഡേറ്റിന് ശേഷം പുതിയ നോട്ട്സും ചോദ്യങ്ങളും ഇവിടെ വരും."
                )
                setTextColor(Theme.grey(this@MainActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
            })
        } else {
            topics.forEach { t -> col.addView(topicCard(t)) }
        }
        col.addView(quizAllButton())

        setContentView(root)
        maybeShowWelcome()
    }

    /** Single seamless banner: generated banner art with the brand header overlaid on top. */
    private fun banner(col: LinearLayout): TextView {
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                // Bleed to the screen edges (the col has 12dp side / 14dp top padding).
                leftMargin = dp(-12); rightMargin = dp(-12); topMargin = dp(-14)
            }
            // Header illustration backdrop (dark #0D2925 per v1.37 spec).
            if (Theme.isDark(this@MainActivity)) setBackgroundColor(Color.parseColor("#0D2925"))
            // v1.39: round the BOTTOM corners (28dp) so the banner has no sharp
            // rectangle cut; top stays square (banner meets the status bar area).
            val cornerR = dp(28).toFloat()
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val r = cornerR
                    val w = view.width.toFloat()
                    val h = view.height.toFloat()
                    if (w <= 0f || h <= 0f) return
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, h - r)
                        quadTo(w, h, w - r, h)
                        lineTo(r, h)
                        quadTo(0f, h, 0f, h - r)
                        close()
                    }
                    if (path.isConvex) outline.setConvexPath(path)
                }
            }
            clipToOutline = true
            // Re-evaluate the outline once laid out (width/height known then).
            post { invalidateOutline() }
        }
        frame.addView(ImageView(this).apply {
            // v1.44 root-cause fix: app theme is manual (SharedPreferences), NOT the
            // system uiMode — so -night qualifiers resolve wrong. Pick explicitly.
            setImageResource(if (Theme.isDark(this@MainActivity)) R.drawable.banner_home_dark else R.drawable.banner_home)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = null
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })
        // Bottom fade: melts the banner art into the screen background so the banner
        // has no hard bottom edge. Bottom-only, so it never touches the header row.
        // v1.39: taller (88dp) + 3-stop eased gradient for a gradual melt.
        frame.addView(View(this).apply {
            val bg = Theme.bg(this@MainActivity)
            val mid = (bg and 0x00FFFFFF) or (0x73 shl 24) // 45% screen-bg
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.TRANSPARENT, mid, bg)
            )
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(88), Gravity.BOTTOM
            )
        })
        // v1.40: side fades — melt the banner's left/right edges into the screen
        // background so no rectangle outline shows. Added above the art but below
        // the header content row (title/bell/avatar stay untouched).
        val bgc = Theme.bg(this@MainActivity)
        frame.addView(View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(bgc, Color.TRANSPARENT)
            )
            layoutParams = FrameLayout.LayoutParams(
                dp(28), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START
            )
        })
        frame.addView(View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, bgc)
            )
            layoutParams = FrameLayout.LayoutParams(
                dp(28), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END
            )
        })
        val (row, btn) = headerContent()
        row.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP
        )
        frame.addView(row)
        col.addView(frame)
        return btn
    }

    /** Brand header row: cap + two-tone "StudyQuiz" (one line) + tagline + bell/avatar. No ? icon. */
    private fun headerContent(): Pair<LinearLayout, TextView> {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // Same visual padding the row used to get from the content column; the row itself
            // stays transparent so the banner art shows through behind it.
            setPadding(dp(12), dp(14), dp(12), dp(4))
        }
        row.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_grad_cap)
            contentDescription = Lang.t("StudyQuiz", "StudyQuiz")
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).apply { rightMargin = dp(10) }
        })
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = styledBrand()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 27f)
            setTypeface(typeface, Typeface.BOLD)
            isSingleLine = true
        })
        titleCol.addView(TextView(this).apply {
            text = Lang.t(
                "Read notes, quiz yourself, track growth.",
                "നോട്ട്സ് വായിക്കൂ, ക്വിസ് ചെയ്യൂ, ഗ്രോത്ത് ട്രാക്ക് ചെയ്യൂ."
            )
            setTextColor(Theme.grey(this@MainActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })
        row.addView(titleCol)
        val (actions, btn) = Ui.actionButtons(
            this, withHelp = false, withGear = false,
            onProfile = {
                startActivity(
                    Intent(this, ProfileActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
                overridePendingTransition(R.anim.book_open_in, R.anim.book_open_out)
            }
        )
        // Icons top-align with the title, like the reference banner.
        // (v1.29 crash fix: assign fresh LayoutParams — never cast a possibly-null layoutParams.)
        actions.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.TOP }
        row.addView(actions)
        return row to btn
    }

    private fun styledBrand(): SpannableString {
        // Exact banner shades in light mode; theme-adaptive in dark mode.
        val dark = Theme.isDark(this)
        val study = if (dark) Theme.ink(this) else Color.parseColor("#1A2E4A")
        val quiz = if (dark) Theme.green(this) else Color.parseColor("#2E9E63")
        val s = SpannableString("StudyQuiz")
        s.setSpan(
            ForegroundColorSpan(study), 0, 5,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        s.setSpan(
            ForegroundColorSpan(quiz), 5, 9,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return s
    }

    /** Exams section: Upcoming Exams + PYQ cards, directly below the banner. */
    private fun examsSection(): LinearLayout {
        val cards = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        cards.addView(examsCard())
        cards.addView(pyqCard())
        return cards
    }

    /** Built-in upcoming exams (read-only): summary card -> full list.
     * Short card, plain "Upcoming Exams" title (no count). */
    private fun examsCard(): LinearLayout {
        val card = Ui.card(this)
        card.setPadding(dp(14), dp(10), dp(14), dp(10))
        card.isClickable = true
        card.isFocusable = true
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(Ui.iconChip(this, "📋", Theme.mint(this)))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(12)
            }
        }
        col.addView(Ui.wordWrap(TextView(this).apply {
            text = Lang.t("Upcoming Exams", "വരാനിരിക്കുന്ന പരീക്ഷകൾ")
            setTextColor(Theme.ink(this@MainActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        }))
        col.addView(Ui.wordWrap(TextView(this).apply {
            text = Lang.t(
                "Tap to see notification details & PDFs.",
                "നോട്ടിഫിക്കേഷൻ വിശദാംശങ്ങളും PDF-ഉം കാണാൻ അമർത്തൂ."
            )
            setTextColor(Theme.grey(this@MainActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        }))
        row.addView(col)
        row.addView(Ui.chevron(this))
        card.addView(row)
        card.setOnClickListener {
            startActivity(Intent(this, UpcomingExamsActivity::class.java))
        }
        return card
    }

    /** PYQ papers (read-only): summary card -> papers list. Plain title, no count. */
    private fun pyqCard(): LinearLayout {
        val card = Ui.card(this)
        card.isClickable = true
        card.isFocusable = true
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(Ui.iconChip(this, "📝", Theme.lightBlue(this)))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(12)
            }
        }
        col.addView(Ui.wordWrap(TextView(this).apply {
            text = Lang.t("PYQ Papers", "PYQ പേപ്പറുകൾ")
            setTextColor(Theme.ink(this@MainActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
        }))
        col.addView(Ui.wordWrap(TextView(this).apply {
            text = Lang.t(
                "Previous year question papers, exam style.",
                "മുൻവർഷ ചോദ്യപേപ്പറുകൾ, പരീക്ഷാ രീതിയിൽ."
            )
            setTextColor(Theme.grey(this@MainActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        }))
        row.addView(col)
        row.addView(Ui.chevron(this))
        card.addView(row)
        card.setOnClickListener {
            startActivity(Intent(this, PyqPapersActivity::class.java))
        }
        return card
    }

    /** Practice modes on a soft mint panel: gradient Mock Test CTA + duo cards. */
    private fun practiceModes(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // v1.40: container box removed (user feedback: the pale box created an
            // unwanted separation). Cards sit directly on the screen background so
            // the banner color flows smoothly down behind them.
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }
        box.addView(Ui.sectionHeader(this, "📊", Lang.t("Practice modes", "പ്രാക്ടീസ് മോഡുകൾ")))
        box.addView(mockTestCard())
        val duo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        }
        val bmCard = practiceCard(
            bg = Theme.amberTint(this),
            chipBg = Theme.amber(this),
            emoji = "⭐",
            title = Lang.t("Bookmarks", "ബുക്ക്മാർക്കുകൾ"),
            sub = Lang.t("Save important questions", "പ്രധാന ചോദ്യങ്ങൾ സേവ് ചെയ്യൂ")
        ) { openBookmarks() }
        bmCard.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            rightMargin = dp(5)
        }
        val wrongCard = practiceCard(
            bg = Theme.redTint(this),
            chipBg = Theme.red(this),
            emoji = "✕",
            chipFg = Color.WHITE,
            title = Lang.t("Wrong only", "തെറ്റിയത് മാത്രം"),
            sub = Lang.t("Practice your mistakes", "തെറ്റുകൾ പരിശീലിക്കൂ")
        ) { openWrongOnly() }
        wrongCard.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            leftMargin = dp(5)
        }
        duo.addView(bmCard)
        duo.addView(wrongCard)
        box.addView(duo)
        return box
    }

    /** Study timetable entry card — shows today's slot progress. */
    private fun timetableSection(): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        }
        box.addView(Ui.sectionHeader(this, "🕖", Lang.t("Study timetable", "സ്റ്റഡി ടൈംടേബിൾ")))
        val slots = TimetableStore.getSlots(this)
        val sub = if (slots.isEmpty()) {
            Lang.t("Set subjects, time & alerts", "Subjects, സമയം & അലേർട്ടുകൾ സെറ്റ് ചെയ്യൂ")
        } else {
            val done = TimetableStore.doneIds(this)
            val n = slots.count { done.contains(it.id) }
            Lang.t(
                "$n of ${slots.size} slots done today",
                "ഇന്ന് ${slots.size}-ൽ $n slots കഴിഞ്ഞു"
            )
        }
        val card = practiceCard(
            bg = Theme.mint(this),
            chipBg = Theme.green(this),
            chipFg = Color.WHITE,
            emoji = "🕖",
            title = Lang.t("My Timetable", "എന്റെ ടൈംടേബിൾ"),
            sub = sub
        ) {
            startActivity(
                Intent(
                    this,
                    if (TimetableStore.hasTimetable(this)) TimetableActivity::class.java
                    else TimetableSetupActivity::class.java
                )
            )
        }
        box.addView(card)
        return box
    }

    /** Large Mock Test CTA — solid #008F7A in dark mode (v1.37 spec), green gradient in light. */
    private fun mockTestCard(): FrameLayout {
        val card = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                if (Theme.isDark(this@MainActivity)) {
                    setColor(Color.parseColor("#008F7A"))
                } else {
                    orientation = GradientDrawable.Orientation.TL_BR
                    colors = intArrayOf(Color.parseColor("#008F7A"), Color.parseColor("#00695C"))
                }
                cornerRadius = dp(20).toFloat()
            }
            // v1.44: flat card — no drop shadow.
            elevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            isClickable = true
            isFocusable = true
        }
        card.addView(TextView(this).apply {
            text = "⏱️"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 88f)
            alpha = 0.16f
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            ).apply { rightMargin = dp(52) }
        })
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        row.addView(Ui.iconChip(this, "📝", Color.argb(48, 255, 255, 255), boxDp = 58, emojiSp = 28f))
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(14)
            }
        }
        col.addView(TextView(this).apply {
            text = Lang.t("Mock Test", "മോക്ക് ടെസ്റ്റ്")
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = Lang.t("100 Questions · 90 Minutes", "100 ചോദ്യങ്ങൾ · 90 മിനിറ്റ്")
            setTextColor(Color.argb(217, 255, 255, 255))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
        })
        row.addView(col)
        row.addView(Ui.chevron(this, bg = Color.parseColor("#00695C"), fg = Color.WHITE, circleDp = 40))
        card.addView(row)
        card.setOnClickListener { openMockTest() }
        return card
    }

    /**
     * Compact half-page tinted practice card (bookmarks / wrong-only).
     * Internal layout: row[icon chip -> title (one line) -> chevron], then
     * small description below. No count number. Compact, word-level
     * wrapping only, minimum width enforced.
     */
    private fun practiceCard(
        bg: Int,
        chipBg: Int,
        emoji: String,
        chipFg: Int? = null,
        title: String,
        sub: String,
        onClick: () -> Unit
    ): LinearLayout {
        val card = Ui.tintedCard(this, bg)
        card.setPadding(dp(10), dp(10), dp(10), dp(10))
        card.minimumWidth = dp(140)
        card.isClickable = true
        card.isFocusable = true
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(Ui.iconChip(this, emoji, chipBg, boxDp = 32, emojiSp = 16f, fg = chipFg))
        row.addView(TextView(this).apply {
            text = title
            setTextColor(Theme.ink(this@MainActivity))
            // Slightly smaller so the title fits on one line (esp. Malayalam).
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (Lang.ml) 11.5f else 13f)
            setTypeface(typeface, Typeface.BOLD)
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ).apply { leftMargin = dp(8) }
        })
        row.addView(Ui.chevron(this, circleDp = 28))
        card.addView(row)
        card.addView(Ui.wordWrap(TextView(this).apply {
            text = sub
            setTextColor(Theme.grey(this@MainActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }))
        card.setOnClickListener { onClick() }
        return card
    }

    /** Outlined "Quiz: All Topics" CTA with chevron. */
    private fun quizAllButton(): LinearLayout {
        val btn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Theme.card(this@MainActivity))
                setStroke(dp(2), Theme.green(this@MainActivity))
            }
            setPadding(dp(18), dp(15), dp(12), dp(15))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(14) }
            isClickable = true
            isFocusable = true
        }
        btn.addView(TextView(this).apply {
            text = Lang.t("🎯 Quiz: All Topics", "🎯 ക്വിസ്: എല്ലാ ടോപ്പിക്കുകളും")
            setTextColor(Theme.green(this@MainActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        btn.addView(Ui.chevron(this, circleDp = 32))
        btn.setOnClickListener {
            val n = Store.data(this).questions.size
            if (n == 0) {
                AlertDialog.Builder(this)
                    .setMessage(Lang.t("No questions yet — content coming soon.", "ചോദ്യങ്ങളില്ല — ഉള്ളടക്കം ഉടൻ വരും."))
                    .setPositiveButton(Lang.t("OK", "ശരി"), null)
                    .show()
            } else {
                askCountAndStart(Store.ALL)
            }
        }
        return btn
    }

    private fun openMockTest() {
        val n = Store.data(this).questions.size
        if (n == 0) {
            AlertDialog.Builder(this)
                .setMessage(Lang.t("No questions yet — content coming soon.", "ചോദ്യങ്ങളില്ല — ഉള്ളടക്കം ഉടൻ വരും."))
                .setPositiveButton(Lang.t("OK", "ശരി"), null)
                .show()
        } else {
            startActivity(Intent(this, MockTestActivity::class.java))
        }
    }

    private fun openBookmarks() {
        if (Store.bookmarkCount(this) == 0) {
            Toast.makeText(
                this,
                Lang.t(
                    "No bookmarks yet — tap ☆ on a question to star it.",
                    "ബുക്ക്മാർക്കുകളില്ല — ചോദ്യത്തിലെ ☆ അമർത്തൂ."
                ),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            startActivity(Intent(this, BookmarksActivity::class.java))
        }
    }

    private fun openWrongOnly() {
        if (Store.wrongCount(this) == 0) {
            Toast.makeText(
                this,
                Lang.t("Nothing wrong yet — nice! 🎉", "ഇതുവരെ തെറ്റിയതൊന്നുമില്ല — കൊള്ളാം! 🎉"),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            startActivity(Intent(this, QuizActivity::class.java).putExtra("mode", "wrong"))
        }
    }

    /** First-install welcome popup: what the app is + how it works. Once only. */
    private fun maybeShowWelcome() {
        if (welcomePopupShown) return
        val prefs = getSharedPreferences("studyquiz_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("welcome_shown", false)) return
        welcomePopupShown = true
        AlertDialog.Builder(this)
            .setTitle("🎉 Welcome 🎉")
            .setMessage(
                "StudyQuiz is your exam-prep companion for Junior Clerk and Assistant Secretary exams.\n\nHow it works:\n• PYQ Papers → pick a year → pick a category number → answer one question at a time.\n• Move between questions with ◀ ▶; your score, time and full review appear at the end.\n• Practice quizzes, bookmarks and My Analysis on the Home tab track your growth.\n\nEverything works offline — your attempts stay on your device."
            )
            .setPositiveButton("Continue") { d, _ ->
                prefs.edit().putBoolean("welcome_shown", true).apply()
                d.dismiss()
            }
            .setCancelable(false)
            .show()
    }


    private fun topicCard(t: Topic): LinearLayout {
        val card = Ui.card(this)
        card.isClickable = true
        card.isFocusable = true

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = t.name
            setTextColor(Theme.ink(this@MainActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(title)
        val count = TextView(this).apply {
            val n = Store.questionCount(this@MainActivity, t.id)
            text = "$n ${Lang.t("Qs", "ചോദ്യം")}"
            setTextColor(Theme.grey(this@MainActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        }
        row.addView(count)
        card.addView(row)

        if (t.notes.isNotBlank()) {
            card.addView(TextView(this).apply {
                text = if (t.notes.length > 110) t.notes.take(110) + "…" else t.notes
                setTextColor(Theme.grey(this@MainActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(4) }
            })
        }

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val open = Ui.button(this, Lang.t("Open", "തുറക്കൂ")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(4); topMargin = dp(10)
            }
            setOnClickListener {
                startActivity(
                    Intent(this@MainActivity, TopicDetailActivity::class.java)
                        .putExtra("topicId", t.id)
                )
            }
        }
        val quiz = Ui.button(this, Lang.t("Quiz", "ക്വിസ്"), filled = false).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(4); topMargin = dp(10)
            }
            setOnClickListener { askCountAndStart(t.id) }
        }
        btnRow.addView(open); btnRow.addView(quiz)
        card.addView(btnRow)
        return card
    }

    fun askCountAndStart(topicId: String) {
        val total = if (topicId == Store.ALL) Store.data(this).questions.size
        else Store.questionCount(this, topicId)
        if (total == 0) {
            AlertDialog.Builder(this)
                .setMessage(Lang.t("No questions yet — content coming soon.", "ചോദ്യങ്ങളില്ല — ഉള്ളടക്കം ഉടൻ വരും."))
                .setPositiveButton(Lang.t("OK", "ശരി"), null)
                .show()
            return
        }
        val options = mutableListOf<String>()
        if (total >= 10) options.add("10")
        if (total >= 20) options.add("20")
        options.add(Lang.t("All (%d)", "എല്ലാം (%d)").format(total))
        AlertDialog.Builder(this)
            .setTitle(Lang.t("How many questions?", "എത്ര ചോദ്യങ്ങൾ?"))
            .setItems(options.toTypedArray()) { d, which ->
                val count = when {
                    which < options.size - 1 -> options[which].toInt()
                    else -> total
                }
                d.dismiss()
                startActivity(
                    Intent(this, QuizActivity::class.java)
                        .putExtra("topicId", topicId)
                        .putExtra("count", count)
                )
            }
            .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
            .show()
    }

    /**
     * Anonymous install counter: one ping on first launch, only if Anandu
     * has wired a counter endpoint (Config.COUNTER_URL). No names, just +1.
     */
    private fun pingCounterOnce() {
        val url = Config.COUNTER_URL
        if (url.isBlank()) return
        // ?src=app lets the Apps Script count only real app installs (browser visits just read).
        val pingUrl = url + (if (url.contains("?")) "&src=app" else "?src=app")
        val prefs = getSharedPreferences("studyquiz_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("counter_pinged", false)) return
        thread {
            try {
                (URL(pingUrl).openConnection() as java.net.HttpURLConnection).run {
                    connectTimeout = 8000; readTimeout = 8000
                    requestMethod = "GET"; connect()
                    responseCode
                    disconnect()
                }
                prefs.edit().putBoolean("counter_pinged", true).apply()
            } catch (_: Exception) { /* silent — never break the app */ }
        }
    }
}
