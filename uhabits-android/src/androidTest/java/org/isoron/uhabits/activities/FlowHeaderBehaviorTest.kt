package org.isoron.uhabits.activities

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.appbar.AppBarLayout
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.FlowHeaderBehavior
import org.isoron.uhabits.utils.FlowScrollingBehavior
import org.isoron.uhabits.utils.bindFlowHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlowHeaderBehaviorTest {
    @Test
    fun collapsedScrollViewportReachesBothWindowEdges() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.FlowTheme_Light)
            val parent = CoordinatorLayout(context)
            val behavior = FlowHeaderBehavior(context)
            val toolbarHeight = context.resources.getDimensionPixelSize(R.dimen.flow_toolbar_height)
            val header = AppBarLayout(context).apply { setPadding(0, 24, 0, 0) }
            header.addView(
                View(context).apply { minimumHeight = toolbarHeight },
                AppBarLayout.LayoutParams(MATCH_PARENT, toolbarHeight + 200).apply {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                }
            )
            parent.addView(
                header,
                CoordinatorLayout.LayoutParams(MATCH_PARENT, toolbarHeight + 224).apply {
                    this.behavior = behavior
                }
            )
            val content = View(context)
            parent.addView(
                content,
                CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    this.behavior = FlowScrollingBehavior(context)
                }
            )
            parent.measure(
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
            )
            parent.layout(0, 0, 400, 800)
            behavior.setTopAndBottomOffset(-header.totalScrollRange)
            parent.dispatchDependentViewsChanged(header)
            assertEquals(0f, content.y, 0.01f)
            assertEquals(parent.height, content.height)
        }
    }

    @Test
    fun expandedContentFadesDuringCollapseAndReturnsOnExpansion() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.FlowTheme_Light)
            val parent = CoordinatorLayout(context)
            val behavior = FlowHeaderBehavior(context)
            val header = AppBarLayout(context)
            val expanded = View(context).apply { minimumHeight = 56 }
            header.addView(
                expanded,
                AppBarLayout.LayoutParams(MATCH_PARENT, 300).apply {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                }
            )
            parent.addView(
                header,
                CoordinatorLayout.LayoutParams(MATCH_PARENT, 300).apply { this.behavior = behavior }
            )
            val content = View(context)
            parent.addView(content, CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            header.bindFlowHeader(Toolbar(context), expandedContent = expanded)
            parent.measure(
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
            )
            parent.layout(0, 0, 400, 800)
            behavior.onStartNestedScroll(
                parent,
                header,
                content,
                content,
                ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH
            )
            behavior.onNestedPreScroll(parent, header, content, 0, 100, IntArray(2), ViewCompat.TYPE_TOUCH)
            assertTrue(expanded.alpha > 0f && expanded.alpha < 1f)
            assertEquals(View.VISIBLE, expanded.visibility)
            behavior.onNestedPreScroll(parent, header, content, 0, 300, IntArray(2), ViewCompat.TYPE_TOUCH)
            assertEquals(0f, expanded.alpha, 0.001f)
            assertEquals(View.INVISIBLE, expanded.visibility)
            behavior.onStartNestedScroll(
                parent,
                header,
                content,
                content,
                ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH
            )
            behavior.onNestedScroll(
                parent,
                header,
                content,
                0,
                0,
                0,
                -400,
                ViewCompat.TYPE_TOUCH,
                IntArray(2)
            )
            assertEquals(1f, expanded.alpha, 0.001f)
            assertEquals(View.VISIBLE, expanded.visibility)
        }
    }

    @Test
    fun reachingContentTopAllowsExpansionAfterAnExtraPull() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.FlowTheme_Light)
            val parent = CoordinatorLayout(context)
            val behavior = FlowHeaderBehavior(context)
            val header = AppBarLayout(context)
            header.addView(View(context).apply { minimumHeight = 56 },
                AppBarLayout.LayoutParams(MATCH_PARENT, 300).apply {
                    scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                        AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
                })
            parent.addView(header, CoordinatorLayout.LayoutParams(MATCH_PARENT, 300).apply {
                this.behavior = behavior
            })
            var scrolled = true
            val content = object : View(context) {
                override fun canScrollVertically(direction: Int) = direction < 0 && scrolled
            }
            parent.addView(content, CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            parent.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY))
            parent.layout(0, 0, 400, 800)
            val closedOffset = -header.totalScrollRange
            assertTrue(closedOffset < 0)
            behavior.setTopAndBottomOffset(closedOffset)
            fun start(type: Int) = behavior.onStartNestedScroll(parent, header, content, content,
                ViewCompat.SCROLL_AXIS_VERTICAL, type)
            fun pull(type: Int, distance: Int) = behavior.onNestedScroll(parent, header, content,
                0, 0, 0, -distance, type, IntArray(2))
            val threshold = (16 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)

            assertTrue(start(ViewCompat.TYPE_TOUCH))
            scrolled = false // The same gesture has now reached the first card.
            pull(ViewCompat.TYPE_TOUCH, threshold)
            assertEquals(closedOffset, behavior.topAndBottomOffset)
            start(ViewCompat.TYPE_NON_TOUCH)
            pull(ViewCompat.TYPE_NON_TOUCH, threshold)
            assertEquals(closedOffset, behavior.topAndBottomOffset)

            pull(ViewCompat.TYPE_TOUCH, 8)
            assertEquals(closedOffset + 8, behavior.topAndBottomOffset)

            behavior.setTopAndBottomOffset(closedOffset)
            assertTrue(start(ViewCompat.TYPE_TOUCH))
            pull(ViewCompat.TYPE_TOUCH, 8)
            assertTrue(behavior.topAndBottomOffset > closedOffset)
        }
    }
}
