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
package org.isoron.uhabits

import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.setToday
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.memory.MemoryModelFactory
import org.isoron.uhabits.core.tasks.CoroutineTaskRunner
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.core.test.HabitFixtures
import org.junit.After
import org.junit.Before
import org.junit.Test

open class BaseAndroidJVMTest {
    private lateinit var habitList: HabitList
    protected lateinit var fixtures: HabitFixtures
    private lateinit var modelFactory: MemoryModelFactory
    private lateinit var taskRunner: TaskRunner
    private lateinit var commandRunner: CommandRunner

    @Before
    open fun setUp() {
        setToday(LocalDate(2015, 1, 25))
        modelFactory = MemoryModelFactory()
        habitList = modelFactory.buildHabitList()
        fixtures = HabitFixtures(modelFactory, habitList)
        taskRunner = CoroutineTaskRunner(
            mainDispatcher = UnconfinedTestDispatcher(),
            ioDispatcher = UnconfinedTestDispatcher()
        )
        commandRunner = CommandRunner(taskRunner)
    }

    @After
    fun tearDown() {
    }

    @Test
    fun nothing() {
    }
}
