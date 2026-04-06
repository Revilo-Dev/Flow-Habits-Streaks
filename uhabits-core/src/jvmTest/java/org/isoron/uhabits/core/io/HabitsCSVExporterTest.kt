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
package org.isoron.uhabits.core.io

import kotlinx.coroutines.runBlocking
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Habit
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HabitsCSVExporterTest : BaseUnitTest() {
    private lateinit var baseDir: File

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        habitList.add(fixtures.createShortHabit())
        habitList.add(fixtures.createEmptyHabit())
        baseDir = Files.createTempDirectory("csv").toFile()
        baseDir.deleteOnExit()
    }

    @Test
    @Throws(IOException::class)
    fun testExportCSV() = runBlocking {
        val selected: MutableList<Habit> = LinkedList()
        for (h in habitList) selected.add(h)
        val exporter = HabitsCSVExporter(habitList, selected)
        val bytes = exporter.writeArchive()
        assertTrue(bytes.isNotEmpty())

        // Extract zip entries to baseDir for comparison
        unzip(bytes)

        val filesToCheck = arrayOf(
            "001 Meditate/Checkmarks.csv",
            "001 Meditate/Scores.csv",
            "002 Wake up early/Checkmarks.csv",
            "002 Wake up early/Scores.csv",
            "Checkmarks.csv",
            "Habits.csv",
            "Scores.csv"
        )

        for (file in filesToCheck) {
            assertPathExists(file)
            assertFileAndReferenceAreEqual(file)
        }
    }

    private fun unzip(bytes: ByteArray) {
        val zis = ZipInputStream(ByteArrayInputStream(bytes))
        var entry = zis.nextEntry
        while (entry != null) {
            val outFile = File(baseDir, entry.name)
            outFile.parentFile?.mkdirs()
            outFile.writeBytes(zis.readBytes())
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
    }

    private fun assertPathExists(s: String) {
        val file = File(baseDir, s)
        assertTrue(
            String.format("File %s should exist", file.absolutePath)
        ) { file.exists() }
    }

    private fun assertFileAndReferenceAreEqual(s: String) {
        val assetFilename = String.format("csv_export/%s", s)
        val actualFile = File(baseDir, s)
        val expectedFile = File.createTempFile("asset", "")
        expectedFile.deleteOnExit()
        copyAssetToFile(assetFilename, expectedFile)
        val actualContents = actualFile.readText()
        val expectedContents = expectedFile.readText()
        assertEquals(expectedContents, actualContents, "content mismatch for $s")
    }
}
