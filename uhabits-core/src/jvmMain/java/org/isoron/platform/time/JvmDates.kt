package org.isoron.platform.time

import java.util.TimeZone

fun computeToday(hourOffset: Int = 0, minuteOffset: Int = 0): LocalDate {
    val nowMillis = System.currentTimeMillis()
    val tz = TimeZone.getDefault()
    val localMillis = nowMillis + tz.getOffset(nowMillis)
    val offsetMillis = hourOffset * 3600000L + minuteOffset * 60000L
    val adjustedMillis = localMillis - offsetMillis
    val daysSinceEpoch = Math.floorDiv(adjustedMillis, 86400000L)
    val daysSince2000 = (daysSinceEpoch - 10957).toInt()
    return LocalDate(daysSince2000)
}
