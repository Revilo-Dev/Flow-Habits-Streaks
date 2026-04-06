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
package org.isoron.uhabits.core

import org.apache.commons.io.IOUtils
import org.isoron.platform.io.JavaDatabaseOpener
import org.isoron.platform.io.TestDatabaseHelper
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.setToday
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.memory.MemoryModelFactory
import org.isoron.uhabits.core.tasks.SingleThreadTaskRunner
import org.isoron.uhabits.core.test.HabitFixtures
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.spy
import org.mockito.kotlin.validateMockitoUsage
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import java.util.GregorianCalendar
import java.util.TimeZone

@RunWith(MockitoJUnitRunner::class)
open class BaseUnitTest {
    protected open lateinit var habitList: HabitList
    protected lateinit var fixtures: HabitFixtures
    protected lateinit var modelFactory: ModelFactory
    protected lateinit var taskRunner: SingleThreadTaskRunner
    protected open lateinit var commandRunner: CommandRunner
    protected var databaseOpener: org.isoron.platform.io.DatabaseOpener = JavaDatabaseOpener()

    @Before
    @Throws(Exception::class)
    open fun setUp() {
        setToday(LocalDate(2015, 1, 25))
        val memoryModelFactory = MemoryModelFactory()
        habitList = spy(memoryModelFactory.buildHabitList())
        fixtures = HabitFixtures(memoryModelFactory, habitList)
        modelFactory = memoryModelFactory
        taskRunner = SingleThreadTaskRunner()
        commandRunner = CommandRunner(taskRunner)
    }

    @After
    @Throws(Exception::class)
    open fun tearDown() {
        validateMockitoUsage()
    }

    fun unixTime(year: Int, month: Int, day: Int): Long {
        return unixTime(year, month, day, 0, 0)
    }

    open fun unixTime(year: Int, month: Int, day: Int, hour: Int, minute: Int, milliseconds: Long = 0): Long {
        val cal = GregorianCalendar(TimeZone.getTimeZone("GMT"))
        cal.set(year, month, day, hour, minute, 0)
        cal.set(GregorianCalendar.MILLISECOND, 0)
        return cal.timeInMillis + milliseconds
    }

    @Test
    fun nothing() {
    }

    @Throws(IOException::class)
    protected fun copyAssetToFile(assetPath: String, dst: File?) {
        IOUtils.copy(openAsset(assetPath), FileOutputStream(dst!!))
    }

    @Throws(IOException::class)
    protected fun openAsset(assetPath: String): InputStream {
        var inputStream = javaClass.getResourceAsStream(assetPath)
        if (inputStream != null) return inputStream
        val pwd = Paths.get(".").toAbsolutePath().normalize().toString()
        val fullPath = "$pwd/assets/test/$assetPath"
        val file = File(fullPath)
        if (file.exists() && file.canRead()) inputStream = FileInputStream(file)
        if (inputStream != null) return inputStream
        throw IllegalStateException("asset not found: $fullPath")
    }

    @Throws(IOException::class)
    protected fun openDatabaseResource(path: String): org.isoron.platform.io.Database {
        val original = openAsset(path)
        val tmpDbFile = File.createTempFile("database", ".db")
        tmpDbFile.deleteOnExit()
        IOUtils.copy(original, FileOutputStream(tmpDbFile))
        return databaseOpener.open(tmpDbFile.absolutePath)
    }

    companion object {
        fun buildMemoryDatabase(): org.isoron.platform.io.Database {
            return TestDatabaseHelper.createEmptyDatabase()
        }
    }
}
