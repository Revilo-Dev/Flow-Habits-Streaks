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

package org.isoron.platform.gui

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.computeToday
import org.isoron.platform.time.countWeekdayOccurrencesInMonth
import org.isoron.platform.time.getToday
import org.isoron.platform.time.getWeekdaySequence
import org.isoron.platform.time.resetToday
import org.isoron.platform.time.setToday
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatesTest {
    @Test
    fun testDatesBefore2000() {
        val date = LocalDate(-1)
        assertEquals(31, date.day)
        assertEquals(12, date.month)
        assertEquals(1999, date.year)
    }

    @Test
    fun testDayOfWeekBefore2000() {
        // 1999-12-31 (daysSince2000 = -1) was a Friday
        assertEquals(DayOfWeek.FRIDAY, LocalDate(1999, 12, 31).dayOfWeek)
        // 1999-12-30 (daysSince2000 = -2) was a Thursday
        assertEquals(DayOfWeek.THURSDAY, LocalDate(1999, 12, 30).dayOfWeek)
        // 1999-12-25 (Saturday)
        assertEquals(DayOfWeek.SATURDAY, LocalDate(1999, 12, 25).dayOfWeek)
        // 1999-12-26 (Sunday)
        assertEquals(DayOfWeek.SUNDAY, LocalDate(1999, 12, 26).dayOfWeek)
    }

    @Test
    fun testFromUnixTimeBefore2000() {
        val epoch2000 = 946684800000L
        val msPerDay = 86400000L
        // One millisecond before 2000-01-01 should be 1999-12-31
        val justBefore2000 = LocalDate.fromUnixTime(epoch2000 - 1)
        assertEquals(LocalDate(1999, 12, 31), justBefore2000)
        // One full day before 2000-01-01 should be 1999-12-31
        val oneDayBefore = LocalDate.fromUnixTime(epoch2000 - msPerDay)
        assertEquals(LocalDate(1999, 12, 31), oneDayBefore)
        // One full day + 1ms before should be 1999-12-30
        val oneDayAndOneMsBefore = LocalDate.fromUnixTime(epoch2000 - msPerDay - 1)
        assertEquals(LocalDate(1999, 12, 30), oneDayAndOneMsBefore)
    }

    @Test
    fun testGetWeekdaySequence() {
        val seq = getWeekdaySequence(DayOfWeek.TUESDAY)
        assertEquals(
            listOf(
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY
            ),
            seq
        )
        val seqSun = getWeekdaySequence(DayOfWeek.SUNDAY)
        assertEquals(
            listOf(
                DayOfWeek.SUNDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY
            ),
            seqSun
        )
    }

    @Test
    fun testCountWeekdayOccurrencesInMonth() {
        // February 2018 (28 days, starts on Thursday)
        assertContentEquals(
            arrayOf(4, 4, 4, 4, 4, 4, 4),
            countWeekdayOccurrencesInMonth(LocalDate(2018, 2, 1))
        )
        // February 2020 (leap, 29 days, starts on Saturday)
        assertContentEquals(
            arrayOf(5, 4, 4, 4, 4, 4, 4),
            countWeekdayOccurrencesInMonth(LocalDate(2020, 2, 1))
        )
        // April 2020 (30 days, starts on Wednesday)
        assertContentEquals(
            arrayOf(4, 4, 4, 4, 5, 5, 4),
            countWeekdayOccurrencesInMonth(LocalDate(2020, 4, 1))
        )
        // August 2020 (31 days, starts on Saturday)
        assertContentEquals(
            arrayOf(5, 5, 5, 4, 4, 4, 4),
            countWeekdayOccurrencesInMonth(LocalDate(2020, 8, 1))
        )
    }

    @Test
    fun testStartOfWeek() {
        // Wednesday 2015-01-28
        val wed = LocalDate(2015, 1, 28)

        // firstWeekday = SUNDAY -> start is Sunday 2015-01-25
        assertEquals(LocalDate(2015, 1, 25), wed.startOfWeek(DayOfWeek.SUNDAY))

        // firstWeekday = MONDAY -> start is Monday 2015-01-26
        assertEquals(LocalDate(2015, 1, 26), wed.startOfWeek(DayOfWeek.MONDAY))

        // firstWeekday = WEDNESDAY -> start is Wednesday 2015-01-28 (same day)
        assertEquals(LocalDate(2015, 1, 28), wed.startOfWeek(DayOfWeek.WEDNESDAY))

        // firstWeekday = SATURDAY -> start is Saturday 2015-01-24
        assertEquals(LocalDate(2015, 1, 24), wed.startOfWeek(DayOfWeek.SATURDAY))
    }

    @Test
    fun testStartOfMonth() {
        assertEquals(LocalDate(2015, 1, 1), LocalDate(2015, 1, 25).startOfMonth())
        assertEquals(LocalDate(2015, 2, 1), LocalDate(2015, 2, 28).startOfMonth())
        assertEquals(LocalDate(2024, 2, 1), LocalDate(2024, 2, 29).startOfMonth())
    }

    @Test
    fun testStartOfQuarter() {
        assertEquals(LocalDate(2015, 1, 1), LocalDate(2015, 1, 15).startOfQuarter())
        assertEquals(LocalDate(2015, 1, 1), LocalDate(2015, 3, 31).startOfQuarter())
        assertEquals(LocalDate(2015, 4, 1), LocalDate(2015, 4, 1).startOfQuarter())
        assertEquals(LocalDate(2015, 4, 1), LocalDate(2015, 6, 15).startOfQuarter())
        assertEquals(LocalDate(2015, 7, 1), LocalDate(2015, 9, 30).startOfQuarter())
        assertEquals(LocalDate(2015, 10, 1), LocalDate(2015, 12, 31).startOfQuarter())
    }

    @Test
    fun testStartOfYear() {
        assertEquals(LocalDate(2015, 1, 1), LocalDate(2015, 6, 15).startOfYear())
        assertEquals(LocalDate(2024, 1, 1), LocalDate(2024, 12, 31).startOfYear())
    }

    @Test
    fun testToCSVString() {
        assertEquals("2015-01-25", LocalDate(2015, 1, 25).toCSVString())
        assertEquals("2024-02-29", LocalDate(2024, 2, 29).toCSVString())
        assertEquals("1999-12-31", LocalDate(1999, 12, 31).toCSVString())
    }

    @Test
    fun testComparisons() {
        val jan1 = LocalDate(2015, 1, 1)
        val jan2 = LocalDate(2015, 1, 2)
        val jan1b = LocalDate(2015, 1, 1)

        assertTrue(jan1 < jan2)
        assertTrue(jan2 > jan1)
        assertTrue(jan1 <= jan1b)
        assertTrue(jan1 >= jan1b)
        assertEquals(jan1, jan1b)
    }

    @Test
    fun testGetTodayBeforeSetTodayThrows() {
        resetToday()
        try {
            assertFailsWith<IllegalStateException> { getToday() }
        } finally {
            setToday(LocalDate(2015, 1, 25))
        }
    }

    @Test
    fun testSetTodayUpdatable() {
        setToday(LocalDate(2015, 1, 25))
        assertEquals(LocalDate(2015, 1, 25), getToday())
        setToday(LocalDate(2020, 6, 15))
        assertEquals(LocalDate(2020, 6, 15), getToday())
    }

    @Test
    fun testComputeTodayReturnsSensibleDate() {
        val today = computeToday()
        // Should be somewhere around the current date (after 2020, before 2100)
        assertTrue(today.year >= 2020)
        assertTrue(today.year <= 2100)
    }
}
