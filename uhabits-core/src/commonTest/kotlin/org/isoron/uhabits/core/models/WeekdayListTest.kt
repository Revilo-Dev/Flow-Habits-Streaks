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
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeekdayListTest : BaseUnitTest() {
    @Test
    fun test() {
        val daysInt = 124
        val daysArray = booleanArrayOf(false, false, true, true, true, true, true)
        var list = WeekdayList(daysArray)
        assertContentEquals(daysArray, list.toArray())
        assertEquals(daysInt, list.toInteger())
        list = WeekdayList(daysInt)
        assertContentEquals(daysArray, list.toArray())
        assertEquals(daysInt, list.toInteger())
    }

    @Test
    fun testEmpty() {
        val list = WeekdayList(0)
        assertTrue(list.isEmpty)
        assertFalse(WeekdayList.EVERY_DAY.isEmpty)
    }

    @Test
    fun testWeekdayList_IntConstructor_toString() {
        val string = WeekdayList(0).toString()
        assertEquals("{weekdays: [false,false,false,false,false,false,false]}", string)
    }

    @Test
    fun testWeekdayList_BooleanArrayConstructor_toString() {
        val string = WeekdayList(
            booleanArrayOf(false, false, true, true, true, true, true)
        ).toString()
        assertEquals("{weekdays: [false,false,true,true,true,true,true]}", string)
    }
}
