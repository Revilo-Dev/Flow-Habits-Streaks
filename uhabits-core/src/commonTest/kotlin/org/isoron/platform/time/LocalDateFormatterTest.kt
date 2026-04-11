package org.isoron.platform.time

import org.isoron.platform.io.createTestDateFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateFormatterTest {
    private val formatter = createTestDateFormatter()

    @Test
    fun testShortWeekdayName() {
        assertEquals("Sat", formatter.shortWeekdayName(DayOfWeek.SATURDAY))
        assertEquals("Sun", formatter.shortWeekdayName(DayOfWeek.SUNDAY))
        assertEquals("Mon", formatter.shortWeekdayName(DayOfWeek.MONDAY))
        assertEquals("Tue", formatter.shortWeekdayName(DayOfWeek.TUESDAY))
        assertEquals("Wed", formatter.shortWeekdayName(DayOfWeek.WEDNESDAY))
        assertEquals("Thu", formatter.shortWeekdayName(DayOfWeek.THURSDAY))
        assertEquals("Fri", formatter.shortWeekdayName(DayOfWeek.FRIDAY))
    }

    @Test
    fun testLongWeekdayName() {
        assertEquals("Saturday", formatter.longWeekdayName(DayOfWeek.SATURDAY))
        assertEquals("Sunday", formatter.longWeekdayName(DayOfWeek.SUNDAY))
        assertEquals("Monday", formatter.longWeekdayName(DayOfWeek.MONDAY))
        assertEquals("Tuesday", formatter.longWeekdayName(DayOfWeek.TUESDAY))
        assertEquals("Wednesday", formatter.longWeekdayName(DayOfWeek.WEDNESDAY))
        assertEquals("Thursday", formatter.longWeekdayName(DayOfWeek.THURSDAY))
        assertEquals("Friday", formatter.longWeekdayName(DayOfWeek.FRIDAY))
    }

    @Test
    fun testShortWeekdayNameFromDate() {
        // 2015-01-25 is a Sunday
        assertEquals("Sun", formatter.shortWeekdayName(LocalDate(2015, 1, 25)))
        // 2015-01-26 is a Monday
        assertEquals("Mon", formatter.shortWeekdayName(LocalDate(2015, 1, 26)))
    }

    @Test
    fun testShortMonthName() {
        assertEquals("Jan", formatter.shortMonthName(LocalDate(2015, 1, 1)))
        assertEquals("Feb", formatter.shortMonthName(LocalDate(2015, 2, 1)))
        assertEquals("Dec", formatter.shortMonthName(LocalDate(2015, 12, 1)))
    }

    @Test
    fun testLongMonthName() {
        assertEquals("January", formatter.longMonthName(LocalDate(2015, 1, 1)))
        assertEquals("February", formatter.longMonthName(LocalDate(2015, 2, 1)))
        assertEquals("December", formatter.longMonthName(LocalDate(2015, 12, 1)))
    }
}
