package org.isoron.uhabits.utils

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.AppBarLayout
import org.isoron.uhabits.R

class FlowScrollingBehavior @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppBarLayout.ScrollingViewBehavior(context, attrs) {
    private val toolbarHeight = context.resources.getDimensionPixelSize(R.dimen.flow_toolbar_height)

    override fun onMeasureChild(
        parent: CoordinatorLayout,
        child: View,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int
    ): Boolean {
        val header = parent.getDependencies(child).filterIsInstance<AppBarLayout>().firstOrNull()
        val overlap = if (header == null) 0 else toolbarHeight + header.paddingTop
        val measured = super.onMeasureChild(
            parent,
            child,
            parentWidthMeasureSpec,
            widthUsed,
            parentHeightMeasureSpec,
            heightUsed - overlap
        )
        // Keep the scroll viewport behind the pinned controls without changing header offsets
        child.translationY = -overlap.toFloat()
        return measured
    }
}

internal fun WindowInsetsCompat.flowTopInset(): Int =
    getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()).top

internal fun View.flowChromeHeight(insets: WindowInsetsCompat): Int =
    resources.getDimensionPixelSize(R.dimen.flow_toolbar_height) + insets.flowTopInset()

internal fun View.applyFlowContentInsets(includeBottom: Boolean = false) {
    val top = paddingTop
    val bottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val safeBottom = if (includeBottom) {
            insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()).bottom
        } else {
            0
        }
        view.setPadding(view.paddingLeft, top + view.flowChromeHeight(insets), view.paddingRight, bottom + safeBottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}
