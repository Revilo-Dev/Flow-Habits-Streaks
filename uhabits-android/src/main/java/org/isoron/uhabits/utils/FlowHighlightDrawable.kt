package org.isoron.uhabits.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.graphics.ColorUtils
import org.isoron.uhabits.R

internal fun AppCompatSpinner.styleFlowPopup() {
    setPopupBackgroundDrawable(flowPopupSurface(dim(R.dimen.flow_dialog_radius)))
}

internal fun View.flowPopupSurface(radius: Float) =
    FlowHighlightDrawable(context, radius).apply {
        setColor(sres.getColor(R.attr.flowSurfaceSecondaryColor))
    }

internal class FlowHighlightDrawable(context: Context, private val radius: Float) : GradientDrawable() {
    private val highlight = FlowEdgeHighlight(context)

    init {
        cornerRadius = radius
        setColor(Color.TRANSPARENT)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        highlight.draw(canvas, bounds, radius)
    }
}

internal class FlowEdgeHighlight(context: Context) {
    private val color = context.getColor(R.color.flow_glass_border)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimension(R.dimen.flow_glass_border_width)
    }
    private val edge = RectF()
    private val lastBounds = Rect()

    fun draw(canvas: Canvas, bounds: Rect, cornerRadius: Float) {
        if (bounds.isEmpty) return
        if (lastBounds != bounds) {
            lastBounds.set(bounds)
            edge.set(bounds)
            val inset = paint.strokeWidth / 2f
            edge.inset(inset, inset)
            paint.shader = LinearGradient(
                0f,
                edge.top,
                0f,
                edge.bottom,
                color,
                ColorUtils.setAlphaComponent(color, 0),
                Shader.TileMode.CLAMP
            )
        }
        val radius = (cornerRadius - paint.strokeWidth / 2f).coerceIn(0f, edge.height().coerceAtLeast(0f) / 2f)
        canvas.drawRoundRect(edge, radius, radius, paint)
    }
}
