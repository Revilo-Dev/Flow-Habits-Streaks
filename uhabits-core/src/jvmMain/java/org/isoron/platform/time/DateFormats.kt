package org.isoron.platform.time

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DateFormats {

    companion object {

        @JvmStatic fun fromSkeleton(
            skeleton: String,
            locale: Locale
        ): SimpleDateFormat {
            val df = SimpleDateFormat(skeleton, locale)
            df.timeZone = TimeZone.getTimeZone("UTC")
            return df
        }

        @JvmStatic fun getBackupDateFormat(): SimpleDateFormat =
            fromSkeleton("yyyy-MM-dd HHmmss", Locale.US)
    }
}
