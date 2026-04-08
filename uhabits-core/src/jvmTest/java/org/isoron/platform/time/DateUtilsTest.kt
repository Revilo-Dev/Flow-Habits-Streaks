package org.isoron.platform.time

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.test.assertEquals

class DateUtilsTest {
    companion object {
        const val HOUR_OFFSET = 3
        const val HOURS_IN_ONE_DAY = 24
    }

    @Before
    fun setUp() {
        DateUtils.setFixedTimeZone(null)
        DateUtils.setFixedLocalTime(null)
    }

    @After
    fun tearDown() {
        DateUtils.setFixedLocalTime(null)
        DateUtils.setFixedTimeZone(null)
    }

    // ---------------------------------------------------------------
    // getLocalTime
    // ---------------------------------------------------------------

    @Test
    fun testGetLocalTime() {
        DateUtils.setFixedLocalTime(null)
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("Australia/Sydney"))
        val utcTime = unixTime(2015, Calendar.JANUARY, 11)
        val localTime = DateUtils.getLocalTime(utcTimeInMillis = utcTime)
        val expectedOffset = 11 * 60 * 60 * 1000L
        assertEquals(utcTime + expectedOffset, localTime)
    }

    // ---------------------------------------------------------------
    // getStartOfDay / getStartOfDayWithOffset
    // ---------------------------------------------------------------

    @Test
    fun testGetStartOfDay() {
        val startOfDay = unixTime(2017, Calendar.JANUARY, 1)
        val laterInDay = unixTime(2017, Calendar.JANUARY, 1, 20, 0)
        assertEquals(startOfDay, DateUtils.getStartOfDay(laterInDay))
    }

    @Test
    fun testGetStartOfDayWithOffset_noOffset() {
        val timestamp = unixTime(2020, Calendar.SEPTEMBER, 3)
        assertEquals(
            timestamp,
            DateUtils.getStartOfDayWithOffset(timestamp + DateUtils.HOUR_LENGTH, 0, 0)
        )
    }

    @Test
    fun testGetStartOfDayWithOffset_withOffset() {
        val timestamp = unixTime(2020, Calendar.SEPTEMBER, 3)
        // 3:29 AM on Sep 3 with a 3:30 offset → still "Sep 2" conceptually
        assertEquals(
            timestamp - DateUtils.DAY_LENGTH,
            DateUtils.getStartOfDayWithOffset(
                timestamp + 3 * DateUtils.HOUR_LENGTH + 29 * DateUtils.MINUTE_LENGTH,
                3,
                30
            )
        )
    }

    // ---------------------------------------------------------------
    // getStartOfTomorrowWithOffset
    // ---------------------------------------------------------------

    @Test
    fun testGetStartOfTomorrowWithOffset_priorToOffset() {
        configureOffsetTest(HOUR_OFFSET - 1)
        assertEquals(
            fixedStartOfTodayWithOffset(HOUR_OFFSET),
            DateUtils.getStartOfTomorrowWithOffset(HOUR_OFFSET, 0)
        )
    }

    @Test
    fun testGetStartOfTomorrowWithOffset_afterOffset() {
        configureOffsetTest(HOUR_OFFSET + 1 - HOURS_IN_ONE_DAY)
        assertEquals(
            fixedStartOfTodayWithOffset(HOUR_OFFSET),
            DateUtils.getStartOfTomorrowWithOffset(HOUR_OFFSET, 0)
        )
    }

    // ---------------------------------------------------------------
    // getUpcomingTimeInMillis
    // ---------------------------------------------------------------

    @Test
    fun testGetUpcomingTimeInMillis() {
        val fixedTime = unixTime(2015, Calendar.JANUARY, 25, 8, 0)
        DateUtils.setFixedLocalTime(fixedTime)
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("GMT"))
        val expected = unixTime(2015, Calendar.JANUARY, 25, 10, 1)
        assertEquals(expected, DateUtils.getUpcomingTimeInMillis(10, 1))
    }

    // ---------------------------------------------------------------
    // millisecondsUntilTomorrowWithOffset
    // ---------------------------------------------------------------

    @Test
    fun testMillisecondsUntilTomorrow_justBeforeMidnight() {
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("GMT"))
        DateUtils.setFixedLocalTime(unixTime(2017, Calendar.JANUARY, 1, 23, 59))
        assertEquals(DateUtils.MINUTE_LENGTH, DateUtils.millisecondsUntilTomorrowWithOffset(0, 0))
    }

    @Test
    fun testMillisecondsUntilTomorrow_earlyEvening() {
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("GMT"))
        DateUtils.setFixedLocalTime(unixTime(2017, Calendar.JANUARY, 1, 20, 0))
        assertEquals(4 * DateUtils.HOUR_LENGTH, DateUtils.millisecondsUntilTomorrowWithOffset(0, 0))
    }

    @Test
    fun testMillisecondsUntilTomorrow_withHourAndMinuteOffset() {
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("GMT"))
        DateUtils.setFixedLocalTime(unixTime(2017, Calendar.JANUARY, 1, 23, 59))
        assertEquals(
            HOUR_OFFSET * DateUtils.HOUR_LENGTH + 31 * DateUtils.MINUTE_LENGTH,
            DateUtils.millisecondsUntilTomorrowWithOffset(HOUR_OFFSET, 30)
        )
    }

    @Test
    fun testMillisecondsUntilTomorrow_afterMidnightBeforeOffset() {
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("GMT"))
        DateUtils.setFixedLocalTime(unixTime(2017, Calendar.JANUARY, 2, 1, 0))
        assertEquals(
            2 * DateUtils.HOUR_LENGTH + 30 * DateUtils.MINUTE_LENGTH,
            DateUtils.millisecondsUntilTomorrowWithOffset(HOUR_OFFSET, 30)
        )
    }

    // ---------------------------------------------------------------
    // applyTimezone (Australia/Sydney, covering DST transitions)
    // ---------------------------------------------------------------

    @Test
    fun testApplyTimezone_sydney() {
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("Australia/Sydney"))
        // Winter (AEST = UTC+10)
        assertEquals(
            unixTime(2017, Calendar.JULY, 30, 8, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.JULY, 30, 18, 0))
        )
        // Before DST starts (still UTC+10)
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 29, 14, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 0, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 0, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 10, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 1, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 11, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 2, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 12, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 3, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 13, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 12, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 22, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 13, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 23, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 14, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 0, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 15, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 1, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 15, 59),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 1, 59))
        )
        // DST begins (clocks spring forward, AEDT = UTC+11)
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 16, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 3, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 17, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 4, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 18, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 5, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 0, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 11, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 1, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 12, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 2, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 13, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 3, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 14, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 4, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 15, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 8, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 1, 19, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 2, 8, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.OCTOBER, 2, 19, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.NOVEMBER, 30, 8, 0),
            DateUtils.applyTimezone(unixTime(2017, Calendar.NOVEMBER, 30, 19, 0))
        )
        // Before DST ends (still AEDT = UTC+11)
        assertEquals(
            unixTime(2018, Calendar.MARCH, 30, 13, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.MARCH, 31, 0, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 1, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.MARCH, 31, 12, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 7, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.MARCH, 31, 18, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 13, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.APRIL, 1, 0, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 14, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.APRIL, 1, 1, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 14, 59),
            DateUtils.applyTimezone(unixTime(2018, Calendar.APRIL, 1, 1, 59))
        )
        // DST ends (clocks fall back, AEST = UTC+10)
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 16, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.APRIL, 1, 2, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 17, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.APRIL, 1, 3, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 18, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.APRIL, 1, 4, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 0, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.APRIL, 1, 10, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 8, 0),
            DateUtils.applyTimezone(unixTime(2018, Calendar.APRIL, 1, 18, 0))
        )
    }

    // ---------------------------------------------------------------
    // removeTimezone (Australia/Sydney, covering DST transitions)
    // ---------------------------------------------------------------

    @Test
    fun testRemoveTimezone_sydney() {
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("Australia/Sydney"))
        // Winter (AEST = UTC+10)
        assertEquals(
            unixTime(2017, Calendar.JULY, 30, 18, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.JULY, 30, 8, 0))
        )
        // Before DST starts
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 0, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 29, 14, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 10, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 0, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 11, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 1, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 12, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 2, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 13, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 3, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 22, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 12, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.SEPTEMBER, 30, 23, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 13, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 0, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 14, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 1, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 15, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 1, 59),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 15, 59))
        )
        // DST begins (AEDT = UTC+11)
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 3, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 16, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 4, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 17, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 5, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.SEPTEMBER, 30, 18, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 11, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.OCTOBER, 1, 0, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 12, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.OCTOBER, 1, 1, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 13, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.OCTOBER, 1, 2, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 14, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.OCTOBER, 1, 3, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 15, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.OCTOBER, 1, 4, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 1, 19, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.OCTOBER, 1, 8, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.OCTOBER, 2, 19, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.OCTOBER, 2, 8, 0))
        )
        assertEquals(
            unixTime(2017, Calendar.NOVEMBER, 30, 19, 0),
            DateUtils.removeTimezone(unixTime(2017, Calendar.NOVEMBER, 30, 8, 0))
        )
        // Before DST ends (still AEDT = UTC+11)
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 0, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.MARCH, 30, 13, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 12, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.MARCH, 31, 1, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.MARCH, 31, 18, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.MARCH, 31, 7, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 0, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.MARCH, 31, 13, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 1, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.MARCH, 31, 14, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 1, 59),
            DateUtils.removeTimezone(unixTime(2018, Calendar.MARCH, 31, 14, 59))
        )
        // DST ends (AEST = UTC+10)
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 2, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.MARCH, 31, 16, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 3, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.MARCH, 31, 17, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 4, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.MARCH, 31, 18, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 10, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.APRIL, 1, 0, 0))
        )
        assertEquals(
            unixTime(2018, Calendar.APRIL, 1, 18, 0),
            DateUtils.removeTimezone(unixTime(2018, Calendar.APRIL, 1, 8, 0))
        )
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private fun configureOffsetTest(startOfTodayOffset: Int) {
        DateUtils.setFixedTimeZone(TimeZone.getTimeZone("GMT"))
        DateUtils.setFixedLocalTime(fixedStartOfTodayWithOffset(startOfTodayOffset))
    }

    private fun fixedStartOfTodayWithOffset(hourOffset: Int): Long {
        return unixTime(2017, Calendar.JANUARY, 1, hourOffset, 0)
    }
}
