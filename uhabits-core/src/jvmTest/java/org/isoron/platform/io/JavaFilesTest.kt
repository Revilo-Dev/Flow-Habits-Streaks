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

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaFilesTest {

    @Test
    fun testWriteStringAndLines() = runBlocking {
        val file = JavaUserFile(kotlin.io.path.createTempFile("test", ".txt"))
        file.writeString("hello\nworld\n")
        val lines = file.lines()
        assertEquals(listOf("hello", "world"), lines)
        file.delete()
    }

    @Test
    fun testWriteBytesAndReadBytes() = runBlocking {
        val file = JavaUserFile(kotlin.io.path.createTempFile("test", ".bin"))
        val data = byteArrayOf(0x53, 0x51, 0x4C, 0x69, 0x74, 0x65)
        file.writeBytes(data)
        val read = file.readBytes(6)
        assertTrue(data.contentEquals(read))
        file.delete()
    }

    @Test
    fun testReadBytesWithLimit() = runBlocking {
        val file = JavaUserFile(kotlin.io.path.createTempFile("test", ".bin"))
        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        val read = file.readBytes(3)
        assertEquals(3, read.size)
        assertEquals(1, read[0])
        assertEquals(2, read[1])
        assertEquals(3, read[2])
        file.delete()
    }

    @Test
    fun testPath() = runBlocking {
        val tmpPath = kotlin.io.path.createTempFile("test", ".txt")
        val file = JavaUserFile(tmpPath)
        assertEquals(tmpPath.toString(), file.pathString)
        file.delete()
    }

    @Test
    fun testExists() = runBlocking {
        val file = JavaUserFile(kotlin.io.path.createTempFile("test", ".txt"))
        assertTrue(file.exists())
        file.delete()
        assertFalse(file.exists())
    }
}
