package org.isoron.uhabits.utils

import android.content.Context
import androidx.preference.PreferenceManager
import org.isoron.uhabits.R

// Use the toolkit system switch and store the custom primary independently for each theme
internal object FlowAccent {
    const val ENABLED = "flow_theming_enabled"
    const val SYSTEM = "global_use_color_palette"
    const val LIGHT = "oneui_palette_primary_light"
    const val DARK = "oneui_palette_primary_dark"

    data class State(val enabled: Boolean, val system: Boolean, val light: Int, val dark: Int)

    private enum class Preset(val light: Int, val dark: Int, val lightStyle: Int, val darkStyle: Int) {
        DEFAULT(R.color.flow_light_accent, R.color.flow_dark_accent, 0, 0),
        BLUE(R.color.flow_blue_light, R.color.flow_blue_dark, R.style.FlowBlueAccentLight, R.style.FlowBlueAccentDark),
        TEAL(R.color.flow_teal_light, R.color.flow_teal_dark, R.style.FlowTealAccentLight, R.style.FlowTealAccentDark),
        PURPLE(
            R.color.flow_purple_light,
            R.color.flow_purple_dark,
            R.style.FlowPurpleAccentLight,
            R.style.FlowPurpleAccentDark
        )
    }

    fun apply(context: Context, dark: Boolean) {
        if (!isEnabled(context)) return
        val style = if (usesSystem(context)) {
            if (dark) R.style.FlowSystemAccentDark else R.style.FlowSystemAccentLight
        } else {
            val preset = Preset.entries[choice(context, dark)]
            if (dark) preset.darkStyle else preset.lightStyle
        }
        if (style != 0) context.theme.applyStyle(style, true)
    }

    fun usesSystem(context: Context) = preferences(context).getBoolean(SYSTEM, true)

    fun isEnabled(context: Context) = preferences(context).getBoolean(ENABLED, true)

    fun choice(context: Context, dark: Boolean): Int {
        val saved = preferences(context).getInt(if (dark) DARK else LIGHT, 0)
        return Preset.entries.indexOfFirst { context.getColor(if (dark) it.dark else it.light) == saved }
            .coerceAtLeast(0)
    }

    fun select(context: Context, dark: Boolean, position: Int) {
        val preset = Preset.entries[position]
        preferences(context).edit()
            .putInt(if (dark) DARK else LIGHT, context.getColor(if (dark) preset.dark else preset.light))
            .apply()
    }

    fun snapshot(context: Context) = State(isEnabled(context), usesSystem(context), choice(context, false), choice(context, true))

    fun isAccentKey(key: String?) = key == ENABLED || key == SYSTEM || key == LIGHT || key == DARK

    private fun preferences(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
}
