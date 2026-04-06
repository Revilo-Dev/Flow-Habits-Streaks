package org.isoron.uhabits.core.database

import kotlin.test.Test
import kotlin.test.assertEquals

class SQLParserTest {
    @Test
    fun testBasicParsing() {
        val commands = SQLParser.parse("create table t(a int); insert into t values(1);")
        assertEquals(2, commands.size)
        assertEquals("create table t(a int)", commands[0])
        assertEquals("insert into t values(1)", commands[1])
    }

    @Test
    fun testCommentStripping() {
        val sql = """
            -- This is a comment
            create table t(a int);
            /* block comment */
            insert into t values(1);
        """.trimIndent()
        val commands = SQLParser.parse(sql)
        assertEquals(2, commands.size)
    }

    @Test
    fun testQuotedStrings() {
        val sql = "insert into t values('hello; world');"
        val commands = SQLParser.parse(sql)
        assertEquals(1, commands.size)
        assertEquals("insert into t values('hello; world')", commands[0])
    }

    @Test
    fun testEmptyInput() {
        assertEquals(0, SQLParser.parse("").size)
        assertEquals(0, SQLParser.parse("  \n  ").size)
        assertEquals(0, SQLParser.parse("-- just a comment").size)
    }
}
