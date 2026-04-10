package org.isoron.platform.io

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseQueryHelpersTest {
    @Test
    fun testQueryIteratesAllRows() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table t(v int)")
        db.run("insert into t(v) values (10)")
        db.run("insert into t(v) values (20)")
        db.run("insert into t(v) values (30)")

        val values = mutableListOf<Int>()
        db.query("select v from t order by v") { stmt ->
            values.add(stmt.getInt(0))
        }
        assertEquals(listOf(10, 20, 30), values)
        db.close()
    }

    @Test
    fun testQueryWithParameters() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table t(name text, age int)")
        db.run("insert into t(name, age) values ('Alice', 30)")
        db.run("insert into t(name, age) values ('Bob', 25)")
        db.run("insert into t(name, age) values ('Carol', 35)")

        val names = mutableListOf<String>()
        db.query("select name from t where age > ?", "28") { stmt ->
            names.add(stmt.getText(0))
        }
        assertEquals(2, names.size)
        assertTrue(names.contains("Alice"))
        assertTrue(names.contains("Carol"))
        db.close()
    }

    @Test
    fun testQueryWithNoResults() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table t(v int)")

        var called = false
        db.query("select v from t") { called = true }
        assertFalse(called)
        db.close()
    }

    @Test
    fun testQuerySingleReturnsFirstRow() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table t(v int)")
        db.run("insert into t(v) values (42)")
        db.run("insert into t(v) values (99)")

        val result = db.querySingle("select v from t order by v") { it.getInt(0) }
        assertEquals(42, result)
        db.close()
    }

    @Test
    fun testQuerySingleReturnsNullForEmptyResult() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table t(v int)")

        val result = db.querySingle("select v from t") { it.getInt(0) }
        assertNull(result)
        db.close()
    }

    @Test
    fun testQuerySingleWithParameters() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table t(name text, score int)")
        db.run("insert into t(name, score) values ('Alice', 90)")
        db.run("insert into t(name, score) values ('Bob', 80)")

        val score = db.querySingle(
            "select score from t where name = ?",
            "Alice"
        ) { it.getInt(0) }
        assertEquals(90, score)

        val missing = db.querySingle(
            "select score from t where name = ?",
            "Nobody"
        ) { it.getInt(0) }
        assertNull(missing)
        db.close()
    }

    @Test
    fun testNestedQueries() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table parents(id int, name text)")
        db.run("create table children(parent_id int, name text)")
        db.run("insert into parents(id, name) values (1, 'Alice')")
        db.run("insert into parents(id, name) values (2, 'Bob')")
        db.run("insert into children(parent_id, name) values (1, 'Charlie')")
        db.run("insert into children(parent_id, name) values (1, 'Diana')")
        db.run("insert into children(parent_id, name) values (2, 'Eve')")

        val result = mutableMapOf<String, MutableList<String>>()
        db.query("select id, name from parents order by id") { parent ->
            val parentId = parent.getInt(0)
            val parentName = parent.getText(1)
            val kids = mutableListOf<String>()
            db.query(
                "select name from children where parent_id = ? order by name",
                parentId.toString()
            ) { child ->
                kids.add(child.getText(0))
            }
            result[parentName] = kids
        }

        assertEquals(listOf("Charlie", "Diana"), result["Alice"]!!.toList())
        assertEquals(listOf("Eve"), result["Bob"]!!.toList())
        db.close()
    }

    @Test
    fun testQueryHandlesNullableColumns() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("create table t(a int, b text)")
        db.run("insert into t(a, b) values (1, 'hello')")
        db.run("insert into t(a, b) values (null, null)")

        val results = mutableListOf<Pair<Int?, String?>>()
        db.query("select a, b from t order by rowid") { stmt ->
            results.add(Pair(stmt.getIntOrNull(0), stmt.getTextOrNull(1)))
        }
        assertEquals(2, results.size)
        assertEquals(1, results[0].first)
        assertEquals("hello", results[0].second)
        assertNull(results[1].first)
        assertNull(results[1].second)
        db.close()
    }
}
