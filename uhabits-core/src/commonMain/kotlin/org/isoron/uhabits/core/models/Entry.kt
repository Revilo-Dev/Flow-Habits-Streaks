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
package org.isoron.uhabits.core.models

import org.isoron.platform.time.LocalDate

data class Entry(
    val date: LocalDate,
    val value: Int,
    val notes: String = ""
) {
    val formattedValue: String
        get() = when (value) {
            YES_MANUAL -> "YES_MANUAL"
            YES_AUTO -> "YES_AUTO"
            NO -> "NO"
            SKIP -> "SKIP"
            UNKNOWN -> "UNKNOWN"
            else -> value.toString()
        }

    companion object {
        // A skip pauses a habit without recording a completion
        const val SKIP = 3

        // A manual completion records an action taken by the user
        const val YES_MANUAL = 2

        // An automatic completion covers a day allowed by the habit frequency
        const val YES_AUTO = 1

        // A zero value records a missed boolean habit
        const val NO = 0

        // An unknown value means no answer has been recorded
        const val UNKNOWN = -1

        // Cycle through only the answer states enabled in settings
        fun nextToggleValue(
            value: Int,
            isSkipEnabled: Boolean,
            areQuestionMarksEnabled: Boolean
        ): Int {
            return when (value) {
                YES_AUTO -> YES_MANUAL
                YES_MANUAL -> if (isSkipEnabled) SKIP else NO
                SKIP -> NO
                NO -> if (areQuestionMarksEnabled) UNKNOWN else YES_MANUAL
                UNKNOWN -> YES_MANUAL
                else -> YES_MANUAL
            }
        }
    }
}
