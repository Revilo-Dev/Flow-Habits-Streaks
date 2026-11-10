package org.isoron.uhabits.activities

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import org.isoron.platform.time.getToday
import org.isoron.uhabits.BuildConfig
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.AccessibleHistoryChartView
import org.isoron.uhabits.activities.habits.edit.EditHabitActivity
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.activities.habits.show.ShowHabitActivity
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.ui.views.HistoryChart
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FlowHabitFeaturesTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = ApplicationProvider.getApplicationContext<Context>() as HabitsApplication
    private val device get() = UiDevice.getInstance(instrumentation)
    private lateinit var habit: Habit
    private var scenario: ActivityScenario<*>? = null

    @Before
    fun createHabit() {
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(app.packageName, android.Manifest.permission.POST_NOTIFICATIONS)
        }
        instrumentation.runOnMainSync {
            app.component.preferences.isFirstRun = false
            app.component.preferences.isSkipEnabled = true
            habit = app.component.modelFactory.buildHabit().apply { name = "Walking feature check" }
            app.component.habitList.add(habit)
            habit.originalEntries.add(Entry(getToday(), Entry.YES_MANUAL))
            habit.originalEntries.add(Entry(getToday().minus(1), Entry.SKIP, "Walked in the park"))
            habit.originalEntries.add(Entry(getToday().minus(2), Entry.YES_MANUAL))
            habit.recompute()
        }
    }

    @After
    fun cleanUp() {
        scenario?.close()
        instrumentation.runOnMainSync {
            if (::habit.isInitialized) {
                app.component.habitList.getById(habit.id!!)?.let {
                    app.component.habitList.remove(it)
                }
            }
        }
    }

    @Test
    fun skipPolicySurvivesRecreationAndSavesOnlyWhenConfirmed() {
        val intent = Intent(app, EditHabitActivity::class.java).putExtra("habitId", habit.id!!)
        val editor = ActivityScenario.launch<EditHabitActivity>(intent)
        scenario = editor
        editor.onActivity {
            assertFalse(it.findViewById<SwitchMaterial>(R.id.skipsCountTowardsStreaks).isChecked)
            it.findViewById<SwitchMaterial>(R.id.skipsCountTowardsStreaks).performClick()
        }
        editor.recreate()
        val saved = CountDownLatch(1)
        val listener = object : CommandRunner.Listener {
            override fun onCommandFinished(command: Command) {
                if (command is EditHabitCommand && command.habitId == habit.id) saved.countDown()
            }
        }
        editor.onActivity {
            assertTrue(it.findViewById<SwitchMaterial>(R.id.skipsCountTowardsStreaks).isChecked)
            assertFalse(habit.skipsCountTowardsStreaks)
            app.component.commandRunner.addListener(listener)
            it.findViewById<android.view.View>(R.id.buttonSave).performClick()
        }
        assertTrue(saved.await(5, TimeUnit.SECONDS))
        instrumentation.runOnMainSync { app.component.commandRunner.removeListener(listener) }
        assertTrue(habit.skipsCountTowardsStreaks)
        assertEquals(3, habit.streaks.getBest(1).single().length)
    }

    @Test
    fun calendarNoteTapShowsAPreviewAndOffersExplicitEditing() {
        app.component.preferences.isShortToggleEnabled = true
        val intent = Intent(app, ShowHabitActivity::class.java).setData(Uri.parse(habit.uriString))
        val detail = ActivityScenario.launch<ShowHabitActivity>(intent)
        scenario = detail
        detail.onActivity { it.findViewById<AppBarLayout>(R.id.appBar).setExpanded(false, false) }
        device.waitForIdle()
        val point = IntArray(2)
        detail.onActivity {
            val chart = it.findViewById<AccessibleHistoryChartView>(R.id.historyChart)
            val day = (chart.view as HistoryChart).visibleDays().first { cell -> cell.date == getToday().minus(1) }
            assertTrue(chart.getGlobalVisibleRect(Rect()))
            chart.getLocationOnScreen(point)
            point[0] += ((day.left + day.size / 2) * chart.resources.displayMetrics.density).toInt()
            point[1] += ((day.top + day.size / 2) * chart.resources.displayMetrics.density).toInt()
        }
        device.click(point[0], point[1])
        assertTrue(device.wait(Until.hasObject(By.text("Walked in the park")), 5000))
        assertEquals(Entry.SKIP, habit.originalEntries.get(getToday().minus(1)).value)
        capture("calendar-note-preview")
        device.findObject(By.text(app.getString(R.string.flow_edit_entry))).click()
        assertTrue(device.wait(Until.hasObject(By.res(BuildConfig.APPLICATION_ID, "notes")), 5000))
        assertEquals("Walked in the park", device.findObject(By.res(BuildConfig.APPLICATION_ID, "notes")).text)
        device.findObject(By.text("Cancel")).click()
        assertEquals(Entry.SKIP, habit.originalEntries.get(getToday().minus(1)).value)
    }

    @Test
    fun returningFromDetailAndRecreationKeepTheViewedDays() {
        val home = ActivityScenario.launch(ListHabitsActivity::class.java)
        scenario = home
        home.onActivity {
            it.rootView.header.scrollToDataOffset(10)
            it.findViewById<AppBarLayout>(R.id.flowHomeAppBar).setExpanded(false, false)
        }
        device.waitForIdle()
        home.onActivity {
            assertEquals(android.view.View.INVISIBLE, it.rootView.largeHeader.visibility)
            it.startActivity(Intent(it, ShowHabitActivity::class.java).setData(Uri.parse(habit.uriString)))
        }
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
        home.onActivity { assertEquals(10, it.rootView.header.dataOffset) }
        home.recreate()
        home.onActivity {
            assertEquals(10, it.rootView.header.dataOffset)
            assertEquals(10, it.rootView.listView.dataOffset)
        }
        device.waitForIdle()
        home.onActivity {
            assertEquals(android.view.View.INVISIBLE, it.rootView.largeHeader.visibility)
        }
        capture("today-restored")
    }

    @Test
    fun calendarViewportSurvivesRecreation() {
        val intent = Intent(app, ShowHabitActivity::class.java).setData(Uri.parse(habit.uriString))
        val detail = ActivityScenario.launch<ShowHabitActivity>(intent)
        scenario = detail
        device.waitForIdle()
        detail.onActivity {
            val chart = it.findViewById<AccessibleHistoryChartView>(R.id.historyChart)
            chart.view!!.dataOffset = 5
            chart.invalidate()
        }
        detail.recreate()
        device.waitForIdle()
        detail.onActivity {
            assertEquals(5, it.findViewById<AccessibleHistoryChartView>(R.id.historyChart).view!!.dataOffset)
        }
    }

    @Test
    fun numericEntryPreservesInvalidInputAndRestoresItsSaveTarget() {
        instrumentation.runOnMainSync {
            habit.type = org.isoron.uhabits.core.models.HabitType.NUMERICAL
            habit.targetValue = 20.0
            habit.unit = "minutes"
            app.component.habitList.update(habit)
            habit.recompute()
        }
        val intent = Intent(app, ShowHabitActivity::class.java).setData(Uri.parse(habit.uriString))
        val detail = ActivityScenario.launch<ShowHabitActivity>(intent)
        scenario = detail
        detail.onActivity { activity ->
            org.isoron.uhabits.activities.common.dialogs.NumberDialog().apply {
                arguments = android.os.Bundle().apply {
                    putDouble("value", 12.5)
                    putString("notes", "Numeric draft")
                    putLong("habit", habit.id!!)
                    putLong("date", getToday().unixTime)
                }
            }.show(activity.supportFragmentManager, "numericFeatureCheck")
        }
        assertTrue(device.wait(Until.hasObject(By.res(BuildConfig.APPLICATION_ID, "value")), 5000))
        assertEquals("12.5", device.findObject(By.res(BuildConfig.APPLICATION_ID, "value")).text)
        capture("numeric-entry")
        detail.onActivity { activity ->
            val dialog = activity.supportFragmentManager.findFragmentByTag("numericFeatureCheck") as org.isoron.uhabits.activities.common.dialogs.NumberDialog
            val field = dialog.requireDialog().findViewById<android.widget.EditText>(R.id.value)
            field.setText("invalid")
            dialog.save()
            assertTrue(dialog.requireDialog().isShowing)
            assertTrue(field.error != null)
        }
        detail.recreate()
        val saved = CountDownLatch(1)
        val listener = object : CommandRunner.Listener {
            override fun onCommandFinished(command: Command) { saved.countDown() }
        }
        detail.onActivity { activity ->
            val dialog = activity.supportFragmentManager.findFragmentByTag("numericFeatureCheck") as org.isoron.uhabits.activities.common.dialogs.NumberDialog
            val field = dialog.requireDialog().findViewById<android.widget.EditText>(R.id.value)
            assertEquals("invalid", field.text.toString())
            field.setText("2.75")
            app.component.commandRunner.addListener(listener)
            dialog.save()
        }
        assertTrue(saved.await(5, TimeUnit.SECONDS))
        instrumentation.runOnMainSync { app.component.commandRunner.removeListener(listener) }
        assertEquals(2750, habit.originalEntries.get(getToday()).value)
        assertEquals("Numeric draft", habit.originalEntries.get(getToday()).notes)
    }

    private fun capture(name: String) {
        val directory = app.getExternalFilesDir("oneui-review")!!
        directory.mkdirs()
        device.takeScreenshot(File(directory, "$name.png"))
    }
}
