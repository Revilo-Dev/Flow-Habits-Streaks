package org.isoron.uhabits.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import org.isoron.uhabits.R

class FlowScrollFades internal constructor(
    private val scrollable: View,
    private val topFade: View,
    private val bottomFade: View
) {
    private var topAllowed = true

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { update() }

    init {
        FlowBlurSettings.observe(scrollable) { update() }
        scrollable.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update() }
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
        val enabled = FlowBlurSettings.isEnabled(scrollable.context)
        topFade.visibility = if (enabled && topAllowed && scrollable.canScrollVertically(-1)) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        bottomFade.visibility = if (enabled && scrollable.canScrollVertically(1)) View.VISIBLE else View.INVISIBLE
    }
}

fun CoordinatorLayout.addFlowScrollFades(
    scrollable: View,
    bottomHeight: Int = resources.getDimensionPixelSize(R.dimen.flow_scroll_edge_fade_size)
): FlowScrollFades {
    val height = resources.getDimensionPixelSize(R.dimen.flow_toolbar_height)
    val background = sres.getColor(R.attr.flowBackgroundColor)
    val topFade = createScrollFade(context, background, top = true)
    val bottomFade = createScrollFade(context, background, top = false)
    var content = scrollable
    while (content.parent is View && content.parent !== this) content = content.parent as View
    addView(
        bottomFade,
        indexOfChild(content) + 1,
        CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, bottomHeight).apply {
            gravity = Gravity.BOTTOM
        }
    )
    // Draw from the physical top edge with the controls above the fade
    topFade.translationZ = 1f
    addView(
        topFade,
        CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
            gravity = Gravity.TOP
        }
    )
    ViewCompat.setOnApplyWindowInsetsListener(topFade) { view, insets ->
        val size = height + insets.flowTopInset()
        if (view.layoutParams.height != size) {
            view.layoutParams = view.layoutParams.apply { this.height = size }
        }
        insets
    }
    ViewCompat.requestApplyInsets(topFade)
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
