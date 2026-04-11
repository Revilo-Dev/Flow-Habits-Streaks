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
package org.isoron.uhabits.core.ui.screens.habits.show

import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import kotlinx.coroutines.test.runTest
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.tasks.CoroutineTaskRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ShowHabitMenuPresenterTest : BaseUnitTest() {
    private lateinit var system: ShowHabitMenuPresenter.System
    private lateinit var screen: ShowHabitMenuPresenter.Screen
    private lateinit var habit: Habit
    private lateinit var menu: ShowHabitMenuPresenter

    @BeforeTest
    override fun setUp() {
        super.setUp()
        system = mock()
        screen = mock()
        habit = fixtures.createShortHabit()
        menu = ShowHabitMenuPresenter(
            commandRunner,
            habit,
            habitList,
            screen,
            system,
            taskRunner
        )
    }

    @Test
    fun testOnEditHabit() {
        menu.onEditHabit()
        verify { screen.showEditHabitScreen(habit) }
    }

    @Test
    fun testOnExport() = runTest {
        val outputDir = createTempDir()
        every { system.getCSVOutputDir() } returns outputDir
        menu.onExportCSV()
        (taskRunner as CoroutineTaskRunner).await()
        val files = outputDir.listFiles()
        assertEquals(1, files!!.size)
    }
}
