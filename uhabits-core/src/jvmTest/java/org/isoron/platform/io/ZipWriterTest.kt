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
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZipWriterTest {

    @Test
    fun testSingleEntry() = runBlocking {
        val zip = ZipWriter()
        zip.addEntry("hello.txt", "Hello, World!")
        val bytes = zip.toBytes()

        val entries = readZipEntries(bytes)
        assertEquals(1, entries.size)
        assertEquals("Hello, World!", entries["hello.txt"])
    }

    @Test
    fun testMultipleEntries() = runBlocking {
        val zip = ZipWriter()
        zip.addEntry("a.csv", "name,value\nfoo,1\n")
        zip.addEntry("subdir/b.csv", "col1,col2\nbar,2\n")
        zip.addEntry("subdir/c.csv", "x\n")
        val bytes = zip.toBytes()

        val entries = readZipEntries(bytes)
        assertEquals(3, entries.size)
        assertEquals("name,value\nfoo,1\n", entries["a.csv"])
        assertEquals("col1,col2\nbar,2\n", entries["subdir/b.csv"])
        assertEquals("x\n", entries["subdir/c.csv"])
    }

    @Test
    fun testEmptyContent() = runBlocking {
        val zip = ZipWriter()
        zip.addEntry("empty.txt", "")
        val bytes = zip.toBytes()

        val entries = readZipEntries(bytes)
        assertEquals(1, entries.size)
        assertEquals("", entries["empty.txt"])
    }

    @Test
    fun testLargeContent() = runBlocking {
        val zip = ZipWriter()
        val large = "x".repeat(100_000)
        zip.addEntry("large.txt", large)
        val bytes = zip.toBytes()

        assertTrue(bytes.size < large.length, "ZIP should compress repeated content")
        val entries = readZipEntries(bytes)
        assertEquals(large, entries["large.txt"])
    }

    private fun readZipEntries(bytes: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val zis = ZipInputStream(ByteArrayInputStream(bytes))
        var entry = zis.nextEntry
        while (entry != null) {
            result[entry.name] = zis.readBytes().decodeToString()
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        return result
    }
}
