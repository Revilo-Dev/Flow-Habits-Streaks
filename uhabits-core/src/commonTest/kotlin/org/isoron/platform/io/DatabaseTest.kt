package org.isoron.platform.io

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseTest {
    @Test
    fun testVersionReadWrite() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.setVersion(0)
        assertEquals(0, db.getVersion())
        db.setVersion(25)
        assertEquals(25, db.getVersion())
        db.close()
    }

    @Test
    fun testCreateInsertQuery() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()

        db.run("drop table if exists demo")
        db.run("create table demo(key int, value text)")

        val insert = db.prepareStatement("insert into demo(key, value) values (?, ?)")
        insert.bindInt(1, 42)
        insert.bindText(2, "Hello World")
        insert.step()
        insert.finalize()

        val select = db.prepareStatement("select * from demo where key > ?")
        select.bindInt(1, 10)
        assertEquals(StepResult.ROW, select.step())
        assertEquals(42, select.getInt(0))
        assertEquals("Hello World", select.getText(1))
        assertEquals(StepResult.DONE, select.step())
        select.finalize()

        db.close()
    }

    @Test
    fun testNullHandling() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()

        db.run("create table nullable_demo(a int, b text, c real)")
        val insert = db.prepareStatement("insert into nullable_demo(a, b, c) values (?, ?, ?)")
        insert.bindNull(1)
        insert.bindNull(2)
        insert.bindNull(3)
        insert.step()
        insert.finalize()

        val select = db.prepareStatement("select a, b, c from nullable_demo")
        assertEquals(StepResult.ROW, select.step())
        assertNull(select.getIntOrNull(0))
        assertNull(select.getTextOrNull(1))
        assertNull(select.getRealOrNull(2))
        select.finalize()

        db.close()
    }

    @Test
    fun testStatementReset() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()

        db.run("create table reset_demo(v int)")
        val insert = db.prepareStatement("insert into reset_demo(v) values (?)")

        insert.bindInt(1, 10)
        insert.step()
        insert.reset()

        insert.bindInt(1, 20)
        insert.step()
        insert.reset()

        insert.bindInt(1, 30)
        insert.step()
        insert.finalize()

        val select = db.prepareStatement("select v from reset_demo order by v")
        assertEquals(StepResult.ROW, select.step())
        assertEquals(10, select.getInt(0))
        assertEquals(StepResult.ROW, select.step())
        assertEquals(20, select.getInt(0))
        assertEquals(StepResult.ROW, select.step())
        assertEquals(30, select.getInt(0))
        assertEquals(StepResult.DONE, select.step())
        select.finalize()

        db.close()
    }

    @Test
    fun testRunExtensionFunction() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table ext_demo(v int)")
        db.run("insert into ext_demo(v) values (?)") { bindInt(1, 99) }
        assertEquals(99, db.queryInt("select v from ext_demo"))
        db.close()
    }

    @Test
    fun testLastInsertRowId() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table rowid_demo(id integer primary key autoincrement, v text)")
        db.run("insert into rowid_demo(v) values ('first')")
        val id1 = db.queryLong("select last_insert_rowid()")
        db.run("insert into rowid_demo(v) values ('second')")
        val id2 = db.queryLong("select last_insert_rowid()")
        assertTrue(id2 > id1)
        db.close()
    }
}
