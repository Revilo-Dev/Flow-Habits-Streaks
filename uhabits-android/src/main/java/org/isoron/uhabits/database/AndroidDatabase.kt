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

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import org.isoron.platform.io.PreparedStatement
import org.isoron.platform.io.StepResult
import java.io.File

class AndroidPreparedStatement(
    private val db: SQLiteDatabase,
    private val sql: String
) : PreparedStatement {
    private val isQuery = sql.trimStart().uppercase().let {
        it.startsWith("SELECT") || it.startsWith("PRAGMA")
    }

    private val compiledStmt: SQLiteStatement? = if (!isQuery) db.compileStatement(sql) else null

    private val bindings = mutableMapOf<Int, Any?>()
    private var cursor: android.database.Cursor? = null

    override fun step(): StepResult {
        if (isQuery) {
            if (cursor == null) {
                val args = buildBindArgs()
                cursor = db.rawQuery(sql, args)
            }
            return if (cursor!!.moveToNext()) StepResult.ROW else StepResult.DONE
        } else {
            compiledStmt!!.execute()
            return StepResult.DONE
        }
    }

    override fun getInt(index: Int) = cursor!!.getInt(index)
    override fun getLong(index: Int) = cursor!!.getLong(index)
    override fun getReal(index: Int) = cursor!!.getDouble(index)
    override fun getText(index: Int) = cursor!!.getString(index)

    override fun getIntOrNull(index: Int): Int? =
        if (cursor!!.isNull(index)) null else cursor!!.getInt(index)

    override fun getLongOrNull(index: Int): Long? =
        if (cursor!!.isNull(index)) null else cursor!!.getLong(index)

    override fun getRealOrNull(index: Int): Double? =
        if (cursor!!.isNull(index)) null else cursor!!.getDouble(index)

    override fun getTextOrNull(index: Int): String? =
        if (cursor!!.isNull(index)) null else cursor!!.getString(index)

    override fun bindInt(index: Int, value: Int) {
        if (isQuery) {
            bindings[index] = value.toString()
        } else {
            compiledStmt!!.bindLong(index, value.toLong())
        }
    }

    override fun bindLong(index: Int, value: Long) {
        if (isQuery) {
            bindings[index] = value.toString()
        } else {
            compiledStmt!!.bindLong(index, value)
        }
    }

    override fun bindReal(index: Int, value: Double) {
        if (isQuery) {
            bindings[index] = value.toString()
        } else {
            compiledStmt!!.bindDouble(index, value)
        }
    }

    override fun bindText(index: Int, value: String) {
        if (isQuery) {
            bindings[index] = value
        } else {
            compiledStmt!!.bindString(index, value)
        }
    }

    override fun bindNull(index: Int) {
        if (isQuery) {
            bindings[index] = null
        } else {
            compiledStmt!!.bindNull(index)
        }
    }

    override fun reset() {
        cursor?.close()
        cursor = null
        bindings.clear()
        compiledStmt?.clearBindings()
    }

    override fun finalize() {
        cursor?.close()
        compiledStmt?.close()
    }

    private fun buildBindArgs(): Array<String>? {
        if (bindings.isEmpty()) return null
        val maxIndex = bindings.keys.max()
        return Array(maxIndex) { i -> bindings[i + 1]?.toString() ?: "" }
    }
}

/**
 * Implements both the new multiplatform Database interface (for repositories)
 * and the old JVM-only Database interface (for importers that still use Cursor).
 */
class AndroidDatabase(
    private val db: SQLiteDatabase,
    override val file: File? = null
) : org.isoron.platform.io.Database, org.isoron.uhabits.core.database.Database {

    // New PreparedStatement-based interface
    override fun prepareStatement(sql: String): PreparedStatement =
        AndroidPreparedStatement(db, sql)

    // Old Cursor-based interface
    override fun query(q: String, vararg params: String) = AndroidCursor(db.rawQuery(q, params))
    override fun execute(query: String, vararg params: Any) = db.execSQL(query, params)

    override fun update(
        tableName: String,
        values: Map<String, Any?>,
        where: String,
        vararg params: String
    ): Int {
        val contValues = mapToContentValues(values)
        return db.update(tableName, contValues, where, params)
    }

    override fun insert(tableName: String, values: Map<String, Any?>): Long {
        val contValues = mapToContentValues(values)
        return db.insert(tableName, null, contValues)
    }

    override fun delete(tableName: String, where: String, vararg params: String) {
        db.delete(tableName, where, params)
    }

    override fun beginTransaction() = db.beginTransaction()
    override fun setTransactionSuccessful() = db.setTransactionSuccessful()
    override fun endTransaction() = db.endTransaction()

    override fun close() = db.close()

    override val version: Int
        get() = db.version

    private fun mapToContentValues(map: Map<String, Any?>): ContentValues {
        val values = ContentValues()
        for ((key, value) in map) {
            when (value) {
                null -> values.putNull(key)
                is Int -> values.put(key, value)
                is Long -> values.put(key, value)
                is Double -> values.put(key, value)
                is String -> values.put(key, value)
                else -> throw IllegalStateException("unsupported type: $value")
            }
        }
        return values
    }
}

class AndroidCursor(private val cursor: android.database.Cursor) : org.isoron.uhabits.core.database.Cursor {

    override fun close() = cursor.close()
    override fun moveToNext() = cursor.moveToNext()

    override fun getInt(index: Int): Int? =
        if (cursor.isNull(index)) null else cursor.getInt(index)

    override fun getLong(index: Int): Long? =
        if (cursor.isNull(index)) null else cursor.getLong(index)

    override fun getDouble(index: Int): Double? =
        if (cursor.isNull(index)) null else cursor.getDouble(index)

    override fun getString(index: Int): String? =
        if (cursor.isNull(index)) null else cursor.getString(index)
}
