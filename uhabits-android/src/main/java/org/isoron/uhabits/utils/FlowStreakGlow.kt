package org.isoron.uhabits.utils

import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.graphics.ColorUtils
import org.isoron.uhabits.R

internal fun View.updatePerfectStreakGlow(completed: Boolean) {
    val light = ColorUtils.calculateLuminance(sres.getColor(R.attr.flowBackgroundColor)) > 0.5
    elevation = when {
        completed -> 8 * resources.displayMetrics.density
        light -> dim(R.dimen.flow_card_elevation)
        else -> 0f
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val glow = context.getColor(R.color.flow_streak_end)
        outlineAmbientShadowColor = if (completed) glow else Color.BLACK
        outlineSpotShadowColor = if (completed) glow else Color.BLACK
    }
}
