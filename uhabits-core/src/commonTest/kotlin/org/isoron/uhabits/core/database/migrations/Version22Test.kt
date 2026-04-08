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
package org.isoron.uhabits.core.database.migrations

import org.isoron.platform.io.Database
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.querySingle
import org.isoron.platform.io.run
import org.isoron.platform.runSuspend
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Version22Test : BaseUnitTest() {
    private lateinit var db: Database

    override fun setUp() {
        super.setUp()
        db = openDatabaseResource("/databases/021.db")
    }

    private fun migrateTo(version: Int) = runSuspend {
        db.migrateTo(version) { v ->
            val path = "migrations/%02d.sql".format(v)
            fileOpener.openResourceFile(path).lines().joinToString("\n")
        }
    }

    @Test
    fun testKeepValidReps() {
        val before = db.querySingle("select count(*) from repetitions") { it.getInt(0) }
        assertEquals(3, before)
        migrateTo(22)
        val after = db.querySingle("select count(*) from repetitions") { it.getInt(0) }
        assertEquals(3, after)
    }

    @Test
    fun testRemoveRepsWithInvalidId() {
        db.run("insert into Repetitions(habit, timestamp, value) values (99999, 100, 2)")
        val before = db.querySingle(
            "select count(*) from repetitions where habit = 99999"
        ) { it.getInt(0) }
        assertEquals(1, before)
        migrateTo(22)
        val after = db.querySingle(
            "select count(*) from repetitions where habit = 99999"
        ) { it.getInt(0) }
        assertEquals(0, after)
    }

    @Test
    fun testDisallowNewRepsWithInvalidRef() {
        migrateTo(22)
        val exception = assertFailsWith<Exception> {
            db.run("insert into Repetitions(habit, timestamp, value) values (99999, 100, 2)")
        }
        assertContains(exception.message!!, "constraint")
    }

    @Test
    fun testRemoveRepetitionsWithNullTimestamp() {
        db.run("insert into repetitions(habit, value) values (0, 2)")
        val before = db.querySingle(
            "select count(*) from repetitions where timestamp is null"
        ) { it.getInt(0) }
        assertEquals(1, before)
        migrateTo(22)
        val after = db.querySingle(
            "select count(*) from repetitions where timestamp is null"
        ) { it.getInt(0) }
        assertEquals(0, after)
    }

    @Test
    fun testDisallowNullTimestamp() {
        migrateTo(22)
        val exception = assertFailsWith<Exception> {
            db.run("insert into Repetitions(habit, value) values (0, 2)")
        }
        assertContains(exception.message!!, "constraint")
    }

    @Test
    fun testRemoveRepetitionsWithNullHabit() {
        db.run("insert into repetitions(timestamp, value) values (0, 2)")
        val before = db.querySingle(
            "select count(*) from repetitions where habit is null"
        ) { it.getInt(0) }
        assertEquals(1, before)
        migrateTo(22)
        val after = db.querySingle(
            "select count(*) from repetitions where habit is null"
        ) { it.getInt(0) }
        assertEquals(0, after)
    }

    @Test
    fun testDisallowNullHabit() {
        migrateTo(22)
        val exception = assertFailsWith<Exception> {
            db.run("insert into Repetitions(timestamp, value) values (5, 2)")
        }
        assertContains(exception.message!!, "constraint")
    }

    @Test
    fun testRemoveDuplicateRepetitions() {
        db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 2)")
        db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 5)")
        db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 10)")
        val before = db.querySingle(
            "select count(*) from repetitions where timestamp=100 and habit=0"
        ) { it.getInt(0) }
        assertEquals(3, before)
        migrateTo(22)
        val after = db.querySingle(
            "select count(*) from repetitions where timestamp=100 and habit=0"
        ) { it.getInt(0) }
        assertEquals(1, after)
    }

    @Test
    fun testDisallowNewDuplicateTimestamps() {
        migrateTo(22)
        db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 2)")
        val exception = assertFailsWith<Exception> {
            db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 5)")
        }
        assertContains(exception.message!!, "constraint")
    }
}
