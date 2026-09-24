package org.isoron.uhabits.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.isoron.uhabits.R

/** Softly fades scroll content into the screen background at its visible edges. */
class FlowScrollFades internal constructor(
    private val scrollable: View,
    private val topFade: View,
    private val bottomFade: View
) {
    private var topAllowed = true

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { update() }

    init {
        scrollable.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            private var observer: ViewTreeObserver? = null

            override fun onViewAttachedToWindow(view: View) {
                observer = view.viewTreeObserver.also { it.addOnScrollChangedListener(scrollListener) }
                view.post { update() }
            }

            override fun onViewDetachedFromWindow(view: View) {
                observer?.takeIf { it.isAlive }?.removeOnScrollChangedListener(scrollListener)
                observer = null
            }
        })
    }

    fun update(topAllowed: Boolean = this.topAllowed) {
        this.topAllowed = topAllowed
        topFade.visibility = if (topAllowed && scrollable.canScrollVertically(-1)) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        bottomFade.visibility = if (scrollable.canScrollVertically(1)) View.VISIBLE else View.INVISIBLE
    }
}

fun ViewGroup.addFlowScrollFades(
    scrollable: View,
    topMargin: Int = 0,
    bottomHeight: Int? = null,
    includeTopInset: Boolean = false
): FlowScrollFades {
    val height = resources.getDimensionPixelSize(R.dimen.flow_scroll_edge_fade_size)
    val resolvedBottomHeight = bottomHeight ?: height
    val background = sres.getColor(R.attr.flowBackgroundColor)
    val topFade = createScrollFade(context, background, top = true)
    val bottomFade = createScrollFade(context, background, top = false)

    when (this) {
        is CoordinatorLayout -> {
            addView(topFade, CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
            ).apply {
                gravity = Gravity.TOP
                this.topMargin = topMargin
            })
            addView(bottomFade, CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resolvedBottomHeight
            ).apply {
                gravity = Gravity.BOTTOM
            })
        }
        is FrameLayout -> {
            addView(topFade, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height,
                Gravity.TOP
            ).apply { this.topMargin = topMargin })
            addView(bottomFade, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resolvedBottomHeight,
                Gravity.BOTTOM
            ))
        }
        else -> error("Flow scroll fades require a FrameLayout or CoordinatorLayout container")
    }
    if (includeTopInset) {
        ViewCompat.setOnApplyWindowInsetsListener(topFade) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val insetTop = maxOf(bars.top, cutout.top)
            when (val params = view.layoutParams) {
                is CoordinatorLayout.LayoutParams -> params.topMargin = topMargin + insetTop
                is FrameLayout.LayoutParams -> params.topMargin = topMargin + insetTop
            }
            view.layoutParams = view.layoutParams
            insets
        }
    }
    return FlowScrollFades(scrollable, topFade, bottomFade).also { it.update() }
}

private fun createScrollFade(context: Context, background: Int, top: Boolean): View {
    val colors = if (top) {
        intArrayOf(background, ColorUtils.setAlphaComponent(background, Color.TRANSPARENT))
    } else {
        intArrayOf(ColorUtils.setAlphaComponent(background, Color.TRANSPARENT), background)
    }
    return View(context).apply {
        this.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
}
