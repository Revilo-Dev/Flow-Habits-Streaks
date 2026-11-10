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

import org.isoron.platform.JvmStatic
import org.isoron.platform.time.LocalDate
import kotlin.math.pow
import kotlin.math.sqrt

data class Score(
    val date: LocalDate,
    val value: Double
) {
    companion object {
        // Blend recent performance into the previous score using the habit frequency
        @JvmStatic
        fun compute(
            frequency: Double,
            previousScore: Double,
            checkmarkValue: Double
        ): Double {
            val multiplier = 0.5.pow(sqrt(frequency) / 13.0)
            var score = previousScore * multiplier
            score += checkmarkValue * (1 - multiplier)
            return score
        }
    }
}
