package org.isoron.uhabits.widgets

import android.graphics.Color
import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class WidgetSurfaceTest {
    @Test
    fun launcherCanApplyTintToEveryRemoteLayout() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val layouts = listOf(
            R.layout.widget_wrapper,
            R.layout.widget_error,
            R.layout.checkmark_stackview_widget,
            R.layout.frequency_stackview_widget,
            R.layout.history_stackview_widget,
            R.layout.score_stackview_widget,
            R.layout.streak_stackview_widget,
            R.layout.target_stackview_widget
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (layout in layouts) {
                for ((opacity, expectedAlpha) in listOf(0 to 1, 102 to 102, 255 to 254)) {
                    val views = RemoteViews(context.packageName, layout)
                    WidgetSurface.apply(views, opacity)
                    val root = views.apply(context, null)
                    assertEquals(android.R.id.background, root.id)
                    assertNotNull(root.background)
                    assertEquals(Color.argb(expectedAlpha, 255, 255, 255), root.backgroundTintList!!.defaultColor)
                }
            }
        }
    }
}
