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
import android.database.sqlite.SQLiteStatement
import org.isoron.platform.io.PreparedStatement
import org.isoron.platform.io.StepResult

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

class AndroidDatabase(
    private val db: SQLiteDatabase
) : org.isoron.platform.io.Database {

    override fun prepareStatement(sql: String): PreparedStatement =
        AndroidPreparedStatement(db, sql)

    override fun close() = db.close()
}
