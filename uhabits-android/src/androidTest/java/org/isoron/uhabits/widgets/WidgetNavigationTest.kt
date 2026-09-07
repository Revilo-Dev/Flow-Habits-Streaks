package org.isoron.uhabits.widgets

import android.content.ComponentName
import android.content.Intent
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.activities.habits.show.ShowHabitActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetNavigationTest {
    @Test
    fun widgetDetailIntentResolvesParentWithDifferentApplicationId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val detail = ComponentName(context, ShowHabitActivity::class.java)
        val parent = NavUtils.getParentActivityIntent(context, detail)!!
        val expected = ComponentName(context, ListHabitsActivity::class.java)
        assertEquals(expected, parent.component)
        context.packageManager.getActivityInfo(parent.component!!, 0)
        val stack = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(Intent().setComponent(detail))
        assertEquals(2, stack.intentCount)
        assertEquals(expected, stack.editIntentAt(0)!!.component)
    }
}
