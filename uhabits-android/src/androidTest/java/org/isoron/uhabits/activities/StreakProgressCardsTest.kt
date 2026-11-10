package org.isoron.uhabits.activities

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.isoron.platform.gui.toInt
import org.isoron.platform.time.DayOfWeek
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.memory.MemoryModelFactory
import org.isoron.uhabits.core.ui.screens.habits.show.views.HistoryCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.OverviewCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.StreakCardState
import org.isoron.uhabits.core.ui.views.LightTheme
import org.isoron.uhabits.databinding.StreakProgressBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreakProgressCardsTest {
    @Test
    fun sharedCardIconsFollowHabitColourAndTheSummaryOmitsScores() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.FlowTheme_Light)
            val binding = StreakProgressBinding.inflate(LayoutInflater.from(context))
            val theme = LightTheme()
            val habit = MemoryModelFactory().buildHabit().apply { color = PaletteColor(3) }
            val color = theme.color(habit.color).toInt()
            binding.overviewCard.setState(OverviewCardPresenter.buildState(habit, theme))
            binding.historyCard.setState(HistoryCardPresenter.buildState(habit, DayOfWeek.MONDAY, theme))
            binding.bestStreakCard.setState(StreakCardState(habit.color, emptyList(), theme))
            listOf(binding.overviewCard, binding.historyCard, binding.bestStreakCard).forEach { card ->
                assertEquals(color, card.findViewById<TextView>(R.id.title).compoundDrawableTintList!!.defaultColor)
            }
            binding.overviewCard.setStreakSummary(3, 9, 20, color)
            assertEquals(View.GONE, binding.overviewCard.findViewById<View>(R.id.scoreRingContainer).visibility)
            assertEquals("3", binding.overviewCard.findViewById<TextView>(R.id.monthDiffLabel).text)
            binding.historyCard.setReadOnly(R.string.flow_streak_calendar)
            assertEquals(View.GONE, binding.historyCard.findViewById<View>(R.id.edit).visibility)
            assertNull(binding.root.findViewById<View>(R.id.scoreCard))
            assertNull(binding.root.findViewById<View>(R.id.frequencyCard))
        }
    }
}
