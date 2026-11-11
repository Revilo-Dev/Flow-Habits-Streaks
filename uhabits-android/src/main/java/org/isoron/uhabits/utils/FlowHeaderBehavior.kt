package org.isoron.uhabits.utils

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import org.isoron.uhabits.R

class FlowHeaderBehavior @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppBarLayout.Behavior(context, attrs) {
    private var expansionAllowed = false
    private var pulledHeaderOpen = false
    private var pullAtContentTop = 0
    private val pullThreshold = (16 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

    override fun onStartNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        val started = super.onStartNestedScroll(parent, child, directTargetChild, target, axes, type)
        if (started && type == ViewCompat.TYPE_TOUCH) {
            expansionAllowed = !target.canScrollVertically(-1)
            pulledHeaderOpen = false
            pullAtContentTop = 0
        }
        return started
    }

    override fun onNestedPreScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        if (target.canScrollVertically(-1)) expansionAllowed = false
        if (dy < 0 && (!expansionAllowed || type != ViewCompat.TYPE_TOUCH)) return
        if (dy > 0) {
            pulledHeaderOpen = false
            pullAtContentTop = 0
        }
        val previousOffset = topAndBottomOffset
        super.onNestedPreScroll(parent, child, target, dx, dy, consumed, type)
        if (dy < 0 && topAndBottomOffset > previousOffset) pulledHeaderOpen = true
    }

    override fun onNestedScroll(
        parent: CoordinatorLayout,
        child: AppBarLayout,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        var headerDelta = dyUnconsumed
        if (headerDelta < 0) {
            if (type != ViewCompat.TYPE_TOUCH) return
            if (!expansionAllowed) {
                if (target.canScrollVertically(-1)) return
                pullAtContentTop -= headerDelta
                if (pullAtContentTop <= pullThreshold) return
                headerDelta = pullThreshold - pullAtContentTop
                expansionAllowed = true
            }
        }
        val previousOffset = topAndBottomOffset
        super.onNestedScroll(
            parent,
            child,
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            headerDelta,
            type,
            consumed
        )
        if (dyUnconsumed < 0 && topAndBottomOffset > previousOffset) pulledHeaderOpen = true
    }

    override fun onStopNestedScroll(parent: CoordinatorLayout, child: AppBarLayout, target: View, type: Int) {
        super.onStopNestedScroll(parent, child, target, type)
        // Snap open only after a touch pull moves the header itself
        if (type == ViewCompat.TYPE_TOUCH && expansionAllowed && pulledHeaderOpen) {
            child.setExpanded(true, true)
        }
    }
}

fun AppBarLayout.bindFlowHeader(
    toolbar: Toolbar,
    compact: Boolean = false,
    expandedContent: View? = null,
    onStateChanged: (collapsed: Boolean, contentScrolled: Boolean) -> Unit = { _, _ -> }
) {
    setBackgroundColor(Color.TRANSPARENT)
    stateListAnimator = null
    outlineProvider = null
    elevation = 0f
    translationZ = 2f
    toolbar.setBackgroundColor(Color.TRANSPARENT)
    val edgeMargin = resources.getDimensionPixelSize(R.dimen.flow_sticky_control_inset)
    toolbar.updatePaddingRelative(start = toolbar.paddingStart + edgeMargin, end = toolbar.paddingEnd + edgeMargin)
    val glass = FlowToolbarGlass(toolbar, compact)
    var collapsed = false
    var previousState: Pair<Boolean, Boolean>? = null

    fun View.scrolledContent(): View? {
        if (this === this@bindFlowHeader || visibility != View.VISIBLE) return null
        if (this is RecyclerView || this is NestedScrollView) return this
        if (this is ViewGroup) {
            for (index in 0 until childCount) {
                getChildAt(index).scrolledContent()?.let { return it }
            }
        }
        return null
    }

    fun update() {
        val source = (parent as? View)?.scrolledContent()
        val contentScrolled = source?.canScrollVertically(-1) == true
        glass.update(source.takeIf { contentScrolled })
        glass.refresh()
        val state = collapsed to contentScrolled
        if (state == previousState) {
            return
        }
        previousState = state
        onStateChanged(collapsed, contentScrolled)
    }

    val offsetListener = AppBarLayout.OnOffsetChangedListener { bar, offset ->
        collapsed = bar.totalScrollRange > 0 && -offset >= bar.totalScrollRange
        expandedContent?.apply {
            val progress = if (bar.totalScrollRange > 0) {
                (-offset.toFloat() / bar.totalScrollRange).coerceIn(0f, 1f)
            } else {
                0f
            }
            alpha = 1f - progress
            visibility = if (collapsed) View.INVISIBLE else View.VISIBLE
        }
        update()
    }
    addOnOffsetChangedListener(offsetListener)
    toolbar.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        previousState = null
        update()
    }
    val scrollListener = ViewTreeObserver.OnScrollChangedListener { update() }
    FlowBlurSettings.observe(toolbar) {
        previousState = null
        update()
    }
    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        private var observer: ViewTreeObserver? = null
        override fun onViewAttachedToWindow(view: View) {
            observer = view.viewTreeObserver.also { it.addOnScrollChangedListener(scrollListener) }
            previousState = null
            view.post { update() }
        }
        override fun onViewDetachedFromWindow(view: View) {
            observer?.takeIf { it.isAlive }?.removeOnScrollChangedListener(scrollListener)
            observer = null
            glass.update(null)
        }
    })
}
