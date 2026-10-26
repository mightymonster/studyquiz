package com.anandu.studyquiz

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.anandu.studyquiz.Ui.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Daily timetable: today's slots with tick-off checkboxes, progress bar,
 * current-slot highlight, edit mode (reorder / edit / delete / add),
 * and alert settings (notifications on-off, pre-alert minutes).
 */
class TimetableActivity : Activity() {

    private var editMode = false
    private var pendingAfterPermission: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Theme.applyTo(this)
        super.onCreate(savedInstanceState)
        Lang.init(this)
        build()
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun build() {
        val (scroll, col) = Ui.screen(this)
        header(col)

        val slots = TimetableStore.getSlots(this)
        if (slots.isEmpty()) {
            emptyState(col)
        } else {
            progressCard(col)
            slots.forEach { s -> col.addView(slotRow(s, slots)) }
            if (editMode) col.addView(addButton())
            alertSettings(col)
        }

        setContentView(scroll)
    }

    // ---------------- header ----------------
    private fun header(col: LinearLayout) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) }
        }
        row.addView(TextView(this).apply {
            text = "‹"
            gravity = Gravity.CENTER
            setTextColor(Theme.ink(this@TimetableActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(2), dp(10), dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener { finish() }
        })
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = Lang.t("Study Timetable", "സ്റ്റഡി ടൈംടേബിൾ")
            setTextColor(Theme.ink(this@TimetableActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTypeface(typeface, Typeface.BOLD)
        })
        titleCol.addView(TextView(this).apply {
            text = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date())
            setTextColor(Theme.grey(this@TimetableActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        })
        row.addView(titleCol)
        if (TimetableStore.hasTimetable(this)) {
            val edit = Ui.smallOutlineButton(
                this,
                if (editMode) Lang.t("✓ Done", "✓ ശരി") else Lang.t("✎ Edit", "✎ എഡിറ്റ്")
            )
            edit.setOnClickListener { editMode = !editMode; build() }
            row.addView(edit)
        }
        col.addView(row)
    }

    private fun emptyState(col: LinearLayout) {
        col.addView(TextView(this).apply {
            text = Lang.t(
                "No timetable yet.\nSet your subjects, time per subject\nand start times — in 3 quick steps.",
                "ടൈംടേബിൾ ഇതുവരെ സെറ്റ് ചെയ്തിട്ടില്ല.\nSubjects, സമയം, തുടങ്ങുന്ന സമയം —\n3 സ്റ്റെപ്പിൽ സെറ്റ് ചെയ്യൂ."
            )
            setTextColor(Theme.grey(this@TimetableActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(48); bottomMargin = dp(16) }
        })
        val b = Ui.button(this, Lang.t("🕖 Set up Timetable", "🕖 ടൈംടേബിൾ സെറ്റ് ചെയ്യൂ"))
        b.setOnClickListener {
            startActivity(Intent(this, TimetableSetupActivity::class.java))
        }
        col.addView(b)
    }

    // ---------------- progress ----------------
    private fun progressCard(col: LinearLayout) {
        val slots = TimetableStore.getSlots(this)
        val done = TimetableStore.doneIds(this)
        val n = slots.count { done.contains(it.id) }
        col.addView(TextView(this).apply {
            text = Lang.t("$n of ${slots.size} slots done", "${slots.size}-ൽ $n slots കഴിഞ്ഞു")
            setTextColor(Theme.ink(this@TimetableActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8); bottomMargin = dp(4) }
        })
        col.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = slots.size.coerceAtLeast(1)
            progress = n
            progressTintList = android.content.res.ColorStateList.valueOf(Theme.green(this@TimetableActivity))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8)
            ).apply { bottomMargin = dp(10) }
        })
    }

    // ---------------- slot rows (flat, directly on background — matches mockup) ----------------
    private fun slotRow(s: TSlot, all: List<TSlot>): LinearLayout {
        val done = TimetableStore.doneIds(this).contains(s.id)
        val nowMin = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) * 60 +
            Calendar.getInstance().get(Calendar.MINUTE)
        // Overnight slots (endMin > 1440) wrap past midnight.
        val isCurrent = !done && if (s.endMin <= 1440) {
            nowMin in s.startMin until s.endMin
        } else {
            nowMin >= s.startMin || nowMin < s.endMin - 1440
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(12), dp(8), dp(12))
            if (isCurrent) setBackgroundColor(Theme.mint(this@TimetableActivity))

            if (editMode) {
                // reorder arrows
                val idx = all.indexOf(s)
                addView(TextView(this@TimetableActivity).apply {
                    text = "↑"
                    setTextColor(Theme.green(this@TimetableActivity))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    isClickable = true; isFocusable = true
                    alpha = if (idx > 0) 1f else 0.25f
                    setOnClickListener { if (idx > 0) moveSlot(all, idx, idx - 1) }
                })
                addView(TextView(this@TimetableActivity).apply {
                    text = "↓"
                    setTextColor(Theme.green(this@TimetableActivity))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                    isClickable = true; isFocusable = true
                    alpha = if (idx < all.size - 1) 1f else 0.25f
                    setOnClickListener { if (idx < all.size - 1) moveSlot(all, idx, idx + 1) }
                })
            }

            val textCol = LinearLayout(this@TimetableActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = dp(4)
                }
            }
            textCol.addView(TextView(this@TimetableActivity).apply {
                text = TimetableStore.fmtRange(s)
                setTextColor(Theme.grey(this@TimetableActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            })
            textCol.addView(TextView(this@TimetableActivity).apply {
                text = s.subject + if (isCurrent) Lang.t(" • now", " • ഇപ്പോൾ") else ""
                setTextColor(if (done) Theme.muted(this@TimetableActivity) else Theme.ink(this@TimetableActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(textCol)

            if (editMode) {
                addView(TextView(this@TimetableActivity).apply {
                    text = "✎"
                    setTextColor(Theme.green(this@TimetableActivity))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    isClickable = true; isFocusable = true
                    setOnClickListener { editSlotDialog(s) }
                })
                addView(TextView(this@TimetableActivity).apply {
                    text = "🗑"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    isClickable = true; isFocusable = true
                    setOnClickListener { deleteSlotDialog(s) }
                })
            } else {
                val cb = CheckBox(this@TimetableActivity).apply {
                    isChecked = done
                    buttonTintList = android.content.res.ColorStateList.valueOf(
                        Theme.green(this@TimetableActivity)
                    )
                }
                cb.setOnCheckedChangeListener { _, v ->
                    TimetableStore.setDone(this@TimetableActivity, s.id, v)
                    build()
                }
                addView(cb)
            }
        }

        // row + thin divider
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(row)
            addView(View(this@TimetableActivity).apply {
                setBackgroundColor(Theme.divider(this@TimetableActivity))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
                )
            })
        }
    }

    private fun moveSlot(all: List<TSlot>, from: Int, to: Int) {
        // Slots are ordered by start time, so "moving" a row = swapping the two slots' start times.
        val reordered = all.toMutableList()
        val a = reordered[from]
        val b = reordered[to]
        reordered[from] = a.copy(startMin = b.startMin)
        reordered[to] = b.copy(startMin = a.startMin)
        TimetableStore.saveSlots(this, reordered)
        TimetableAlarms.scheduleAll(this)
        build()
    }

    private fun addButton(): LinearLayout {
        val b = Ui.button(this, Lang.t("＋ Add slot", "＋ Slot ചേർക്കൂ"), filled = false)
        b.setOnClickListener { editSlotDialog(null) }
        val wrap = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        wrap.addView(b)
        return wrap
    }

    // ---------------- edit / add dialog ----------------
    private fun editSlotDialog(s: TSlot?) {
        val isNew = s == null
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val nameInput = EditText(this).apply {
            setText(s?.subject ?: "")
            hint = Lang.t("Subject", "Subject")
            setSingleLine(true)
            setTextColor(Theme.ink(this@TimetableActivity))
        }
        form.addView(nameInput)

        var startMin = s?.startMin ?: (9 * 60)
        var durMin = s?.durMin ?: 60
        val timeBtn = Ui.button(this, "🕐 " + TimetableStore.fmtTime(startMin), filled = false)
        timeBtn.setOnClickListener {
            TimePickerDialog(
                this,
                { _, h, m ->
                    startMin = h * 60 + m
                    timeBtn.text = "🕐 " + TimetableStore.fmtTime(startMin)
                },
                startMin / 60, startMin % 60, false
            ).show()
        }
        form.addView(timeBtn)

        val durRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        durRow.addView(TextView(this).apply {
            text = Lang.t("Duration", "ദൈർഘ്യം")
            setTextColor(Theme.ink(this@TimetableActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val durTv = TextView(this).apply {
            text = TimetableStore.fmtDur(durMin)
            setTextColor(Theme.green(this@TimetableActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            minWidth = dp(86)
        }
        durRow.addView(Ui.smallOutlineButton(this, "−").apply {
            setOnClickListener {
                if (durMin - 15 >= 15) {
                    durMin -= 15
                    durTv.text = TimetableStore.fmtDur(durMin)
                }
            }
        })
        durRow.addView(durTv)
        durRow.addView(Ui.smallOutlineButton(this, "+").apply {
            setOnClickListener {
                if (durMin + 15 <= 300) {
                    durMin += 15
                    durTv.text = TimetableStore.fmtDur(durMin)
                }
            }
        })
        form.addView(durRow)

        // quick subject chips for new slots
        if (isNew) {
            form.addView(TextView(this).apply {
                text = Lang.t("Or pick:", "അല്ലെങ്കിൽ തിരഞ്ഞെടുക്കൂ:")
                setTextColor(Theme.grey(this@TimetableActivity))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            })
            val chips = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            TimetableStore.SUBJECTS.take(6).forEach { (en, ml) ->
                val chip = Ui.pill(this, TimetableStore.subjectName(en, ml), false)
                chip.setOnClickListener { nameInput.setText(TimetableStore.subjectName(en, ml)) }
                chips.addView(chip)
            }
            // horizontal scroll for chips
            val sc = android.widget.HorizontalScrollView(this).apply { addView(chips) }
            form.addView(sc)
        }

        AlertDialog.Builder(this)
            .setTitle(if (isNew) Lang.t("Add slot", "Slot ചേർക്കൂ") else Lang.t("Edit slot", "Slot എഡിറ്റ് ചെയ്യൂ"))
            .setView(form)
            .setPositiveButton(Lang.t("Save", "സേവ് ചെയ്യൂ")) { d, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(
                        this,
                        Lang.t("Enter a subject name.", "Subject-ന്റെ പേര് കൊടുക്കൂ."),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }
                val slots = TimetableStore.getSlots(this).toMutableList()
                if (isNew) slots.add(TSlot(TimetableStore.newId(), name, startMin, durMin))
                else {
                    val i = slots.indexOfFirst { it.id == s!!.id }
                    if (i >= 0) slots[i] = slots[i].copy(subject = name, startMin = startMin, durMin = durMin)
                }
                TimetableStore.saveSlots(this, slots)
                TimetableAlarms.scheduleAll(this)
                d.dismiss()
                build()
            }
            .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
            .show()
    }

    private fun deleteSlotDialog(s: TSlot) {
        AlertDialog.Builder(this)
            .setTitle(Lang.t("Delete slot?", "Slot ഡിലീറ്റ് ചെയ്യട്ടെ?"))
            .setMessage("${s.subject} (${TimetableStore.fmtRange(s)})")
            .setPositiveButton(Lang.t("Delete", "ഡിലീറ്റ് ചെയ്യൂ")) { d, _ ->
                TimetableStore.saveSlots(
                    this,
                    TimetableStore.getSlots(this).filter { it.id != s.id }
                )
                TimetableAlarms.scheduleAll(this)
                d.dismiss()
                build()
            }
            .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
            .show()
    }

    // ---------------- alert settings ----------------
    private fun alertSettings(col: LinearLayout) {
        Ui.sectionLabel(this, col, "🔔", Lang.t("Slot alerts", "Slot അലേർട്ടുകൾ"))
        val card = Ui.card(this)

        val toggleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        toggleRow.addView(TextView(this).apply {
            text = Lang.t("Phone notifications", "ഫോൺ നോട്ടിഫിക്കേഷൻ")
            setTextColor(Theme.ink(this@TimetableActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val sw = Switch(this).apply { isChecked = TimetableStore.notifEnabled(this@TimetableActivity) }
        toggleRow.addView(sw)
        card.addView(toggleRow)

        val preBtn = Ui.button(
            this,
            Lang.t(
                "⏰ Pre-alert: %s  ›",
                "⏰ മുൻകൂർ അലേർട്ട്: %s  ›"
            ).format(preText()),
            filled = false
        )
        preBtn.setOnClickListener { preDialog() }
        card.addView(preBtn)
        col.addView(card)

        sw.setOnCheckedChangeListener { _, on ->
            if (on) {
                ensureNotifPermission {
                    TimetableStore.setNotifEnabled(this@TimetableActivity, true)
                    TimetableAlarms.scheduleAll(this@TimetableActivity)
                }
            } else {
                TimetableStore.setNotifEnabled(this@TimetableActivity, false)
                TimetableAlarms.cancelAll(this@TimetableActivity)
            }
        }
    }

    private fun preText(): String {
        val m = TimetableStore.preMinutes(this)
        return if (m <= 0) Lang.t("Off", "ഓഫ്") else TimetableStore.fmtDur(m)
    }

    private fun preDialog() {
        val opts = arrayOf(
            Lang.t("Off", "ഓഫ്"),
            TimetableStore.fmtDur(5),
            TimetableStore.fmtDur(10),
            TimetableStore.fmtDur(15),
            TimetableStore.fmtDur(30)
        )
        val vals = intArrayOf(0, 5, 10, 15, 30)
        val cur = TimetableStore.preMinutes(this)
        AlertDialog.Builder(this)
            .setTitle(Lang.t("Alert me before each slot", "ഓരോ slot-നും മുൻപ് അലേർട്ട്"))
            .setSingleChoiceItems(opts, vals.indexOf(cur).coerceAtLeast(0)) { d, which ->
                TimetableStore.setPreMinutes(this, vals[which])
                TimetableAlarms.scheduleAll(this)
                d.dismiss()
                build()
            }
            .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
            .show()
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
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 9003)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 9003) {
            pendingAfterPermission?.invoke()
            pendingAfterPermission = null
        }
    }
}
