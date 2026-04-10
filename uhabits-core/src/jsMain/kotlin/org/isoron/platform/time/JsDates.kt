package org.isoron.platform.time

import kotlin.js.Date

actual fun computeToday(hourOffset: Int, minuteOffset: Int): LocalDate {
    val now = Date()
    val localMillis = now.getTime() + now.getTimezoneOffset() * 60000.0 * -1
    val offsetMillis = hourOffset * 3600000.0 + minuteOffset * 60000.0
    val adjustedMillis = localMillis - offsetMillis
    val daysSinceEpoch = kotlin.math.floor(adjustedMillis / 86400000.0).toLong()
    val daysSince2000 = (daysSinceEpoch - 10957).toInt()
    return LocalDate(daysSince2000)
}

actual fun getFirstWeekdayNumberAccordingToLocale(): Int {
    // JS Intl does not expose first day of week. Default to Sunday (1)
    // matching java.util.Calendar convention where Sunday=1.
    return 1
}

private fun toLocaleDateString(date: Date, locale: String, options: dynamic): String {
    return js("date.toLocaleDateString(locale, options)").unsafeCast<String>()
}

class JsLocalDateFormatter(private val locale: String = "en-US") : LocalDateFormatter {
    override fun shortWeekdayName(weekday: DayOfWeek): String {
        val date = weekdayToJsDate(weekday)
        return formatWeekday(date, "short")
    }

    override fun shortWeekdayName(date: LocalDate): String {
        return formatWeekday(localDateToJsDate(date), "short")
    }

    override fun shortMonthName(date: LocalDate): String {
        val jsDate = localDateToJsDate(date)
        val options = js("{month: 'short', timeZone: 'UTC'}")
        return toLocaleDateString(jsDate, locale, options)
    }

    override fun longWeekdayName(weekday: DayOfWeek): String {
        val date = weekdayToJsDate(weekday)
        return formatWeekday(date, "long")
    }

    override fun longMonthName(date: LocalDate): String {
        val jsDate = localDateToJsDate(date)
        val options = js("{month: 'long', timeZone: 'UTC'}")
        return toLocaleDateString(jsDate, locale, options)
    }

    private fun formatWeekday(jsDate: Date, style: String): String {
        val options = js("{timeZone: 'UTC'}")
        options["weekday"] = style
        return toLocaleDateString(jsDate, locale, options)
    }

    private fun localDateToJsDate(date: LocalDate): Date {
        return Date(Date.UTC(date.year, date.month - 1, date.day))
    }

    private fun weekdayToJsDate(weekday: DayOfWeek): Date {
        // Jan 2, 2000 is a Sunday
        val dayOffset = weekday.daysSinceSunday
        return Date(Date.UTC(2000, 0, 2 + dayOffset))
    }
}
