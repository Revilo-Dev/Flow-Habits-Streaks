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
import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.TruncateField
import org.isoron.uhabits.core.models.Entry.Companion.SKIP
import org.isoron.uhabits.core.models.Entry.Companion.UNKNOWN
import org.isoron.uhabits.core.models.Entry.Companion.YES_AUTO
import org.isoron.uhabits.core.models.Entry.Companion.YES_MANUAL
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min

open class EntryList {

    private val entriesByDate: HashMap<LocalDate, Entry> = HashMap()

    // Return an unknown answer when no entry exists for the date
    @Synchronized
    open fun get(date: LocalDate): Entry {
        return entriesByDate[date] ?: Entry(date, UNKNOWN)
    }

    // Include both endpoints and return entries with the newest date first
    @Synchronized
    open fun getByInterval(from: LocalDate, to: LocalDate): List<Entry> {
        val result = mutableListOf<Entry>()
        if (from.isNewerThan(to)) return result
        var current = to
        while (current >= from) {
            result.add(get(current))
            current = current.minus(1)
        }
        return result
    }

    // Replace any existing answer for the same date
    @Synchronized
    open fun add(entry: Entry) {
        entriesByDate[entry.date] = entry
    }

    // Return the entries held in memory with the newest date first
    @Synchronized
    open fun getKnown(): List<Entry> {
        return entriesByDate.values.sortedBy { it.date }.reversed()
    }

    // Copy numerical answers or derive automatic boolean completions from the schedule
    @Synchronized
    open fun recomputeFrom(
        originalEntries: EntryList,
        frequency: Frequency,
        isNumerical: Boolean
    ) {
        clear()
        val original = originalEntries.getKnown()
        if (isNumerical) {
            original.forEach { add(it) }
        } else {
            val intervals = buildIntervals(frequency, original)
            snapIntervalsTogether(intervals)
            val computed = buildEntriesFromInterval(original, intervals)
            computed.filter { it.value != UNKNOWN || it.notes.isNotEmpty() }.forEach { add(it) }
        }
    }

    // Discard the entries held in this list
    @Synchronized
    open fun clear() {
        entriesByDate.clear()
    }

    // Group monthly totals into weekday slots starting with Saturday
    @Synchronized
    fun computeWeekdayFrequency(isNumerical: Boolean): HashMap<LocalDate, Array<Int>> {
        val entries = getKnown()
        val map = hashMapOf<LocalDate, Array<Int>>()
        for (entry in entries) {
            val weekday = (entry.date.dayOfWeek.daysSinceSunday + 1) % 7
            val monthStart = entry.date.startOfMonth()

            var list = map[monthStart]
            if (list == null) {
                list = arrayOf(0, 0, 0, 0, 0, 0, 0)
                map[monthStart] = list
            }

            if (isNumerical) {
                list[weekday] += entry.value
            } else if (entry.value == YES_MANUAL) {
                list[weekday] += 1
            }
        }
        return map
    }

    data class Interval(val begin: LocalDate, val center: LocalDate, val end: LocalDate) {
        val length: Int
            get() = begin.daysUntil(end) + 1
    }

