package org.isoron.uhabits.activities

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.platform.time.getToday
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.dialogs.CheckmarkDialog
import org.isoron.uhabits.activities.common.dialogs.NumberDialog
import org.isoron.uhabits.activities.settings.SettingsActivity
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.utils.dismissCurrentAndShow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class EntryDialogTest {
    private val component
        get() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as HabitsApplication).component

    private fun withHost(block: (ActivityScenario<SettingsActivity>) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = (context.applicationContext as HabitsApplication).component.preferences
        val firstRun = prefs.isFirstRun
        val skipEnabled = prefs.isSkipEnabled
        val position = prefs.entryDialogPosition
        prefs.isFirstRun = false
        try {
            ActivityScenario.launch(SettingsActivity::class.java).use(block)
        } finally {
            prefs.isFirstRun = firstRun
            prefs.isSkipEnabled = skipEnabled
            prefs.entryDialogPosition = position
        }
    }

    private fun arguments() = Bundle().apply {
        putString("habitName", "Morning walk")
        putString("notes", "")
        putInt("value", Entry.UNKNOWN)
    }

    @Test
    fun eachBooleanActionSavesImmediatelyAndCancelDoesNotSave() = withHost { scenario ->
        scenario.onActivity { activity ->
            component.preferences.isSkipEnabled = true
            for ((button, expected) in listOf(
                R.id.yesBtn to Entry.YES_MANUAL,
                R.id.skipBtnNumber to Entry.SKIP,
                R.id.noBtn to Entry.NO
            )) {
                var saved: Int? = null
                var notes = ""
                val dialog = CheckmarkDialog().apply {
                    arguments = arguments()
                    onToggle = { value, text -> saved = value; notes = text }
                }
                dialog.showNow(activity.supportFragmentManager, "action")
                assertEquals("Morning walk", dialog.requireDialog().findViewById<TextView>(R.id.habitName).text)
                dialog.requireDialog().findViewById<EditText>(R.id.notes).setText("  first\nsecond  ")
                dialog.requireDialog().findViewById<View>(button).performClick()
                assertEquals(expected, saved)
                assertEquals("first\nsecond", notes)
                assertFalse(dialog.requireDialog().isShowing)
                activity.supportFragmentManager.executePendingTransactions()
            }
            var saved = false
            val dialog = CheckmarkDialog().apply {
                arguments = arguments()
                onToggle = { _, _ -> saved = true }
            }
            dialog.showNow(activity.supportFragmentManager, "cancel")
            dialog.requireDialog().findViewById<View>(R.id.dismissBtn).performClick()
            assertFalse(saved)
        }
    }

    @Test
    fun placementAndSkipVisibilityFollowSettings() = withHost { scenario ->
        scenario.onActivity { activity ->
            val prefs = component.preferences
            prefs.isSkipEnabled = false
            for ((position, expectedGravity) in listOf("center" to Gravity.CENTER_VERTICAL, "bottom" to Gravity.BOTTOM)) {
                prefs.entryDialogPosition = position
                val dialog = CheckmarkDialog().apply { arguments = arguments() }
                dialog.showNow(activity.supportFragmentManager, "position")
                val window = dialog.requireDialog().window!!
                assertEquals(expectedGravity, window.attributes.gravity and Gravity.VERTICAL_GRAVITY_MASK)
                assertEquals(View.GONE, dialog.requireDialog().findViewById<View>(R.id.skipBtnNumber).visibility)
                dialog.dismiss()
                activity.supportFragmentManager.executePendingTransactions()
            }
        }
    }

    @Test
    fun numericEntryRejectsPartialNumbersAndSavesLocalizedDecimals() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.GERMANY)
        try {
            withHost { scenario ->
                scenario.onActivity { activity ->
                    Locale.setDefault(Locale.GERMANY)
                    var saved: Double? = null
                    val dialog = NumberDialog().apply {
                        arguments = arguments().apply { putDouble("value", 0.125) }
                        onToggle = { value, _ -> saved = value }
                    }
                    dialog.showNow(activity.supportFragmentManager, "number")
                    val input = dialog.requireDialog().findViewById<EditText>(R.id.value)
                    assertEquals("0,125", input.text.toString())
                    input.setText("1,2,3")
                    dialog.requireDialog().findViewById<View>(R.id.yesBtn).performClick()
                    assertTrue(dialog.requireDialog().isShowing)
                    assertNotNull(input.error)
                    input.setText("1,25")
                    dialog.requireDialog().findViewById<View>(R.id.yesBtn).performClick()
                    assertEquals(1.25, saved!!, 0.0001)
                }
            }
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun recreatedDialogRetainsNotesAndSavesToTheViewedDay() = withHost { scenario ->
        val date = getToday().minus(3)
        var id = -1L
        scenario.onActivity { activity ->
            val habit = component.modelFactory.buildHabit().apply { name = "Draft test" }
            component.habitList.add(habit)
            id = habit.id!!
            val dialog = CheckmarkDialog().apply {
                arguments = arguments().apply {
                    putLong("habitId", id)
                    putLong("timestamp", date.unixTime)
                    putString("habitName", habit.name)
                }
            }
            dialog.dismissCurrentAndShow(activity.supportFragmentManager, "checkmarkDialog")
            dialog.requireDialog().findViewById<EditText>(R.id.notes).setText("keep this draft")
        }
        scenario.recreate()
        scenario.onActivity { activity ->
            val dialog = activity.supportFragmentManager.findFragmentByTag("checkmarkDialog") as CheckmarkDialog
            assertEquals("keep this draft", dialog.requireDialog().findViewById<EditText>(R.id.notes).text.toString())
            dialog.requireDialog().findViewById<View>(R.id.yesBtn).performClick()
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val habits = (context.applicationContext as HabitsApplication).component.habitList
        val habit = habits.getById(id)!!
        val deadline = System.currentTimeMillis() + 5000
        while (habit.originalEntries.get(date).value != Entry.YES_MANUAL && System.currentTimeMillis() < deadline) {
            Thread.sleep(25)
        }
        assertEquals(Entry.YES_MANUAL, habit.originalEntries.get(date).value)
        assertEquals("keep this draft", habit.originalEntries.get(date).notes)
        assertEquals(Entry.UNKNOWN, habit.originalEntries.get(getToday()).value)
    }
}
