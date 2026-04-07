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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteHabitsCommandTest : BaseUnitTest() {
    private lateinit var command: DeleteHabitsCommand
    private lateinit var selected: MutableList<Habit>

    @BeforeTest
    override fun setUp() {
        super.setUp()
        selected = mutableListOf()

        // Habits that should be deleted
        for (i in 0..2) {
            val habit = fixtures.createShortHabit()
            habitList.add(habit)
            selected.add(habit)
        }

        // Extra habit that should not be deleted
        val extraHabit = fixtures.createShortHabit()
        extraHabit.name = "extra"
        habitList.add(extraHabit)
        command = DeleteHabitsCommand(habitList, selected)
    }

    @Test
    fun testExecute() {
        assertEquals(4, habitList.size())
        command.run()
        assertEquals(1, habitList.size())
        assertEquals("extra", habitList.getByPosition(0).name)
    }
}
