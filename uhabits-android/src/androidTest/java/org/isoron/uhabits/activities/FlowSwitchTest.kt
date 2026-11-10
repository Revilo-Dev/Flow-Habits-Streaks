package org.isoron.uhabits.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.LayerDrawable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlowSwitchTest {
    @Test
    fun directTapPersistsAndRecycledBindingRestoresTheSwitch() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.FlowTheme_Light)
            val manager = PreferenceManager(context).apply { sharedPreferencesName = "flow_switch_test" }
            val screen = manager.createPreferenceScreen(context)
            val preference = SwitchPreferenceCompat(context).apply { key = "enabled" }
            screen.addPreference(preference)
            preference.isChecked = false
            val row = FrameLayout(context)
            val switch = LayoutInflater.from(context).inflate(R.layout.flow_preference_switch, row, false) as SwitchCompat
            row.addView(switch)
            preference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(row))
            assertFalse(switch.isChecked)
            switch.performClick()
            assertTrue(preference.isChecked)
            assertTrue(manager.sharedPreferences.getBoolean("enabled", false))
            preference.isChecked = false
            preference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(row))
            assertFalse(switch.isChecked)
            manager.sharedPreferences.edit().clear().commit()
        }
    }

    @Test
    fun thumbRemainsCircularAtTheMeasuredSwitchHeight() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.FlowTheme_Light)
            val switch = LayoutInflater.from(context).inflate(R.layout.flow_preference_switch, null) as SwitchCompat
            val size = (48 * context.resources.displayMetrics.density).toInt()
            val spec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
            switch.measure(spec, spec)
            switch.layout(0, 0, size, size)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            switch.draw(Canvas(bitmap))
            val circle = (switch.thumbDrawable as LayerDrawable).getDrawable(1).bounds
            assertTrue(circle.width() > 0)
            assertEquals(circle.width(), circle.height())
            bitmap.recycle()
        }
    }
}
