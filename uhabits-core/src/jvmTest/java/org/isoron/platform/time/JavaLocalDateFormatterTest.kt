package org.isoron.platform.time

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaLocalDateFormatterTest {
    private val formatter = JavaLocalDateFormatter(Locale.US)

    @Test
    fun testLongFormat() {
        assertEquals("Dec 1, 2020", formatter.longFormat(LocalDate(2020, 12, 1)))
        assertEquals("Jan 15, 2015", formatter.longFormat(LocalDate(2015, 1, 15)))
        assertEquals("Feb 28, 2000", formatter.longFormat(LocalDate(2000, 2, 28)))
    }

    @Test
    fun testShortWeekdayNames() {
        val names = formatter.shortWeekdayNames(DayOfWeek.SUNDAY)
        assertEquals(7, names.size)
        assertEquals("Sun", names[0])
        assertEquals("Mon", names[1])
        assertEquals("Tue", names[2])
        assertEquals("Wed", names[3])
        assertEquals("Thu", names[4])
        assertEquals("Fri", names[5])
        assertEquals("Sat", names[6])
    }

    @Test
    fun testShortWeekdayNames_startingMonday() {
        val names = formatter.shortWeekdayNames(DayOfWeek.MONDAY)
        assertEquals("Mon", names[0])
        assertEquals("Sun", names[6])
    }

    @Test
    fun testLongWeekdayNames() {
        val names = formatter.longWeekdayNames(DayOfWeek.SUNDAY)
        assertEquals(7, names.size)
        assertEquals("Sunday", names[0])
        assertEquals("Monday", names[1])
        assertEquals("Tuesday", names[2])
        assertEquals("Wednesday", names[3])
        assertEquals("Thursday", names[4])
        assertEquals("Friday", names[5])
        assertEquals("Saturday", names[6])
    }

    @Test
    fun testLongWeekdayNames_startingSaturday() {
        val names = formatter.longWeekdayNames(DayOfWeek.SATURDAY)
        assertEquals("Saturday", names[0])
        assertEquals("Friday", names[6])
    }
}
