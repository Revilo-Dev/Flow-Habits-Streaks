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
package org.isoron.uhabits.core.ui.screens.habits.list

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.preferences.Preferences
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HintListTest : BaseUnitTest() {
    private lateinit var hintList: HintList
    private lateinit var hints: Array<String>

    private val prefs: Preferences = mock()
    private lateinit var today: LocalDate
    private lateinit var yesterday: LocalDate

    @BeforeTest
    override fun setUp() {
        super.setUp()
        today = getToday()
        yesterday = today.minus(1)
        hints = arrayOf("hint1", "hint2", "hint3")
        hintList = HintList(prefs, hints)
    }

    @Test
    @Throws(Exception::class)
    fun pop() {
        every { prefs.lastHintNumber } returns -1
        assertEquals("hint1", hintList.pop())
        verify { prefs.updateLastHint(0, today) }
        every { prefs.lastHintNumber } returns 2
        assertNull(hintList.pop())
    }

    @Test
    @Throws(Exception::class)
    fun shouldShow() {
        every { prefs.lastHintDate } returns today
        assertFalse(hintList.shouldShow())
        every { prefs.lastHintDate } returns yesterday
        assertTrue(hintList.shouldShow())
    }
}
