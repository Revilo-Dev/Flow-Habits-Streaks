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
package org.isoron.uhabits.core.commands

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateHabitCommandTest : BaseUnitTest() {
    private lateinit var command: CreateHabitCommand
    private lateinit var model: Habit

    @BeforeTest
    override fun setUp() {
        super.setUp()
        model = fixtures.createEmptyHabit()
        model.name = "New habit"
        model.reminder = Reminder(8, 30, WeekdayList.EVERY_DAY)
        command = CreateHabitCommand(modelFactory, habitList, model)
    }

    @Test
    fun testExecute() {
        assertTrue(habitList.isEmpty)
        command.run()
        assertEquals(1, habitList.size())
        val habit = habitList.getByPosition(0)
        assertEquals(model.name, habit.name)
    }
}
