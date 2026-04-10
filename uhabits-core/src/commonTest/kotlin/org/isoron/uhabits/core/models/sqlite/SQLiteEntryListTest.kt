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

package org.isoron.uhabits.core.models.sqlite

import kotlinx.coroutines.test.runTest
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.BaseUnitTest.Companion.buildMemoryDatabase
import org.isoron.uhabits.core.database.EntryData
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Entry.Companion.UNKNOWN
import kotlin.test.Test
import kotlin.test.assertEquals

class SQLiteEntryListTest {

    private lateinit var factory: SQLModelFactory
    private lateinit var entries: SQLiteEntryList
    private val today = LocalDate(2015, 1, 25)

    private suspend fun initTest() {
        val database = buildMemoryDatabase()
        factory = SQLModelFactory(database)
        val habitList = factory.buildHabitList()
        val habit = factory.buildHabit()
        habitList.add(habit)
        entries = habit.originalEntries as SQLiteEntryList
    }

    @Test
    fun testLoad() = runTest {
        initTest()
        val today = LocalDate(2015, 1, 25)
        factory.entryRepository.insert(
            EntryData(
                habitId = entries.habitId,
                timestamp = today.unixTime,
                value = 500
            )
        )
        factory.entryRepository.insert(
            EntryData(
                habitId = entries.habitId,
                timestamp = today.minus(5).unixTime,
                value = 300
            )
        )
        assertEquals(
            Entry(date = today, value = 500),
            entries.get(today)
        )
        assertEquals(
            Entry(date = today.minus(1), value = UNKNOWN),
            entries.get(today.minus(1))
        )
        assertEquals(
            Entry(date = today.minus(5), value = 300),
            entries.get(today.minus(5))
        )
    }

    @Test
    fun testAdd() = runTest {
        initTest()
        val habitId = entries.habitId!!

        assertEquals(0, factory.entryRepository.findAllByHabitId(habitId).size)

        val original = Entry(today, 150)
        entries.add(original)

        val all = factory.entryRepository.findAllByHabitId(habitId)
        assertEquals(1, all.size)
        assertEquals(150, all[0].value)
        assertEquals(today.unixTime, all[0].timestamp)

        val replacement = Entry(today, 90)
        entries.add(replacement)

        val all2 = factory.entryRepository.findAllByHabitId(habitId)
        assertEquals(1, all2.size)
        assertEquals(90, all2[0].value)
    }
}
