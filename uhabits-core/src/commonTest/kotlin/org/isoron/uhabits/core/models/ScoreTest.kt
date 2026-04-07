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

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Score.Companion.compute
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ScoreTest : BaseUnitTest() {
    @BeforeTest
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun test_compute_withDailyHabit() {
        var check = 1
        val freq = 1.0
        assertCloseTo(0.051922, compute(freq, 0.0, check.toDouble()), E)
        assertCloseTo(0.525961, compute(freq, 0.5, check.toDouble()), E)
        assertCloseTo(0.762981, compute(freq, 0.75, check.toDouble()), E)
        check = 0
        assertCloseTo(0.0, compute(freq, 0.0, check.toDouble()), E)
        assertCloseTo(0.474039, compute(freq, 0.5, check.toDouble()), E)
        assertCloseTo(0.711058, compute(freq, 0.75, check.toDouble()), E)
    }

    @Test
    fun test_compute_withNonDailyHabit() {
        var check = 1
        val freq = 1 / 3.0
        assertCloseTo(0.030314, compute(freq, 0.0, check.toDouble()), E)
        assertCloseTo(0.515157, compute(freq, 0.5, check.toDouble()), E)
        assertCloseTo(0.757578, compute(freq, 0.75, check.toDouble()), E)
        check = 0
        assertCloseTo(0.0, compute(freq, 0.0, check.toDouble()), E)
        assertCloseTo(0.484842, compute(freq, 0.5, check.toDouble()), E)
        assertCloseTo(0.727263, compute(freq, 0.75, check.toDouble()), E)
    }

    companion object {
        private const val E = 1e-6

        private fun assertCloseTo(expected: Double, actual: Double, epsilon: Double) {
            val diff = kotlin.math.abs(expected - actual)
            assertTrue(diff <= epsilon, "Expected $expected ± $epsilon, but got $actual (diff=$diff)")
        }
    }
}
