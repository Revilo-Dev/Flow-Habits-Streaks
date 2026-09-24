package org.isoron.uhabits.core.ui.screens.habits.show.views

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.NumericalHabitType
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrentStreakCardPresenterTest : BaseUnitTest() {
    @Test
    fun skippedNumericalDaysDoNotBreakOrIncreaseCurrentStreak() {
        val today = getToday()
        val habit = fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_LEAST)
        habit.originalEntries.add(Entry(today, 2000))
        habit.originalEntries.add(Entry(today.minus(1), Entry.SKIP))
        habit.originalEntries.add(Entry(today.minus(2), 2000))
        habit.originalEntries.add(Entry(today.minus(3), Entry.SKIP))
        habit.originalEntries.add(Entry(today.minus(4), 2000))
        habit.recompute()

        val state = CurrentStreakCardPresenter.buildState(habit, DayOfWeek.MONDAY)

        assertEquals(3, state.currentStreak)
    }

    @Test
    fun failedDayAfterASkipResetsTheCurrentStreak() {
        val today = getToday()
        val habit = fixtures.createEmptyHabit()
        habit.originalEntries.add(Entry(today, Entry.NO))
        habit.originalEntries.add(Entry(today.minus(1), Entry.SKIP))
        habit.originalEntries.add(Entry(today.minus(2), Entry.NO))
        habit.originalEntries.add(Entry(today.minus(3), Entry.NO))
        habit.originalEntries.add(Entry(today.minus(4), Entry.YES_MANUAL))
        habit.originalEntries.add(Entry(today.minus(5), Entry.YES_MANUAL))
        habit.recompute()

        val state = CurrentStreakCardPresenter.buildState(habit, DayOfWeek.MONDAY)

        assertEquals(0, state.currentStreak)
    }
}
