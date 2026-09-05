/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 */

package org.isoron.uhabits.core.ui.screens.habits.show.views

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.NumericalHabitType

data class CurrentStreakCardState(
    val currentStreak: Int,
    val lastSevenDays: List<Boolean>,
    val firstWeekday: DayOfWeek
) {
    val todayComplete: Boolean
        get() = lastSevenDays.lastOrNull() == true
}

object CurrentStreakCardPresenter {
    fun buildState(
        habit: Habit,
        firstWeekday: DayOfWeek
    ): CurrentStreakCardState {
        val today = getToday()
        val lastSevenDays = (6 downTo 0).map { offset ->
            isComplete(habit, today.minus(offset))
        }
        val todayComplete = lastSevenDays.last()
        var currentStreak = 0
        var date = today.minus(if (todayComplete) 0 else 1)
        while (isComplete(habit, date)) {
            currentStreak++
            date = date.minus(1)
        }
        return CurrentStreakCardState(
            currentStreak = currentStreak,
            lastSevenDays = lastSevenDays,
            firstWeekday = firstWeekday
        )
    }

    private fun isComplete(habit: Habit, date: LocalDate): Boolean {
        val value = habit.computedEntries.get(date).value
        if (!habit.isNumerical) {
            return value == Entry.YES_MANUAL || value == Entry.YES_AUTO
        }
        if (value == Entry.UNKNOWN) return false
        val enteredValue = value / 1000.0
        return when (habit.targetType) {
            NumericalHabitType.AT_LEAST -> enteredValue >= habit.targetValue
            NumericalHabitType.AT_MOST -> enteredValue <= habit.targetValue
        }
    }
}
