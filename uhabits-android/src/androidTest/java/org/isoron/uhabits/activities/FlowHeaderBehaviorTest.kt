package org.isoron.uhabits.activities

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.appbar.AppBarLayout
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.FlowHeaderBehavior
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlowHeaderBehaviorTest {
    @Test
    fun reachingContentTopRequiresAnotherTouchBeforeExpanding() {
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
            fun pull(type: Int) = behavior.onNestedScroll(parent, header, content,
                0, 0, 0, -40, type, IntArray(2))

            assertTrue(start(ViewCompat.TYPE_TOUCH))
            scrolled = false // The same gesture has now reached the first card.
            pull(ViewCompat.TYPE_TOUCH)
            assertEquals(closedOffset, behavior.topAndBottomOffset)
            start(ViewCompat.TYPE_NON_TOUCH)
            pull(ViewCompat.TYPE_NON_TOUCH) // A fling cannot cross the boundary either.
            assertEquals(closedOffset, behavior.topAndBottomOffset)

            assertTrue(start(ViewCompat.TYPE_TOUCH))
            pull(ViewCompat.TYPE_TOUCH)
            assertTrue(behavior.topAndBottomOffset > closedOffset)
        }
    }
}
