package org.isoron.uhabits.activities

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.switchmaterial.SwitchMaterial
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitSkipOptionsTest {
    @Test
    fun optionsFollowSkipPreferenceAndSurviveRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = (context.applicationContext as HabitsApplication).component.preferences
        val original = preferences.isSkipEnabled
        try {
            for (enabled in listOf(false, true)) {
                preferences.isSkipEnabled = enabled
                val intent = Intent(context, EditHabitActivity::class.java)
                ActivityScenario.launch<EditHabitActivity>(intent).use { scenario ->
                    scenario.onActivity { activity ->
                        val options = activity.findViewById<View>(R.id.skipOptions)
                        assertEquals(if (enabled) View.VISIBLE else View.GONE, options.visibility)
                        activity.findViewById<SwitchMaterial>(R.id.countSkipsWeekly).isChecked = true
                        activity.findViewById<SwitchMaterial>(R.id.countSkipsStreak).isChecked = false
                    }
                    scenario.recreate()
                    scenario.onActivity { activity ->
                        assertTrue(activity.findViewById<SwitchMaterial>(R.id.countSkipsWeekly).isChecked)
                        assertFalse(activity.findViewById<SwitchMaterial>(R.id.countSkipsStreak).isChecked)
                    }
                }
            }
        } finally {
            preferences.isSkipEnabled = original
        }
    }
}
