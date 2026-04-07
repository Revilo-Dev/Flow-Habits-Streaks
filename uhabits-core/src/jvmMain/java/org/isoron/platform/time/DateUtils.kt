package org.isoron.platform.time

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone

object DateUtils {
    const val SECOND_LENGTH: Long = 1000
    const val MINUTE_LENGTH: Long = 60 * SECOND_LENGTH
    const val HOUR_LENGTH: Long = 60 * MINUTE_LENGTH
    const val DAY_LENGTH: Long = 24 * HOUR_LENGTH

    private var fixedLocalTime: Long? = null

    @JvmStatic
    fun setFixedLocalTime(value: Long?) {
        fixedLocalTime = value
    }

    @JvmStatic
    var fixedTimeZone: TimeZone? = null
        private set

    fun setFixedTimeZone(value: TimeZone?) {
        fixedTimeZone = value
    }

    fun applyTimezone(
        localTimestamp: Long,
        tz: TimeZone = fixedTimeZone ?: TimeZone.getDefault()
    ): Long {
        return localTimestamp - tz.getOffset(localTimestamp - tz.getOffset(localTimestamp))
    }

    fun removeTimezone(
        timestamp: Long,
        tz: TimeZone = fixedTimeZone ?: TimeZone.getDefault()
    ): Long {
        return timestamp + tz.getOffset(timestamp)
    }

    fun getLocalTime(
        tz: TimeZone = fixedTimeZone ?: TimeZone.getDefault(),
        utcTimeInMillis: Long? = null
    ): Long {
        fixedLocalTime?.let { return it }
        val now = utcTimeInMillis ?: Date().time
        return now + tz.getOffset(now)
    }

    fun getStartOfDay(timestamp: Long): Long = (timestamp / DAY_LENGTH) * DAY_LENGTH

    fun getStartOfDayWithOffset(
        timestamp: Long,
        hourOffset: Int,
        minuteOffset: Int
    ): Long {
        val offset = hourOffset * HOUR_LENGTH + minuteOffset * MINUTE_LENGTH
        return getStartOfDay(timestamp - offset)
    }

    fun getStartOfTomorrowWithOffset(
        hourOffset: Int,
        minuteOffset: Int,
        tz: TimeZone = fixedTimeZone ?: TimeZone.getDefault()
    ): Long = getUpcomingTimeInMillis(hourOffset, minuteOffset, tz)

    fun millisecondsUntilTomorrowWithOffset(
        hourOffset: Int,
        minuteOffset: Int,
        tz: TimeZone = fixedTimeZone ?: TimeZone.getDefault()
    ): Long {
        return getStartOfTomorrowWithOffset(hourOffset, minuteOffset, tz) -
            applyTimezone(getLocalTime(tz), tz)
    }

    fun getUpcomingTimeInMillis(
        hour: Int,
        minute: Int,
        tz: TimeZone = fixedTimeZone ?: TimeZone.getDefault()
    ): Long {
        val localTime = getLocalTime(tz)
        val startOfToday = getStartOfDay(localTime)
        val calendar = GregorianCalendar(TimeZone.getTimeZone("GMT"))
        calendar.timeInMillis = startOfToday
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        var time = calendar.timeInMillis

        if (localTime > time) {
            time += DAY_LENGTH
        }

        return applyTimezone(time, tz)
    }
}
