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
package org.isoron.uhabits.core.ui.screens.habits.list

import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.ui.callbacks.OnColorPickedCallback
import org.isoron.uhabits.core.ui.callbacks.OnConfirmedCallback
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListHabitsSelectionMenuBehaviorTest : BaseUnitTest() {
    private val screen: ListHabitsSelectionMenuBehavior.Screen = mock()

    private val adapter: ListHabitsSelectionMenuBehavior.Adapter = mock()
    private lateinit var behavior: ListHabitsSelectionMenuBehavior
    private lateinit var habit1: Habit
    private lateinit var habit2: Habit
    private lateinit var habit3: Habit

    @Test
    @Throws(Exception::class)
    fun canArchive() {
        every { adapter.getSelected() } returns listOf(habit1, habit2)
        assertFalse(behavior.canArchive())
        every { adapter.getSelected() } returns listOf(habit2, habit3)
        assertTrue(behavior.canArchive())
    }

    @Test
    @Throws(Exception::class)
    fun canEdit() {
        every { adapter.getSelected() } returns listOf(habit1)
        assertTrue(behavior.canEdit())
        every { adapter.getSelected() } returns listOf(habit1, habit2)
        assertFalse(behavior.canEdit())
    }

    @Test
    @Throws(Exception::class)
    fun canUnarchive() {
        every { adapter.getSelected() } returns listOf(habit1, habit2)
        assertFalse(behavior.canUnarchive())
        every { adapter.getSelected() } returns listOf(habit1)
        assertTrue(behavior.canUnarchive())
    }

    @Test
    @Throws(Exception::class)
    fun onArchiveHabits() {
        assertFalse(habit2.isArchived)
        every { adapter.getSelected() } returns listOf(habit2)
        behavior.onArchiveHabits()
        assertTrue(habit2.isArchived)
    }

    @Test
    @Throws(Exception::class)
    fun onChangeColor() {
        assertEquals(PaletteColor(8), habit1.color)
        assertEquals(PaletteColor(8), habit2.color)
        every { adapter.getSelected() } returns listOf(habit1, habit2)
        every {
            screen.showColorPicker(any(), any())
        } calls { args ->
            val callback = args.arg<OnColorPickedCallback>(1)
            callback.onColorPicked(PaletteColor(30))
        }
        behavior.onChangeColor()
        assertEquals(PaletteColor(30), habit1.color)
    }

    @Test
    @Throws(Exception::class)
    fun onDeleteHabits() {
        val id = habit1.id!!
        habitList.getById(id)!!
        every { adapter.getSelected() } returns listOf(habit1)
        every {
            screen.showDeleteConfirmationScreen(any(), any())
        } calls { args ->
            val callback = args.arg<OnConfirmedCallback>(0)
            callback.onConfirmed()
        }
        behavior.onDeleteHabits()
        assertNull(habitList.getById(id))
    }

    @Test
    @Throws(Exception::class)
    fun onEditHabits() {
        val selected: List<Habit> = listOf(habit1, habit2)
        every { adapter.getSelected() } returns selected
        behavior.onEditHabits()
        verify { screen.showEditHabitsScreen(selected) }
    }

    @Test
    @Throws(Exception::class)
    fun onUnarchiveHabits() {
        assertTrue(habit1.isArchived)
        every { adapter.getSelected() } returns listOf(habit1)
        behavior.onUnarchiveHabits()
        assertFalse(habit1.isArchived)
    }

    @BeforeTest
    override fun setUp() {
        super.setUp()
        habit1 = fixtures.createShortHabit()
        habit1.isArchived = true
        habit2 = fixtures.createShortHabit()
        habit3 = fixtures.createShortHabit()
        habitList.add(habit1)
        habitList.add(habit2)
        habitList.add(habit3)
        behavior = ListHabitsSelectionMenuBehavior(
            habitList,
            screen,
            adapter,
            commandRunner
        )
    }
}
