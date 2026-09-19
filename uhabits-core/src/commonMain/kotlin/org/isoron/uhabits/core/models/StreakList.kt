/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.models

import org.isoron.platform.Synchronized
import org.isoron.platform.time.LocalDate
import kotlin.math.min

class StreakList {
    private val list = ArrayList<Streak>()

    @Synchronized
    fun getBest(limit: Int): List<Streak> {
        list.sortWith { s1: Streak, s2: Streak -> s2.compareLonger(s1) }
        return list.subList(0, min(list.size, limit)).apply {
            sortWith { s1: Streak, s2: Streak -> s2.compareNewer(s1) }
        }.toList()
    }

    @Synchronized
    fun recompute(
        computedEntries: EntryList,
        from: LocalDate,
        to: LocalDate,
        isNumerical: Boolean,
        targetValue: Double,
        targetType: NumericalHabitType
    ) {
        list.clear()
        var start: LocalDate? = null
        var end: LocalDate? = null
        var trailingSkip: LocalDate? = null
        var completedDays = 0

        fun finishStreak() {
            val streakStart = start ?: return
            list.add(Streak(streakStart, end!!, completedDays, trailingSkip ?: end!!))
            start = null
            end = null
            trailingSkip = null
            completedDays = 0
        }

        computedEntries.getByInterval(from, to).forEach { entry ->
            val complete = entry.value != Entry.SKIP && if (isNumerical) {
                when (targetType) {
                    NumericalHabitType.AT_LEAST -> entry.value / 1000.0 >= targetValue
                    NumericalHabitType.AT_MOST ->
                        entry.value != Entry.UNKNOWN && entry.value / 1000.0 <= targetValue
                }
            } else {
                entry.value > 0
            }

            when {
                complete -> {
                    if (start == null) {
                        start = entry.date
                        end = entry.date
                    } else {
                        start = entry.date
                    }
                    completedDays++
                }
                entry.value == Entry.SKIP -> {
                    // A skipped day bridges completed days, but is not itself a completed day.
                    if (start == null && trailingSkip == null) trailingSkip = entry.date
                }
                else -> finishStreak()
            }
        }
        finishStreak()
    }
}
