package org.isoron.uhabits.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.graphics.ColorUtils
import org.isoron.uhabits.R

internal fun View.perfectStreakBackground(colors: IntArray, completed: Boolean): GradientDrawable {
    val radius = dim(R.dimen.flow_card_radius)
    val backdrop = if (completed) {
        val glowColor = ColorUtils.blendARGB(colors[0], colors[1], 0.5f)
        object : FlowHighlightDrawable(context, radius) {
            private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
            private val area = RectF()
            private val spread = dim(R.dimen.flow_medium_spacing)

            override fun draw(canvas: Canvas) {
                // Draw a soft colored halo behind the opaque card
                for (step in 12 downTo 1) {
                    val distance = spread * step / 12
                    area.set(bounds)
                    area.inset(-distance, -distance)
                    glow.color = glowColor
                    glow.alpha = (13 - step) * 2
                    canvas.drawRoundRect(area, radius + distance, radius + distance, glow)
                }
                super.draw(canvas)
            }
        }
    } else {
        FlowHighlightDrawable(context, radius)
    }
    backdrop.orientation = GradientDrawable.Orientation.TL_BR
    backdrop.colors = colors
    clipToOutline = false
    (parent as? View)?.invalidate()
    return backdrop
}
