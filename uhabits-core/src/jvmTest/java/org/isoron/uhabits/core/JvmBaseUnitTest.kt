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

import dev.mokkery.spy
import org.apache.commons.io.IOUtils
import org.isoron.platform.io.JavaDatabaseOpener
import org.isoron.uhabits.core.models.memory.MemoryModelFactory
import org.isoron.uhabits.core.test.HabitFixtures
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import java.util.GregorianCalendar
import java.util.TimeZone

open class JvmBaseUnitTest : BaseUnitTest() {
    protected var databaseOpener: org.isoron.platform.io.DatabaseOpener = JavaDatabaseOpener()

    @Before
    override fun setUp() {
        super.setUp()
        habitList = spy(habitList)
        fixtures = HabitFixtures(modelFactory as MemoryModelFactory, habitList)
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
}
