package org.isoron.platform.time

import java.util.GregorianCalendar
import java.util.TimeZone

fun unixTime(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, milliseconds: Long = 0): Long {
    val cal = GregorianCalendar(TimeZone.getTimeZone("GMT"))
    cal.set(year, month, day, hour, minute, 0)
    cal.set(GregorianCalendar.MILLISECOND, 0)
    return cal.timeInMillis + milliseconds
}
