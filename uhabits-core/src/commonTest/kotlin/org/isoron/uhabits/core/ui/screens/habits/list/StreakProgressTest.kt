package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.ui.screens.habits.show.views.BarCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.HistoryCardPresenter
import org.isoron.uhabits.core.ui.views.HistoryChart
import org.isoron.uhabits.core.ui.views.LightTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreakProgressTest : BaseUnitTest() {
    @Test
    fun globalSkipCountingJoinsPerfectStreaksWithoutChangingHabitRules() {
        val habit = fixtures.createEmptyHabit()
        val other = fixtures.createEmptyHabit()
        val today = getToday()
        for (offset in 0..2) {
            habit.computedEntries.add(Entry(today.minus(offset), if (offset == 1) Entry.SKIP else Entry.YES_MANUAL))
            other.computedEntries.add(Entry(today.minus(offset), Entry.YES_AUTO))
        }
        val state = StreakProgress.build(listOf(habit, other), DayOfWeek.MONDAY, countSkips = true)
        assertEquals(3, state.current?.length)
        assertEquals(3, state.longest)
        assertTrue(state.previous.isEmpty())
        assertEquals(2, state.habits.first().current)
        assertEquals(Entry.YES_MANUAL, state.toCardHabit().computedEntries.get(today.minus(1)).value)
        assertFalse(PerfectDays.isComplete(habit, today.minus(1)))

        habit.skipsCountTowardsStreaks = true
        val daily = StreakProgress.build(listOf(habit, other), DayOfWeek.MONDAY)
        assertEquals(3, daily.habits.first().current)
        assertEquals(1, daily.current?.length)
    }

    @Test
    fun countingSkipsStillRequiresEveryActiveHabitToQualify() {
        val today = getToday()
        val skipped = fixtures.createEmptyHabit().apply {
            type = HabitType.NUMERICAL
            targetType = NumericalHabitType.AT_MOST
            targetValue = 2.0
            computedEntries.add(Entry(today, Entry.SKIP))
        }
        val other = fixtures.createEmptyHabit()
        assertFalse(PerfectDays.isPerfect(emptyList(), today, true))
        assertFalse(PerfectDays.isPerfect(listOf(skipped, other), today, true))
        other.computedEntries.add(Entry(today, Entry.NO))
        assertFalse(PerfectDays.isPerfect(listOf(skipped, other), today, true))
        other.computedEntries.add(Entry(today, Entry.SKIP))
        assertTrue(PerfectDays.isPerfect(listOf(skipped, other), today, true))
        assertFalse(PerfectDays.isPerfect(listOf(skipped, other), today))
        skipped.computedEntries.add(Entry(today, 3000))
        assertFalse(PerfectDays.isPerfect(listOf(skipped, other), today, true))
    }

    @Test
    fun sharedCardsReceivePerfectDaysWithoutCreatingSavedHabitData() {
        val habit = fixtures.createEmptyHabit()
        val today = getToday()
        habit.computedEntries.add(Entry(today, Entry.YES_MANUAL))
        habit.computedEntries.add(Entry(today.minus(1), Entry.SKIP))
        habit.computedEntries.add(Entry(today.minus(2), Entry.YES_MANUAL))
        val progress = StreakProgress.build(listOf(habit), DayOfWeek.MONDAY)
        val cardHabit = progress.toCardHabit()
        val history = HistoryCardPresenter.buildState(cardHabit, DayOfWeek.MONDAY, LightTheme())
        val bar = BarCardPresenter.buildState(cardHabit, 1, 0, 0, LightTheme())
        assertEquals(listOf(HistoryChart.Square.ON, HistoryChart.Square.OFF, HistoryChart.Square.ON), history.series)
        assertEquals(2000, bar.entries.sumOf { it.value })
        assertEquals(null, cardHabit.id)
        assertTrue(cardHabit.originalEntries.getKnown().isEmpty())
        assertEquals(Entry.SKIP, habit.computedEntries.get(today.minus(1)).value)
    }

    @Test
    fun separatesCurrentAndPreviousStreaksAndIgnoresArchivedHabits() {
        val habit = fixtures.createEmptyHabit()
        val archived = fixtures.createEmptyHabit().apply { isArchived = true }
        val today = getToday()
        listOf(1, 2, 4, 5, 6).forEach { habit.computedEntries.add(Entry(today.minus(it), Entry.YES_MANUAL)) }
        val state = StreakProgress.build(listOf(habit, archived), DayOfWeek.MONDAY)
        assertEquals(2, state.current?.length)
        assertEquals(3, state.longest)
        assertEquals(5, state.perfectDays.size)
        assertEquals(listOf(3), state.previous.map { it.length })
        assertEquals(1, state.habits.size)
        assertEquals(2, state.habits.single().current)
    }

    @Test
    fun skipBridgesHabitStreakButBreaksPerfectStreak() {
        val habit = fixtures.createEmptyHabit()
        val today = getToday()
        habit.computedEntries.add(Entry(today, Entry.YES_MANUAL))
        habit.computedEntries.add(Entry(today.minus(1), Entry.SKIP))
        habit.computedEntries.add(Entry(today.minus(2), Entry.YES_MANUAL))
        val state = StreakProgress.build(listOf(habit), DayOfWeek.SUNDAY)
        assertEquals(1, state.current?.length)
        assertEquals(1, state.previous.single().length)
        assertEquals(2, state.habits.single().current)
        assertEquals(2, state.habits.single().longest)
        assertFalse(today.minus(1) in state.perfectDays)
        habit.skipsCountTowardsStreaks = true
        assertEquals(3, StreakProgress.build(listOf(habit), DayOfWeek.SUNDAY).habits.single().longest)
    }

    @Test
    fun atMostRequiresAnActualEntryAndAllActiveHabits() {
        val habit = fixtures.createEmptyHabit().apply {
            type = HabitType.NUMERICAL
            targetType = NumericalHabitType.AT_MOST
            targetValue = 2.0
        }
        val other = fixtures.createEmptyHabit()
        val today = getToday()
        assertFalse(PerfectDays.isComplete(habit, today))
        habit.computedEntries.add(Entry(today, 0))
        assertTrue(PerfectDays.isComplete(habit, today))
        assertFalse(PerfectDays.isPerfect(listOf(habit, other), today))
        other.computedEntries.add(Entry(today, Entry.YES_AUTO))
        assertEquals(1, StreakProgress.build(listOf(habit, other), DayOfWeek.MONDAY).perfectDays.size)
        habit.computedEntries.add(Entry(today, Entry.SKIP))
        assertFalse(PerfectDays.isComplete(habit, today))
    }

    @Test
    fun emptyAndBrokenHistoriesHaveNoCurrentStreak() {
        assertTrue(StreakProgress.build(emptyList(), DayOfWeek.MONDAY).perfectDays.isEmpty())
        val habit = fixtures.createEmptyHabit()
        habit.computedEntries.add(Entry(getToday().minus(3), Entry.YES_MANUAL))
        val state = StreakProgress.build(listOf(habit), DayOfWeek.MONDAY)
        assertEquals(null, state.current)
        assertEquals(1, state.previous.single().length)
    }
}
