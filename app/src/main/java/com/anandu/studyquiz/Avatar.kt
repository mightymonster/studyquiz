package com.anandu.studyquiz

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.anandu.studyquiz.Ui.dp

/** 12 cartoon animal avatars (emoji on coloured circles — no asset files). */
object Avatar {
    val EMOJIS = listOf("🦁", "🐯", "🐘", "🐼", "🦊", "🐰", "🐒", "🐸", "🦉", "🐧", "🐬", "🦄")
    private val COLORS = listOf(
        "#E57373", "#F06292", "#BA68C8", "#7986CB",
        "#64B5F6", "#4DB6AC", "#81C784", "#AED581",
        "#FFB74D", "#FF8A65", "#A1887F", "#90A4AE"
    )
    private const val PREF = "studyquiz_prefs"
    private const val KEY = "avatar_idx"

    fun index(ctx: Context): Int =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt(KEY, 0).coerceIn(EMOJIS.indices)

    fun setIndex(ctx: Context, i: Int) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putInt(KEY, i.coerceIn(EMOJIS.indices)).apply()
    }

    fun emoji(ctx: Context): String = EMOJIS[index(ctx)]
    fun color(ctx: Context): Int = colorFor(index(ctx))
    fun colorFor(i: Int): Int = Color.parseColor(COLORS[i.coerceIn(EMOJIS.indices)])

    /** Circular TextView showing avatar [i]'s emoji on its colour. */
    fun circle(ctx: Context, i: Int, sizeDp: Int = 52, emojiSp: Float = 26f): TextView =
        TextView(ctx).apply {
            text = EMOJIS[i.coerceIn(EMOJIS.indices)]
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, emojiSp)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorFor(i))
            }
            val s = ctx.dp(sizeDp)
            layoutParams = LinearLayout.LayoutParams(s, s)
            isClickable = true
            isFocusable = true
        }

    /** Style an existing TextView (e.g. the top-bar button) as the current avatar. */
    fun styleButton(act: Activity, tv: TextView, sizeDp: Int = 44) {
        tv.text = emoji(act)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        tv.gravity = Gravity.CENTER
        tv.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color(act))
        }
        val s = act.dp(sizeDp)
        tv.layoutParams = LinearLayout.LayoutParams(s, s).apply { leftMargin = act.dp(8) }
        tv.isClickable = true
        tv.isFocusable = true
    }

    /** Avatar picker grid. Returns (grid, getChosen). */
    fun pickerGrid(act: Activity, initial: Int): Pair<GridLayout, () -> Int> {
        var chosen = initial.coerceIn(EMOJIS.indices)
        val cells = mutableListOf<TextView>()
        val grid = GridLayout(act).apply {
            columnCount = 4
            setPadding(act.dp(4), act.dp(4), act.dp(4), act.dp(4))
        }
        fun refresh() {
            cells.forEachIndexed { j, c -> c.alpha = if (j == chosen) 1f else 0.4f }
        }
        EMOJIS.indices.forEach { i ->
            val cell = circle(act, i, 56, 28f)
            cell.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = act.dp(64)
                columnSpec = GridLayout.spec(i % 4, 1f)
                setMargins(act.dp(6), act.dp(6), act.dp(6), act.dp(6))
            }
            cell.setOnClickListener { chosen = i; refresh() }
            cells.add(cell)
            grid.addView(cell)
        }
        refresh()
        return grid to { chosen }
    }

    /** Standalone picker dialog. Calls [onPick] with the chosen index. */
    fun pickDialog(act: Activity, onPick: (Int) -> Unit) {
        val (grid, getChosen) = pickerGrid(act, index(act))
        AlertDialog.Builder(act)
            .setTitle(Lang.t("Pick your avatar", "അവതാർ തിരഞ്ഞെടുക്കൂ"))
            .setView(grid)
            .setPositiveButton(Lang.t("Done", "ശരി")) { d, _ ->
                onPick(getChosen()); d.dismiss()
            }
            .setNegativeButton(Lang.t("Cancel", "വേണ്ട"), null)
            .show()
    }
}
