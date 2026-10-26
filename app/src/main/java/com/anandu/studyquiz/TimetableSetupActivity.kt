package com.anandu.studyquiz

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.anandu.studyquiz.Ui.dp

/**
 * 2-step timetable setup (v1.47):
 *  Step 1 — tick the subjects to study,
 *  Step 2 — time range per subject: tap "from" → clock, then "to" clock;
 *           total time is shown once both are set. Window kept simple.
 * Pre-filled with the existing timetable, or the default 7:30 AM – 11 PM plan.
 * Overnight ranges (e.g. 11 PM → 1 AM) are supported for night studiers.
 */
class TimetableSetupActivity : Activity() {

    private var step = 1
    private val selected = mutableListOf<String>()       // subject display names, in order
    private val fromMins = mutableMapOf<String, Int>()   // subject -> minutes since midnight
    private val toMins = mutableMapOf<String, Int>()     // subject -> minutes since midnight
    private var pendingAfterPermission: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        prefill()
        build()
    }

    private fun prefill() {
        val existing = TimetableStore.getSlots(this)
        if (existing.isNotEmpty()) {
            existing.forEach { s ->
                val name = TimetableStore.displayName(s.subject)
                selected.add(name)
                fromMins[name] = s.startMin % 1440
                toMins[name] = s.endMin % 1440
            }
        } else {
            TimetableStore.defaultSlots().forEach { s ->
                val name = TimetableStore.displayName(s.subject)
                selected.add(name)
                fromMins[name] = s.startMin % 1440
                toMins[name] = s.endMin % 1440
            }
        }
    }

    /** Duration in minutes; overnight ranges wrap past midnight. 0 = invalid. */
    private fun durOf(name: String): Int {
        val f = fromMins[name] ?: return 0
        val t = toMins[name] ?: return 0
        return (t - f + 1440) % 1440
    }

    private fun build() {
        val (scroll, col) = Ui.screen(this)

        col.addView(TextView(this).apply {
            text = Lang.t("🕖 Study Timetable", "🕖 സ്റ്റഡി ടൈംടേബിൾ")
            setTextColor(Theme.ink(this@TimetableSetupActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = Lang.t("Step $step of 2", "സ്റ്റെപ്പ് $step / 2")
            setTextColor(Theme.green(this@TimetableSetupActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2); bottomMargin = dp(8) }
        })

        when (step) {
            1 -> stepSubjects(col)
            2 -> stepTimeRange(col)
        }

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16) }
        }
        if (step > 1) {
            val back = Ui.button(this, Lang.t("‹ Back", "‹ തിരികെ"), filled = false).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    rightMargin = dp(6)
                }
                setOnClickListener { step--; build() }
            }
            nav.addView(back)
        }
        val nextLabel = when (step) {
            2 -> Lang.t("✓ Save Timetable", "✓ സേവ് ചെയ്യൂ")
            else -> Lang.t("Next ›", "അടുത്തത് ›")
        }
        val next = Ui.button(this, nextLabel).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (step > 1) leftMargin = dp(6)
            }
            setOnClickListener { onNext() }
        }
        nav.addView(next)
        col.addView(nav)

        setContentView(scroll)
    }

    // ---------------- Step 1: subjects ----------------
    private fun stepSubjects(col: LinearLayout) {
        col.addView(TextView(this).apply {
            text = Lang.t("Tick the subjects you want to study.", "പഠിക്കേണ്ട subjects ടിക്ക് ചെയ്യൂ.")
            setTextColor(Theme.grey(this@TimetableSetupActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        })
        val card = Ui.card(this)
        card.addView(checkRow(Lang.t("Select all", "എല്ലാം തിരഞ്ഞെടുക്കൂ"), selected.size == TimetableStore.SUBJECTS.size) { v ->
            selected.clear()
            if (v) TimetableStore.SUBJECTS.forEach { (en, ml) ->
                selected.add(TimetableStore.subjectName(en, ml))
            }
            ensureMaps()
            build()
        })
        card.addView(divider())
        TimetableStore.SUBJECTS.forEachIndexed { i, (en, ml) ->
            val name = TimetableStore.subjectName(en, ml)
            card.addView(checkRow(name, selected.contains(name)) { v ->
                if (v) {
                    if (!selected.contains(name)) selected.add(name)
                } else selected.remove(name)
                ensureMaps()
                build()
            })
            if (i < TimetableStore.SUBJECTS.size - 1) card.addView(divider())
        }
        col.addView(card)
    }

    private fun checkRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(10), dp(4), dp(10))
            isClickable = true
            isFocusable = true
            val cb = CheckBox(this@TimetableSetupActivity).apply {
                isChecked = checked
                buttonTintList = android.content.res.ColorStateList.valueOf(
                    Theme.green(this@TimetableSetupActivity)
                )
            }
            addView(cb)
            addView(TextView(this@TimetableSetupActivity).apply {
                text = label
                setTextColor(Theme.ink(this@TimetableSetupActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(8)
                }
            })
            setOnClickListener {
                cb.isChecked = !cb.isChecked // fires the checked-change listener below
            }
            cb.setOnCheckedChangeListener { _, v -> onToggle(v) }
        }

    /** Fill default from/to for newly selected subjects. */
    private fun ensureMaps() {
        val defaults = TimetableStore.defaultSlots()
            .associate { TimetableStore.displayName(it.subject) to it }
        var t = 7 * 60 + 30
        selected.forEach { name ->
            val d = defaults[name]
            if (!fromMins.containsKey(name)) fromMins[name] = (d?.startMin ?: t) % 1440
            if (!toMins.containsKey(name)) toMins[name] = (d?.endMin ?: (t + 60)) % 1440
            if (d == null) t += 90
        }
    }

    // ---------------- Step 2: time range (from → to) ----------------
    private fun stepTimeRange(col: LinearLayout) {
        col.addView(TextView(this).apply {
            text = Lang.t(
                "Set each subject's time — from → to.",
                "ഓരോ subject-ന്റെയും സമയം സെറ്റ് ചെയ്യൂ — from → to."
            )
            setTextColor(Theme.grey(this@TimetableSetupActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        })
        val card = Ui.card(this)
        selected.forEachIndexed { i, name ->
            card.addView(rangeRow(name))
            if (i < selected.size - 1) card.addView(divider())
        }
        col.addView(card)
        col.addView(TextView(this).apply {
            text = Lang.t(
                "Slot alerts come as phone notifications — even when the app is closed.",
                "Slot സമയത്ത് ഫോൺ നോട്ടിഫിക്കേഷൻ വരും — ആപ്പ് അടച്ചിട്ടാലും."
            )
            setTextColor(Theme.grey(this@TimetableSetupActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
        })
    }

    private fun rangeRow(name: String): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(10), dp(4), dp(10))
        }
        row.addView(TextView(this).apply {
            text = name
            setTextColor(Theme.ink(this@TimetableSetupActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
        })
        val pills = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }
        val from = fromMins[name] ?: (7 * 60 + 30)
        val to = toMins[name] ?: (9 * 60)
        val fromPill = Ui.pill(this, "🕐 " + TimetableStore.fmtTime(from), true)
        val arrow = TextView(this).apply {
            text = " → "
            setTextColor(Theme.grey(this@TimetableSetupActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(dp(4), 0, dp(4), 0)
        }
        val toPill = Ui.pill(this, TimetableStore.fmtTime(to), false)
        val total = TextView(this).apply {
            val d = durOf(name)
            text = if (d > 0) "= " + TimetableStore.fmtDur(d) else "—"
            setTextColor(if (d > 0) Theme.green(this@TimetableSetupActivity) else Theme.red(this@TimetableSetupActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = dp(8) }
        }

        fromPill.setOnClickListener {
            TimePickerDialog(
                this,
                { _, h, m ->
                    fromMins[name] = h * 60 + m
                    build()
                    // After "from" is set, open the "to" clock right away.
                    openToPicker(name)
                },
                from / 60, from % 60, false
            ).show()
        }
        toPill.setOnClickListener { openToPicker(name) }

        pills.addView(fromPill)
        pills.addView(arrow)
        pills.addView(toPill)
        pills.addView(total)
        row.addView(pills)
        return row
    }

    private fun openToPicker(name: String) {
        val to = toMins[name] ?: (9 * 60)
        TimePickerDialog(
            this,
            { _, h, m ->
                toMins[name] = h * 60 + m
                build()
            },
            to / 60, to % 60, false
        ).show()
    }

    // ---------------- nav ----------------
    private fun onNext() {
        when (step) {
            1 -> {
                if (selected.isEmpty()) {
                    Toast.makeText(
                        this,
                        Lang.t("Tick at least one subject.", "ഒരു subject എങ്കിലും ടിക്ക് ചെയ്യൂ."),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                ensureMaps()
                step = 2; build()
            }
            2 -> save()
        }
    }

    private fun save() {
        for (name in selected) {
            if (durOf(name) <= 0) {
                Toast.makeText(
                    this,
                    Lang.t(
                        "\"To\" must be after \"From\" for $name.",
                        "$name-ന്റെ \"to\" സമയം \"from\"-ന് ശേഷമാകണം."
                    ),
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }
        val slots = selected.map { name ->
            TSlot(
                TimetableStore.newId(),
                name,
                fromMins[name] ?: (7 * 60 + 30),
                durOf(name)
            )
        }
        TimetableStore.saveSlots(this, slots)
        ensureNotifPermission {
            TimetableAlarms.scheduleAll(this@TimetableSetupActivity)
            Toast.makeText(
                this@TimetableSetupActivity,
                Lang.t("Timetable saved! ✅", "ടൈംടേബിൾ സേവ് ചെയ്തു! ✅"),
                Toast.LENGTH_SHORT
            ).show()
            startActivity(Intent(this@TimetableSetupActivity, TimetableActivity::class.java))
            finish()
        }
    }

    private fun ensureNotifPermission(after: () -> Unit) {
        if (Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            after()
            return
        }
        pendingAfterPermission = after
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 9002)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 9002) {
            pendingAfterPermission?.invoke()
            pendingAfterPermission = null
        }
    }

    private fun divider(): View = View(this).apply {
        setBackgroundColor(Theme.divider(this@TimetableSetupActivity))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        )
    }
}
