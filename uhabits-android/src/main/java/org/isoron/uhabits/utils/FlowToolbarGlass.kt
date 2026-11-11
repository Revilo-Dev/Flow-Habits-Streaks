package org.isoron.uhabits.utils

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.View
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import org.isoron.uhabits.R
import kotlin.math.ceil

internal class FlowToolbarGlass(private val toolbar: Toolbar, private val compact: Boolean) {
    private val backgrounds = mutableMapOf<View, Drawable?>()
    private var previousSource: View? = null
    private val inset = toolbar.resources.getDimensionPixelSize(R.dimen.flow_sticky_control_inset) / 2
    private val menu = FlowGlassMenu(toolbar, if (compact) inset else 0)

    fun update(source: View?) {
        toolbar.children.filterIsInstance<ActionMenuView>().forEach(menu::bind)
        if (source == null) {
            for ((control, background) in backgrounds) setBackground(control, background)
            backgrounds.clear()
            previousSource = null
            return
        }
        val surface = toolbar.sres.getColor(R.attr.flowSurfaceColor)
        for (control in toolbar.children) {
            val navigation = control is ImageButton
            if (!navigation && control !is ActionMenuView) continue
            if (source === previousSource && backgrounds.containsKey(control)) continue
            val radius = if (navigation) Float.MAX_VALUE else toolbar.dim(R.dimen.flow_toolbar_pill_radius)
            val glass = FlowGlassDrawable(control, source, surface, radius)
            val horizontal = if (navigation) inset else 0
            val vertical = if (navigation || compact) inset else 0
            val insetGlass = InsetDrawable(glass, horizontal, vertical, horizontal, vertical)
            if (!backgrounds.containsKey(control)) backgrounds[control] = control.background
            val background = if (navigation) {
                val mask = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.WHITE)
                }
                RippleDrawable(
                    ColorStateList.valueOf(toolbar.sres.getColor(R.attr.flowRippleColor)),
                    insetGlass,
                    InsetDrawable(mask, horizontal, vertical, horizontal, vertical)
                )
            } else {
                insetGlass
            }
            setBackground(control, background)
        }
        previousSource = source
    }

    fun refresh() = backgrounds.keys.forEach { it.invalidate() }

    private fun setBackground(control: View, background: Drawable?) {
        val padding = Rect(control.paddingLeft, control.paddingTop, control.paddingRight, control.paddingBottom)
        control.background = background
        control.setPadding(padding.left, padding.top, padding.right, padding.bottom)
    }
}

internal class FlowGlassDrawable(
    private val control: View,
    private val source: View,
    private val surface: Int,
    private val cornerRadius: Float
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlight = FlowEdgeHighlight(control.context)
    private val clip = Path()
    private val region = RectF()
    private val blur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Backdrop() else null
    private var bitmap: Bitmap? = null
    private var pixels = IntArray(0)
    private var scratch = IntArray(0)

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val radius = if (FlowBlurSettings.isEnabled(control.context)) {
            FlowBlurSettings.intensity(control.context) * control.resources.displayMetrics.density
        } else {
            0f
        }
        region.set(bounds)
        val corner = cornerRadius.coerceAtMost(region.height() / 2)
        val save = canvas.save()
        clip.rewind()
        clip.addRoundRect(region, corner, corner, Path.Direction.CW)
        canvas.clipPath(clip)
        val hasBackdrop = FlowBlurSettings.isEnabled(control.context) && source.isAttachedToWindow
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blur != null &&
            radius > 0 && canvas.isHardwareAccelerated && hasBackdrop
        ) {
            blur.draw(canvas, control, source, bounds, radius, surface)
        } else if (radius > 0 && hasBackdrop) {
            drawSoftwareBackdrop(canvas, radius)
        } else if (hasBackdrop) {
            canvas.drawColor(surface)
            val backdrop = canvas.save()
            canvas.translateToBackdrop(control, source, Rect())
            source.draw(canvas)
            canvas.restoreToCount(backdrop)
        }
        paint.color = if (hasBackdrop) FlowBlurSettings.tint(control.context, surface) else surface
        canvas.drawRoundRect(region, corner, corner, paint)
        canvas.restoreToCount(save)
        highlight.draw(canvas, bounds, corner)
    }

    private fun drawSoftwareBackdrop(canvas: Canvas, radius: Float) {
        val scale = 4
        val padding = control.samplePadding()
        val sampleBounds = Rect(bounds).apply { inset(-padding, -padding) }
        val width = (sampleBounds.width() / scale).coerceAtLeast(1)
        val height = (sampleBounds.height() / scale).coerceAtLeast(1)
        if (bitmap?.width != width || bitmap?.height != height) {
            bitmap?.recycle()
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            pixels = IntArray(width * height)
            scratch = IntArray(width * height)
        }
        val sample = bitmap ?: return
        sample.eraseColor(surface)
        val recording = Canvas(sample)
        recording.scale(width.toFloat() / sampleBounds.width(), height.toFloat() / sampleBounds.height())
        recording.translateToBackdrop(control, source, sampleBounds)
        source.draw(recording)
        FlowBackdropBlur.apply(sample, pixels, scratch, (radius / scale).toInt().coerceAtLeast(1))
        paint.color = Color.WHITE
        paint.isFilterBitmap = true
        canvas.drawBitmap(sample, null, sampleBounds, paint)
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOutline(outline: android.graphics.Outline) {
        outline.setRoundRect(bounds, cornerRadius.coerceAtMost(bounds.height() / 2f))
    }

    @Deprecated("Drawable opacity is no longer used by the renderer")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

private fun View.samplePadding() = ceil(FlowBlurSettings.MAX_INTENSITY * 2 * resources.displayMetrics.density).toInt()

private fun Canvas.translateToBackdrop(host: View, source: View, sample: Rect) {
    val hostPosition = IntArray(2).also(host::getLocationOnScreen)
    val sourcePosition = IntArray(2).also(source::getLocationOnScreen)
    translate(
        (sourcePosition[0] - hostPosition[0] - sample.left - source.scrollX).toFloat(),
        (sourcePosition[1] - hostPosition[1] - sample.top - source.scrollY).toFloat()
    )
}

@RequiresApi(Build.VERSION_CODES.S)
private class Backdrop {
    private val node = RenderNode("Flow toolbar backdrop")
    private val recordingBounds = Rect()
    private var previousRadius = 0f

    fun draw(canvas: Canvas, host: View, source: View, bounds: Rect, radius: Float, surface: Int) {
        // Keep the sampled area independent of the selected blur strength
        val padding = host.samplePadding()
        recordingBounds.set(bounds)
        recordingBounds.inset(-padding, -padding)
        node.setPosition(recordingBounds)
        val recording = node.beginRecording(recordingBounds.width(), recordingBounds.height())
        recording.drawColor(surface)
        recording.translateToBackdrop(host, source, recordingBounds)
        try {
            source.draw(recording)
        } finally {
            node.endRecording()
        }
        if (radius != previousRadius) {
            node.setRenderEffect(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
            previousRadius = radius
        }
        canvas.drawRenderNode(node)
    }
}
