package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.NumericalHabitType

object PerfectDays {
    fun isPerfect(habits: List<Habit>, date: LocalDate, countSkips: Boolean = false): Boolean =
        habits.isNotEmpty() && habits.all {
            isComplete(it, date) || (countSkips && it.computedEntries.get(date).value == Entry.SKIP)
        }

    fun isComplete(habit: Habit, date: LocalDate): Boolean {
        val value = habit.computedEntries.get(date).value
        // Counting skips towards perfect streaks does not change completion totals
        if (value == Entry.SKIP || value == Entry.UNKNOWN) return false
        if (!habit.isNumerical) return value == Entry.YES_MANUAL || value == Entry.YES_AUTO
        return when (habit.targetType) {
            NumericalHabitType.AT_LEAST -> value / 1000.0 >= habit.targetValue
            NumericalHabitType.AT_MOST -> value / 1000.0 <= habit.targetValue
        }
    }
}
