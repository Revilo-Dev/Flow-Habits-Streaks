package org.isoron.platform.io

import org.isoron.uhabits.core.database.SQLParser

enum class StepResult { ROW, DONE }

interface PreparedStatement {
    fun step(): StepResult
    fun getInt(index: Int): Int
    fun getLong(index: Int): Long
    fun getReal(index: Int): Double
    fun getText(index: Int): String
    fun getIntOrNull(index: Int): Int?
    fun getLongOrNull(index: Int): Long?
    fun getRealOrNull(index: Int): Double?
    fun getTextOrNull(index: Int): String?
    fun bindInt(index: Int, value: Int)
    fun bindLong(index: Int, value: Long)
    fun bindReal(index: Int, value: Double)
    fun bindText(index: Int, value: String)
    fun bindNull(index: Int)
    fun reset()
    fun finalize()
}

interface Database {
    fun prepareStatement(sql: String): PreparedStatement
    fun close()
}

interface DatabaseOpener {
    fun open(path: String): Database
}

fun Database.run(sql: String) {
    val stmt = prepareStatement(sql)
    stmt.step()
    stmt.finalize()
}

fun Database.run(sql: String, bind: PreparedStatement.() -> Unit) {
    val stmt = prepareStatement(sql)
    stmt.bind()
    stmt.step()
    stmt.finalize()
}

fun Database.queryInt(sql: String): Int {
    val stmt = prepareStatement(sql)
    stmt.step()
    val value = stmt.getInt(0)
    stmt.finalize()
    return value
}

fun Database.queryLong(sql: String): Long {
    val stmt = prepareStatement(sql)
    stmt.step()
    val value = stmt.getLong(0)
    stmt.finalize()
    return value
}

fun Database.getVersion(): Int {
    return queryInt("PRAGMA user_version")
}

fun Database.setVersion(v: Int) {
    run("PRAGMA user_version = $v")
}

fun Database.begin() {
    run("BEGIN")
}

fun Database.commit() {
    run("COMMIT")
}

inline fun Database.query(
    sql: String,
    vararg params: String,
    block: (PreparedStatement) -> Unit
) {
    val stmt = prepareStatement(sql)
    for (i in params.indices) {
        stmt.bindText(i + 1, params[i])
    }
    while (stmt.step() == StepResult.ROW) {
        block(stmt)
    }
    stmt.finalize()
}

inline fun <T> Database.querySingle(
    sql: String,
    vararg params: String,
    block: (PreparedStatement) -> T
): T? {
    val stmt = prepareStatement(sql)
    for (i in params.indices) {
        stmt.bindText(i + 1, params[i])
    }
    val result = if (stmt.step() == StepResult.ROW) block(stmt) else null
    stmt.finalize()
    return result
}

suspend fun Database.migrateTo(targetVersion: Int, loadMigrationSQL: suspend (Int) -> String) {
    val currentVersion = getVersion()
    if (currentVersion >= targetVersion) return
    for (v in (currentVersion + 1)..targetVersion) {
        val commands = SQLParser.parse(loadMigrationSQL(v))
        for (cmd in commands) run(cmd)
        setVersion(v)
    }
}
