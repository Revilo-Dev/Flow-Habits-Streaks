package org.isoron.uhabits.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout

/** Reaching the top of the list ends this gesture; a fresh pull opens the header. */
class FlowHeaderBehavior @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppBarLayout.Behavior(context, attrs) {
    private var expansionAllowed = false

    override fun onStartNestedScroll(
        parent: CoordinatorLayout, child: AppBarLayout, directTargetChild: View,
        target: View, axes: Int, type: Int
    ): Boolean {
        val started = super.onStartNestedScroll(parent, child, directTargetChild, target, axes, type)
        if (started && type == ViewCompat.TYPE_TOUCH) {
            expansionAllowed = !target.canScrollVertically(-1)
        }
        return started
    }

    override fun onNestedPreScroll(
        parent: CoordinatorLayout, child: AppBarLayout, target: View,
        dx: Int, dy: Int, consumed: IntArray, type: Int
    ) {
        if (target.canScrollVertically(-1)) expansionAllowed = false
        if (dy < 0 && !expansionAllowed) return
        super.onNestedPreScroll(parent, child, target, dx, dy, consumed, type)
    }

    override fun onNestedScroll(
        parent: CoordinatorLayout, child: AppBarLayout, target: View,
        dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int,
        type: Int, consumed: IntArray
    ) {
        if (dyUnconsumed < 0 && !expansionAllowed) return
        super.onNestedScroll(parent, child, target, dxConsumed, dyConsumed,
            dxUnconsumed, dyUnconsumed, type, consumed)
    }
}

/** Collapsing the header and scrolling the page are separate visual states. */
fun AppBarLayout.bindFlowHeader(
    toolbar: Toolbar,
    compact: Boolean = false,
    onStateChanged: (collapsed: Boolean, contentScrolled: Boolean) -> Unit = { _, _ -> }
) {
    var collapsed = false
    var previousState: Pair<Boolean, Boolean>? = null

    fun View.hasScrolledContent(): Boolean {
        if (this === this@bindFlowHeader || visibility != View.VISIBLE) return false
        if (this is RecyclerView || this is NestedScrollView) return canScrollVertically(-1)
        return this is ViewGroup && (0 until childCount).any { getChildAt(it).hasScrolledContent() }
    }

    fun update() {
        val scrolled = (parent as? View)?.hasScrolledContent() == true
        val state = collapsed to scrolled
        if (state == previousState) return
        previousState = state
        toolbar.updateFlowStickyControls(collapsed && scrolled, compact)
        onStateChanged(collapsed, scrolled)
    }

    addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { bar, offset ->
        collapsed = bar.totalScrollRange > 0 && -offset >= bar.totalScrollRange
        update()
    })
    val scrollListener = ViewTreeObserver.OnScrollChangedListener { update() }
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
        }
    })
}
