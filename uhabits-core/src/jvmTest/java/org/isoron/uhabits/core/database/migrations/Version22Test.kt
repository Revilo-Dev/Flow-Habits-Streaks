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

import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.isoron.platform.io.Database
import org.isoron.platform.io.JavaFileOpener
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.querySingle
import org.isoron.platform.io.run
import org.isoron.uhabits.core.BaseUnitTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertThrows

class Version22Test : BaseUnitTest() {
    private lateinit var db: Database

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        db = openDatabaseResource("/databases/021.db")
    }

    private val fileOpener = JavaFileOpener()

    private fun migrateTo(version: Int) = runBlocking {
        db.migrateTo(version) { v ->
            val path = "migrations/%02d.sql".format(v)
            fileOpener.openResourceFile(path).lines().joinToString("\n")
        }
    }

    @Test
    fun testKeepValidReps() {
        val before = db.querySingle("select count(*) from repetitions") { it.getInt(0) }
        assertThat(before, equalTo(3))
        migrateTo(22)
        val after = db.querySingle("select count(*) from repetitions") { it.getInt(0) }
        assertThat(after, equalTo(3))
    }

    @Test
    fun testRemoveRepsWithInvalidId() {
        db.run("insert into Repetitions(habit, timestamp, value) values (99999, 100, 2)")
        val before = db.querySingle(
            "select count(*) from repetitions where habit = 99999"
        ) { it.getInt(0) }
        assertThat(before, equalTo(1))
        migrateTo(22)
        val after = db.querySingle(
            "select count(*) from repetitions where habit = 99999"
        ) { it.getInt(0) }
        assertThat(after, equalTo(0))
    }

    @Test
    fun testDisallowNewRepsWithInvalidRef() {
        migrateTo(22)
        val exception = assertThrows(Exception::class.java) {
            db.run("insert into Repetitions(habit, timestamp, value) values (99999, 100, 2)")
        }
        assertThat(exception.message, Matchers.containsString("constraint"))
    }

    @Test
    fun testRemoveRepetitionsWithNullTimestamp() {
        db.run("insert into repetitions(habit, value) values (0, 2)")
        val before = db.querySingle(
            "select count(*) from repetitions where timestamp is null"
        ) { it.getInt(0) }
        assertThat(before, equalTo(1))
        migrateTo(22)
        val after = db.querySingle(
            "select count(*) from repetitions where timestamp is null"
        ) { it.getInt(0) }
        assertThat(after, equalTo(0))
    }

    @Test
    fun testDisallowNullTimestamp() {
        migrateTo(22)
        val exception = assertThrows(Exception::class.java) {
            db.run("insert into Repetitions(habit, value) values (0, 2)")
        }
        assertThat(exception.message, Matchers.containsString("constraint"))
    }

    @Test
    fun testRemoveRepetitionsWithNullHabit() {
        db.run("insert into repetitions(timestamp, value) values (0, 2)")
        val before = db.querySingle(
            "select count(*) from repetitions where habit is null"
        ) { it.getInt(0) }
        assertThat(before, equalTo(1))
        migrateTo(22)
        val after = db.querySingle(
            "select count(*) from repetitions where habit is null"
        ) { it.getInt(0) }
        assertThat(after, equalTo(0))
    }

    @Test
    fun testDisallowNullHabit() {
        migrateTo(22)
        val exception = assertThrows(Exception::class.java) {
            db.run("insert into Repetitions(timestamp, value) values (5, 2)")
        }
        assertThat(exception.message, Matchers.containsString("constraint"))
    }

    @Test
    fun testRemoveDuplicateRepetitions() {
        db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 2)")
        db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 5)")
        db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 10)")
        val before = db.querySingle(
            "select count(*) from repetitions where timestamp=100 and habit=0"
        ) { it.getInt(0) }
        assertThat(before, equalTo(3))
        migrateTo(22)
        val after = db.querySingle(
            "select count(*) from repetitions where timestamp=100 and habit=0"
        ) { it.getInt(0) }
        assertThat(after, equalTo(1))
    }

    @Test
    fun testDisallowNewDuplicateTimestamps() {
        migrateTo(22)
        db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 2)")
        val exception = assertThrows(Exception::class.java) {
            db.run("insert into repetitions(habit, timestamp, value)values (0, 100, 5)")
        }
        assertThat(exception.message, Matchers.containsString("constraint"))
    }
}
