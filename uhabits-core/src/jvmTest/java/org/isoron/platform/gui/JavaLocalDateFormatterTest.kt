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
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.platform.time.getFirstWeekdayNumberAccordingToLocale
import org.junit.Test
import java.util.Locale
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class JavaLocalDateFormatterTest {

    @Test
    fun testShortWeekdayNames_us() {
        val formatter = JavaLocalDateFormatter(Locale.US)
        assertContentEquals(
            arrayOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri"),
            formatter.shortWeekdayNames(DayOfWeek.SATURDAY)
        )
    }

    @Test
    fun testShortWeekdayNames_germany() {
        val formatter = JavaLocalDateFormatter(Locale.GERMANY)
        assertContentEquals(
            arrayOf("Sa.", "So.", "Mo.", "Di.", "Mi.", "Do.", "Fr."),
            formatter.shortWeekdayNames(DayOfWeek.SATURDAY)
        )
    }

    @Test
    fun testLongWeekdayNames_us() {
        val formatter = JavaLocalDateFormatter(Locale.US)
        assertContentEquals(
            arrayOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"),
            formatter.longWeekdayNames(DayOfWeek.SATURDAY)
        )
    }

    @Test
    fun testLongWeekdayNames_germany() {
        val formatter = JavaLocalDateFormatter(Locale.GERMANY)
        assertContentEquals(
            arrayOf("Samstag", "Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag"),
            formatter.longWeekdayNames(DayOfWeek.SATURDAY)
        )
    }

    @Test
    fun testGetFirstWeekdayNumberAccordingToLocale_us() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals(1, getFirstWeekdayNumberAccordingToLocale())
        } finally {
            Locale.setDefault(saved)
        }
    }

    @Test
    fun testGetFirstWeekdayNumberAccordingToLocale_germany() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals(2, getFirstWeekdayNumberAccordingToLocale())
        } finally {
            Locale.setDefault(saved)
        }
    }
}