    companion object {
        // Fill schedule intervals before applying original completions skips and notes
        fun buildEntriesFromInterval(
            original: List<Entry>,
            intervals: List<Interval>
        ): List<Entry> {
            val result = arrayListOf<Entry>()
            if (original.isEmpty()) return result

            var from = original[0].date
            var to = original[0].date

            for (e in original) {
                if (e.date < from) from = e.date
                if (e.date > to) to = e.date
            }
            for (interval in intervals) {
                if (interval.begin < from) from = interval.begin
                if (interval.end > to) to = interval.end
            }

            // Create unknown entries
            var current = to
            while (current >= from) {
                result.add(Entry(current, UNKNOWN))
                current = current.minus(1)
            }

            // Create YES_AUTO entries
            intervals.forEach { interval ->
                current = interval.end
                while (current >= interval.begin) {
                    val offset = current.daysUntil(to)
                    result[offset] = Entry(current, YES_AUTO)
                    current = current.minus(1)
                }
            }

            // Copy original entries
            original.forEach { entry ->
                val offset = entry.date.daysUntil(to)
                val value = if (
                    result[offset].value == UNKNOWN ||
                    entry.value == SKIP ||
                    entry.value == YES_MANUAL
                ) {
                    entry.value
                } else {
                    YES_AUTO
                }
                result[offset] = Entry(entry.date, value, entry.notes)
            }

            return result
        }

        // Slide older intervals backwards to close gaps without passing their completed dates
        fun snapIntervalsTogether(intervals: ArrayList<Interval>) {
            for (i in 1 until intervals.size) {
                val curr = intervals[i]
                val next = intervals[i - 1]
                val gapNextToCurrent = next.begin.daysUntil(curr.end)
                val gapCenterToEnd = curr.center.daysUntil(curr.end)
                if (gapNextToCurrent >= 0) {
                    val shift = min(gapCenterToEnd, gapNextToCurrent + 1)
                    intervals[i] = Interval(
                        curr.begin.minus(shift),
                        curr.center,
                        curr.end.minus(shift)
                    )
                }
            }
        }

        // Build covered date ranges when enough manual completions meet the schedule
        fun buildIntervals(
            freq: Frequency,
            entries: List<Entry>
        ): ArrayList<Interval> {
            val filtered = entries.filter { it.value == YES_MANUAL }
            val num = freq.numerator
            val den = freq.denominator
            val intervals = arrayListOf<Interval>()
            for (i in num - 1 until filtered.size) {
                val begin = filtered[i].date
                val center = filtered[i - num + 1].date
                var size = den
                // Monthly schedules use the actual month length when building covered ranges
                if (den == 30 || den == 31) {
                    size = if (begin.day == begin.monthLength) {
                        begin.plus(1).monthLength
                    } else {
                        begin.monthLength
                    }
                }
                if (begin.daysUntil(center) < size) {
                    val end = begin.plus(size - 1)
                    intervals.add(Interval(begin, center, end))
                }
            }
            return intervals
        }
    }
}

private fun truncateDate(
    date: LocalDate,
    field: TruncateField,
    firstWeekday: DayOfWeek
): LocalDate {
    return when (field) {
        TruncateField.DAY -> date
        TruncateField.WEEK_NUMBER -> date.startOfWeek(firstWeekday)
        TruncateField.MONTH -> date.startOfMonth()
        TruncateField.QUARTER -> date.startOfQuarter()
        TruncateField.YEAR -> date.startOfYear()
    }
}

// Sum numerical values or manual completions by period with newest periods first
fun List<Entry>.groupedSum(
    truncateField: TruncateField,
    firstWeekday: Int = 7,
    isNumerical: Boolean
): List<Entry> {
    val firstWeekdayEnum = DayOfWeek.values()[firstWeekday - 1]
    return this.map { (date, value) ->
        if (isNumerical) {
            if (value == SKIP) {
                Entry(date, 0)
            } else {
                Entry(date, max(0, value))
            }
        } else {
            Entry(date, if (value == YES_MANUAL) 1000 else 0)
        }
    }.groupBy { entry ->
        truncateDate(entry.date, truncateField, firstWeekdayEnum)
    }.entries.map { (date, entries) ->
        Entry(date, entries.sumOf { it.value })
    }.sortedBy { (date, _) ->
        -date.daysSince2000
    }
}

// Count skipped days by period so charts can account for paused days
fun List<Entry>.countSkippedDays(
    truncateField: TruncateField,
    firstWeekday: Int = 7
): List<Entry> {
    val firstWeekdayEnum = DayOfWeek.values()[firstWeekday - 1]
    return this.map { (date, value) ->
        if (value == SKIP) {
            Entry(date, 1)
        } else {
            Entry(date, 0)
        }
    }.groupBy { entry ->
        truncateDate(entry.date, truncateField, firstWeekdayEnum)
    }.entries.map { (date, entries) ->
        Entry(date, entries.sumOf { it.value })
    }.sortedBy { (date, _) ->
        -date.daysSince2000
    }
}
