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
    fun skipSettingsIndependentlyControlWeekAndStreak() {
        for (numerical in listOf(false, true)) {
            val habit = if (numerical) {
                fixtures.createEmptyNumericalHabit(NumericalHabitType.AT_MOST)
            } else {
                fixtures.createEmptyHabit()
            }
            val today = getToday()
            habit.originalEntries.add(Entry(today, Entry.SKIP))
            habit.originalEntries.add(Entry(today.minus(1), Entry.SKIP))
            habit.originalEntries.add(Entry(today.minus(2), if (numerical) 0 else Entry.YES_MANUAL))
            for (weekly in listOf(false, true)) {
                for (streak in listOf(false, true)) {
                    habit.skipsCountTowardsWeeklyStreak = weekly
                    habit.skipsCountTowardsStreaks = streak
                    habit.recompute()
                    val state = CurrentStreakCardPresenter.buildState(habit, DayOfWeek.MONDAY)
                    assertEquals(if (streak) 3 else 1, state.currentStreak)
                    assertEquals(weekly, state.lastSevenDays.last())
                    val derived = habit.streaks.getBest(1).single()
                    assertEquals(state.currentStreak, derived.length)
                    assertEquals(today, derived.lastRelevantDate)
                }
            }
        }
    }

    @Test
    fun enabledSkipCountingCanStartAStreakWithoutACompletion() {
        val habit = fixtures.createEmptyHabit()
        habit.skipsCountTowardsStreaks = true
        habit.originalEntries.add(Entry(getToday(), Entry.SKIP))
        habit.originalEntries.add(Entry(getToday().minus(1), Entry.SKIP))
        habit.recompute()
        assertEquals(2, habit.streaks.getBest(1).single().length)
        assertEquals(2, CurrentStreakCardPresenter.buildState(habit, DayOfWeek.MONDAY).currentStreak)
    }

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
