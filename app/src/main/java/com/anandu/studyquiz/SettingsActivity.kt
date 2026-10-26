package com.anandu.studyquiz

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import com.anandu.studyquiz.Ui.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Settings: account, appearance, language, backup, reminder, more.
 * Back arrow (top-left) returns to Profile — there is no Settings tab;
 * Settings is reached via the gear in Home / Profile headers.
 */
class SettingsActivity : Activity() {

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Backup.onResult(this, requestCode, resultCode, data)) build()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 9001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingAfterPermission?.invoke()
            } else {
                android.widget.Toast.makeText(
                    this,
                    Lang.t("Notification permission denied — reminder can't alert you.", "നോട്ടിഫിക്കേഷൻ അനുമതി കിട്ടിയില്ല — ഓർമ്മപ്പെടുത്തൽ വരില്ല."),
                    android.widget.Toast.LENGTH_LONG
                ).show()
                build() // refresh toggle state
            }
            pendingAfterPermission = null
        }
    }

    private fun goProfile() {
        startActivity(
            Intent(this, ProfileActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
        finish()
    }

    private fun build() {
        // Settings is reached via the Profile gear (no bottom nav).
        val (root, col) = Ui.screenWithNav(this, "profile")

        // ---- header: back arrow + title + decorative art ----
        val headRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headRow.addView(TextView(this).apply {
            text = "‹"
            gravity = Gravity.CENTER
            setTextColor(Theme.ink(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(2), dp(2), dp(10), dp(2))
            isClickable = true
            isFocusable = true
            setOnClickListener { goProfile() }
        })
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = Lang.t("Settings", "സെറ്റിംഗ്സ്")
            setTextColor(Theme.ink(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTypeface(typeface, Typeface.BOLD)
        })
        titleCol.addView(TextView(this).apply {
            text = Lang.t("Make the app yours.", "ആപ്പ് നിനക്കിഷ്ടമുള്ളപോലെ ആക്കൂ.")
            setTextColor(Theme.grey(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(2) }
        })
        headRow.addView(titleCol)
        headRow.addView(TextView(this).apply {
            text = "📚🪴"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)
        })
        headRow.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(6) }
        col.addView(headRow)

        accountSection(col)
        appearanceSection(col)
        languageSection(col)
        backupSection(col)
        reminderSection(col)
        moreSection(col)

        setContentView(root)
    }

    // ---------- account ----------
    private fun accountSection(col: LinearLayout) {
        Ui.sectionLabel(this, col, "◉", Lang.t("Account", "അക്കൗണ്ട്"))
        val card = Ui.card(this)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val av = Avatar.circle(this, Avatar.index(this), 56, 28f)
        av.setOnClickListener {
            Avatar.pickDialog(this) { i -> Avatar.setIndex(this, i); build() }
        }
        row.addView(av)
        val nameCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(12)
            }
        }
        nameCol.addView(TextView(this).apply {
            text = Guest.name(this@SettingsActivity)
                ?: Lang.t("Guest", "അതിഥി")
            setTextColor(Theme.ink(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
        })
        nameCol.addView(TextView(this).apply {
            text = Lang.t("Tap avatar to change it", "അവതാർ മാറ്റാൻ അതിൽ അമർത്തൂ")
            setTextColor(Theme.grey(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        })
        val changeBtn = Ui.smallOutlineButton(this, Lang.t("Change profile", "പ്രൊഫൈൽ മാറ്റുക"))
        changeBtn.setOnClickListener { askProfile() }
        nameCol.addView(changeBtn)
        row.addView(nameCol)
        card.addView(row)
        col.addView(card)
    }

    /** "Change profile" chooser: change name and/or avatar. */
    private fun askProfile() {
        val items = arrayOf(
            Lang.t("Change name", "പേര് മാറ്റൂ"),
            Lang.t("Change avatar", "അവതാർ മാറ്റൂ")
        )
        AlertDialog.Builder(this)
            .setTitle(Lang.t("Change profile", "പ്രൊഫൈൽ മാറ്റുക"))
            .setItems(items) { d, which ->
                d.dismiss()
                if (which == 0) askName()
                else Avatar.pickDialog(this) { i -> Avatar.setIndex(this, i); build() }
            }
            .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
            .show()
    }

    private fun askName() {
        val input = EditText(this).apply {
            setText(Guest.name(this@SettingsActivity) ?: "")
            setSingleLine(true)
            setTextColor(Theme.ink(this@SettingsActivity))
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val pad = LinearLayout(this).apply {
            setPadding(dp(20), dp(12), dp(20), dp(4)); addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(Lang.t("Your name", "നിന്റെ പേര്"))
            .setView(pad)
            .setPositiveButton(Lang.t("Save", "സേവ് ചെയ്യൂ")) { d, _ ->
                val n = input.text.toString().trim()
                if (n.isNotEmpty()) { Guest.setName(this, n); build() }
                d.dismiss()
            }
            .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
            .show()
    }

    // ---------- appearance ----------
    private fun appearanceSection(col: LinearLayout) {
        Ui.sectionLabel(this, col, "◐", Lang.t("Appearance", "രൂപം"))
        val card = Ui.card(this)
        val themeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            Ui.pill(this, Lang.t("☀ Light", "☀ ലൈറ്റ്"), Theme.mode == Theme.LIGHT) to Theme.LIGHT,
            Ui.pill(this, Lang.t("🌙 Dark", "🌙 ഡാർക്ക്"), Theme.mode == Theme.DARK) to Theme.DARK,
            Ui.pill(this, Lang.t("📱 System", "📱 സിസ്റ്റം"), Theme.mode == Theme.SYSTEM) to Theme.SYSTEM
        ).forEach { (pill, m) ->
            pill.setOnClickListener { Theme.setMode(this, m); recreate() }
            themeRow.addView(pill)
        }
        card.addView(themeRow)
        col.addView(card)
    }

    // ---------- language ----------
    private fun languageSection(col: LinearLayout) {
        Ui.sectionLabel(this, col, "文", Lang.t("Language", "ഭാഷ"))
        val card = Ui.card(this)
        val langRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val mlPill = Ui.pill(this, "മലയാളം", Lang.ml)
        val enPill = Ui.pill(this, "English", !Lang.ml)
        mlPill.setOnClickListener { Lang.setMalayalam(this, true); recreate() }
        enPill.setOnClickListener { Lang.setMalayalam(this, false); recreate() }
        langRow.addView(mlPill); langRow.addView(enPill)
        card.addView(langRow)
        col.addView(card)
    }

    // ---------- backup ----------
    private fun backupSection(col: LinearLayout) {
        Ui.sectionLabel(this, col, "⟳", Lang.t("Backup & Restore", "ബാക്കപ്പ്"))
        val card = Ui.card(this)
        val backupRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val bExp = Ui.button(this, Lang.t("↑ Backup", "↑ ബാക്കപ്പ്")).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(4); topMargin = 0
            }
            setOnClickListener { Backup.export(this@SettingsActivity) }
        }
        val bImp = Ui.button(this, Lang.t("↓ Restore", "↓ റീസ്റ്റോർ"), filled = false).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(4); topMargin = 0
            }
            setOnClickListener { Backup.import(this@SettingsActivity) }
        }
        backupRow.addView(bExp); backupRow.addView(bImp)
        card.addView(backupRow)
        card.addView(TextView(this).apply {
            text = Lang.t(
                "Keep the backup file safe (WhatsApp yourself / Drive). Your data lives only on this phone.",
                "ബാക്കപ്പ് ഫയൽ സൂക്ഷിച്ച് വെക്കൂ (WhatsApp-ൽ സ്വന്തത്തിന് / Drive). നിന്റെ ഡേറ്റ ഈ ഫോണിൽ മാത്രമാണ്."
            )
            setTextColor(Theme.grey(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        })
        col.addView(card)
    }

    // ---------- daily reminder ----------
    private fun reminderSection(col: LinearLayout) {
        Ui.sectionLabel(this, col, "◷", Lang.t("Daily Reminder", "ദിവസേനയുള്ള ഓർമ്മപ്പെടുത്തൽ"))
        val card = Ui.card(this)

        val toggleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val labelCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        labelCol.addView(Ui.styleBody(TextView(this).apply {
            text = Lang.t("Daily Reminder", "ദിവസേനയുള്ള ഓർമ്മപ്പെടുത്തൽ")
            setTextColor(Theme.ink(this@SettingsActivity))
            setTypeface(typeface, Typeface.BOLD)
        }, 15f))
        labelCol.addView(TextView(this).apply {
            text = Lang.t("Remind me to study daily", "ദിവസവും പഠിക്കാൻ ഓർമ്മിപ്പിക്കൂ")
            setTextColor(Theme.grey(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        })
        toggleRow.addView(labelCol)
        val sw = Switch(this).apply { isChecked = ReminderPrefs.enabled(this@SettingsActivity) }
        toggleRow.addView(sw)
        card.addView(toggleRow)

        val timeBtn = Ui.button(
            this,
            Lang.t("🕐 Time: %s  ›", "🕐 സമയം: %s  ›").format(reminderTimeText()),
            filled = false
        )
        timeBtn.setOnClickListener { pickReminderTime() }
        card.addView(timeBtn)

        // Listener is attached after isChecked is set, so it only fires on user taps.
        sw.setOnCheckedChangeListener { _, on ->
            if (on) {
                ensureNotifPermission {
                    ReminderPrefs.setEnabled(this@SettingsActivity, true)
                    Reminder.schedule(this@SettingsActivity)
                }
            } else {
                ReminderPrefs.setEnabled(this@SettingsActivity, false)
                Reminder.cancel(this@SettingsActivity)
            }
        }
        col.addView(card)
    }

    private fun reminderTimeText(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, ReminderPrefs.hour(this@SettingsActivity))
            set(Calendar.MINUTE, ReminderPrefs.minute(this@SettingsActivity))
        }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    private fun pickReminderTime() {
        TimePickerDialog(
            this,
            { _, h, m ->
                ReminderPrefs.setTime(this, h, m)
                if (ReminderPrefs.enabled(this)) Reminder.schedule(this)
                build()
            },
            ReminderPrefs.hour(this), ReminderPrefs.minute(this), false
        ).show()
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
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 9001)
    }

    // ---------- more ----------
    private fun moreSection(col: LinearLayout) {
        Ui.sectionLabel(this, col, "⋯", Lang.t("More", "കൂടുതൽ"))
        val card = Ui.card(this)
        card.addView(Ui.moreRow(
            this, "💬",
            Lang.t("Send anonymous feedback", "ഫീഡ്ബാക്ക് അയക്കൂ"),
            iconBg = Theme.lightBlue(this)
        ) { Ui.openFeedback(this) })
        card.addView(divider())
        card.addView(Ui.moreRow(
            this, "📤",
            Lang.t("Share app", "ആപ്പ് ഷെയർ ചെയ്യൂ"),
            iconBg = Theme.mint(this)
        ) {
            val msg = "StudyQuiz — CSEB/PSC exam prep app 📚\nDownload: https://github.com/mightymonster/studyquiz/releases\nOr via Google Drive: https://drive.google.com/drive/folders/1h4XtT3ZOMdVQlhZdUMljEvBCT3KHQdIc"
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, msg)
            }
            startActivity(Intent.createChooser(send, Lang.t("Share StudyQuiz", "StudyQuiz ഷെയർ ചെയ്യൂ")))
        })
        card.addView(divider())
        card.addView(Ui.moreRow(
            this, "ⓘ",
            Lang.t("About", "കുറിച്ച്"),
            value = "v${AppVersion.NAME}",
            iconBg = Theme.amberTint(this)
        ) { showAbout() })
        col.addView(card)
    }

    private fun divider(): android.view.View = android.view.View(this).apply {
        setBackgroundColor(Theme.divider(this@SettingsActivity))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        )
    }

    private fun showAbout() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        col.addView(TextView(this).apply {
            text = "Developer ©️"
            setTextColor(Theme.ink(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        })
        col.addView(TextView(this).apply {
            text = "@an_an_dhu.sreeni141"
            setTextColor(Theme.green(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/an_an_dhu.sreeni141"))
                )
            }
        })
        col.addView(TextView(this).apply {
            text = "v${AppVersion.NAME}"
            setTextColor(Theme.grey(this@SettingsActivity))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
        })
        AlertDialog.Builder(this)
            .setTitle(Lang.t("About StudyQuiz", "StudyQuiz-നെ കുറിച്ച്"))
            .setView(col)
            .setPositiveButton(Lang.t("Close", "അടയ്ക്കൂ"), null)
            .show()
    }
}
