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

package org.isoron.platform.io

import kotlin.test.Test
import kotlin.test.assertEquals

class StringsTest {
    @Test
    fun testFormat() {
        assertEquals("hello world!", format("hello %s!", "world"))
        assertEquals("  5", format("%3d", 5))
        assertEquals("005", format("%03d", 5))
        assertEquals(" 45", format("%3d", 45))
        assertEquals("145", format("%3d", 145))
        assertEquals("   13.42", format("%8.2f", 13.419187263))
        assertEquals("00013.42", format("%08.2f", 13.419187263))
        assertEquals("13.42   ", format("%-8.2f", 13.419187263))
    }

    @Test
    fun testParseCsvLine() {
        assertEquals(listOf("a", "b", "c"), parseCsvLine("a,b,c"))
        assertEquals(listOf("hello world", "foo", "bar"), parseCsvLine("hello world,foo,bar"))
        assertEquals(listOf("has,comma", "normal"), parseCsvLine("\"has,comma\",normal"))
        assertEquals(listOf("has\"quote", "x"), parseCsvLine("\"has\"\"quote\",x"))
        assertEquals(listOf("", "", ""), parseCsvLine(",,"))
        assertEquals(listOf("single"), parseCsvLine("single"))
        assertEquals(listOf("a", "line\nbreak", "b"), parseCsvLine("a,\"line\nbreak\",b"))
    }
}
