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

import org.isoron.platform.io.Database
import org.isoron.platform.io.DatabaseOpener
import org.isoron.platform.io.FileOpener
import org.isoron.platform.io.TestDatabaseHelper
import org.isoron.platform.io.UserFile
import org.isoron.platform.io.createTestDatabaseOpener
import org.isoron.platform.io.createTestFileOpener
import org.isoron.platform.runSuspend
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.setToday
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.memory.MemoryModelFactory
import org.isoron.uhabits.core.tasks.SingleThreadTaskRunner
import org.isoron.uhabits.core.test.HabitFixtures
import kotlin.test.BeforeTest

open class BaseUnitTest {
    protected open lateinit var habitList: HabitList
    protected lateinit var fixtures: HabitFixtures
    protected lateinit var modelFactory: ModelFactory
    protected lateinit var taskRunner: SingleThreadTaskRunner
    protected open lateinit var commandRunner: CommandRunner
    protected val fileOpener: FileOpener = createTestFileOpener()
    protected val databaseOpener: DatabaseOpener = createTestDatabaseOpener()

    @BeforeTest
    open fun setUp() {
        setToday(LocalDate(2015, 1, 25))
        val memoryModelFactory = MemoryModelFactory()
        habitList = memoryModelFactory.buildHabitList()
        fixtures = HabitFixtures(memoryModelFactory, habitList)
        modelFactory = memoryModelFactory
        taskRunner = SingleThreadTaskRunner()
        commandRunner = CommandRunner(taskRunner)
    }

    protected fun createTempDir(): UserFile = runSuspend {
        val dir = fileOpener.openUserFile("test-temp-dir-${tempFileCounter++}")
        dir.mkdirs()
        dir
    }

    protected fun copyResourceToTempFile(resourcePath: String): UserFile = runSuspend {
        val cleanPath = resourcePath.removePrefix("/")
        val tempFile = fileOpener.openUserFile("test-temp-${tempFileCounter++}")
        fileOpener.openResourceFile(cleanPath).copyTo(tempFile)
        tempFile
    }

    protected fun openDatabaseResource(resourcePath: String): Database = runSuspend {
        val tempFile = copyResourceToTempFile(resourcePath)
        databaseOpener.open(tempFile.pathString)
    }

    companion object {
        private var tempFileCounter = 0

        fun buildMemoryDatabase(): Database {
            return TestDatabaseHelper.createEmptyDatabase()
        }
    }
}
