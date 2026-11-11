package org.isoron.uhabits.utils

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import kotlin.math.roundToInt

// Port the shared blur preferences from One UI Toolkit
internal object FlowBlurSettings {
    const val ENABLED = "global_blur_enabled"
    const val INTENSITY = "global_blur_intensity"
    const val OPACITY = "global_blur_alpha"
    const val MAX_INTENSITY = 40

    fun upgradeOpacity(context: Context) {
        val prefs = preferences(context)
        if (prefs.getBoolean("flow_glass_opacity_v2", false)) return
        val previous = prefs.getInt(OPACITY, 65).coerceIn(0, 100)
        val opacity = ((previous * 1.3f / 5).roundToInt() * 5).coerceAtMost(100)
        prefs.edit().putInt(OPACITY, opacity).putBoolean("flow_glass_opacity_v2", true).apply()
    }

    fun isEnabled(context: Context) = preferences(context).getBoolean(ENABLED, true)

    fun intensity(context: Context) = preferences(context).getInt(INTENSITY, 18).coerceIn(0, MAX_INTENSITY)

    fun tint(context: Context, color: Int): Int {
        val opacity = preferences(context).getInt(OPACITY, 85).coerceIn(0, 100)
        return ColorUtils.setAlphaComponent(color, (opacity * 255f / 100f).roundToInt())
    }

    fun observe(view: View, update: () -> Unit) {
        val prefs = preferences(view.context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ENABLED || key == INTENSITY || key == OPACITY) update()
        }
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                prefs.registerOnSharedPreferenceChangeListener(listener)
                update()
            }

            override fun onViewDetachedFromWindow(view: View) {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        })
        if (view.isAttachedToWindow) prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    private fun preferences(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
}
