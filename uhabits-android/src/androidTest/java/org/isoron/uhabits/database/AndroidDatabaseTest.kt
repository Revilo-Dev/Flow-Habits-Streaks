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
package org.isoron.uhabits.database

import android.database.sqlite.SQLiteDatabase
import org.isoron.platform.io.StepResult
import org.isoron.platform.io.begin
import org.isoron.platform.io.commit
import org.isoron.platform.io.query
import org.isoron.platform.io.queryInt
import org.isoron.platform.io.querySingle
import org.isoron.platform.io.run
import org.isoron.uhabits.BaseAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidDatabaseTest : BaseAndroidTest() {
    private lateinit var db: AndroidDatabase

    override fun setUp() {
        super.setUp()
        db = AndroidDatabase(SQLiteDatabase.create(null))
        db.run("create table test(color int, name string)")
    }

    @Test
    fun testInsertAndQuery() {
        db.run("insert into test(color, name) values (?, ?)") {
            bindNull(1)
            bindText(2, "asd")
        }
        val stmt = db.prepareStatement("select color, name from test")
        assertEquals(StepResult.ROW, stmt.step())
        assertNull(stmt.getIntOrNull(0))
        assertEquals("asd", stmt.getText(1))
        assertEquals(StepResult.DONE, stmt.step())
        stmt.finalize()
    }

    @Test
    fun testTransactionsViaRawSQL() {
        db.run("create table t(v int)")
        db.begin()
        db.run("insert into t(v) values (1)")
        db.run("insert into t(v) values (2)")
        db.commit()
        assertEquals(2, db.queryInt("select count(*) from t"))
    }

    @Test
    fun testQueryHelpers() {
        db.run("create table t(id int, name text)")
        db.run("insert into t(id, name) values (1, 'Alice')")
        db.run("insert into t(id, name) values (2, 'Bob')")

        val names = mutableListOf<String>()
        db.query("select name from t order by id") { stmt ->
            names.add(stmt.getText(0))
        }
        assertEquals(listOf("Alice", "Bob"), names)

        val single = db.querySingle("select name from t where id = ?", "2") {
            it.getText(0)
        }
        assertEquals("Bob", single)
    }

    @Test
    fun testNestedQueriesOnAndroid() {
        db.run("create table groups_t(id int, name text)")
        db.run("create table items(group_id int, label text)")
        db.run("insert into groups_t(id, name) values (1, 'G1')")
        db.run("insert into groups_t(id, name) values (2, 'G2')")
        db.run("insert into items(group_id, label) values (1, 'A')")
        db.run("insert into items(group_id, label) values (1, 'B')")
        db.run("insert into items(group_id, label) values (2, 'C')")

        val result = mutableMapOf<String, MutableList<String>>()
        db.query("select id, name from groups_t order by id") { g ->
            val gId = g.getInt(0)
            val gName = g.getText(1)
            val items = mutableListOf<String>()
            db.query(
                "select label from items where group_id = ? order by label",
                gId.toString()
            ) { item ->
                items.add(item.getText(0))
            }
            result[gName] = items
        }
        assertEquals(listOf("A", "B"), result["G1"]!!.toList())
        assertEquals(listOf("C"), result["G2"]!!.toList())
    }
}
