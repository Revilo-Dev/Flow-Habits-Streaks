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
package org.isoron.uhabits.core.io

import me.tatarka.inject.annotations.Inject
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateHabitCommand
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.isoron.uhabits.core.database.Cursor
import org.isoron.uhabits.core.database.Database
import org.isoron.uhabits.core.database.DatabaseOpener
import org.isoron.uhabits.core.database.HabitData
import org.isoron.uhabits.core.database.MigrationHelper
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.sqlite.SQLiteHabitList
import org.isoron.uhabits.core.utils.isSQLite3File
import java.io.File

/**
 * Class that imports data from database files exported by Loop Habit Tracker.
 */
@Inject
class LoopDBImporter(
    @AppScope val habitList: HabitList,
    @AppScope val modelFactory: ModelFactory,
    @AppScope val opener: DatabaseOpener,
    @AppScope val runner: CommandRunner,
    @AppScope logging: Logging
) : AbstractImporter() {

    private val logger = logging.getLogger("LoopDBImporter")

    override fun canHandle(file: File): Boolean {
        if (!file.isSQLite3File()) return false
        val db = opener.open(file)
        var canHandle = true
        val c = db.query("select count(*) from SQLITE_MASTER where name='Habits' or name='Repetitions'")
        if (!c.moveToNext() || c.getInt(0) != 2) {
            logger.error("Cannot handle file: tables not found")
            canHandle = false
        }
        if (db.version > DATABASE_VERSION) {
            logger.error("Cannot handle file: incompatible version: ${db.version} > $DATABASE_VERSION")
            canHandle = false
        }
        c.close()
        db.close()
        return canHandle
    }

    override fun importHabitsFromFile(file: File) {
        val db = opener.open(file)
        val helper = MigrationHelper(db)
        helper.migrateTo(DATABASE_VERSION)

        val habitDataList = loadHabits(db)
        for (habitData in habitDataList) {
            var habit = habitList.getByUUID(habitData.uuid)

            if (habit == null) {
                habit = modelFactory.buildHabit()
                val imported = habitData.copy(id = null)
                SQLiteHabitList.copyTo(imported, habit)
                CreateHabitCommand(modelFactory, habitList, habit).run()
            } else {
                val modified = modelFactory.buildHabit()
                SQLiteHabitList.copyTo(habitData.copy(id = habit.id), modified)
                EditHabitCommand(habitList, habit.id!!, modified).run()
            }

            // Reload saved version of the habit
            habit = habitList.getByUUID(habitData.uuid)!!
            val entries = habit.originalEntries

            // Import entries
            loadEntries(db, habitData.id!!).use { c ->
                while (c.moveToNext()) {
                    val timestamp = c.getLong(0) ?: continue
                    val value = c.getInt(1) ?: continue
                    val notes = c.getString(2) ?: ""
                    val date = LocalDate.fromUnixTime(timestamp)
                    val (_, existingValue, existingNotes) = entries.get(date)
                    if (existingValue != value || existingNotes != notes) {
                        entries.add(Entry(date, value, notes))
                    }
                }
            }
            habit.recompute()
        }
        habitList.resort()
        db.close()
    }

    private fun loadHabits(db: Database): List<HabitData> {
        val result = mutableListOf<HabitData>()
        db.query(
            "SELECT id, name, description, question, freq_num, freq_den, color, " +
                "position, reminder_hour, reminder_min, reminder_days, highlight, " +
                "archived, type, target_value, target_type, unit, uuid " +
                "FROM Habits ORDER BY position"
        ).use { c ->
            while (c.moveToNext()) {
                result.add(cursorToHabitData(c))
            }
        }
        return result
    }

    private fun loadEntries(db: Database, habitId: Long): Cursor {
        return db.query(
            "SELECT timestamp, value, notes FROM Repetitions WHERE habit = ? ORDER BY timestamp DESC",
            habitId.toString()
        )
    }

    private fun cursorToHabitData(c: Cursor): HabitData {
        return HabitData(
            id = c.getLong(0),
            name = c.getString(1) ?: "",
            description = c.getString(2) ?: "",
            question = c.getString(3) ?: "",
            freqNum = c.getInt(4) ?: 1,
            freqDen = c.getInt(5) ?: 1,
            color = c.getInt(6) ?: 0,
            position = c.getInt(7) ?: 0,
            reminderHour = c.getInt(8),
            reminderMin = c.getInt(9),
            reminderDays = c.getInt(10) ?: 0,
            highlight = c.getInt(11) ?: 0,
            archived = c.getInt(12) ?: 0,
            type = c.getInt(13) ?: 0,
            targetValue = c.getDouble(14) ?: 0.0,
            targetType = c.getInt(15) ?: 0,
            unit = c.getString(16) ?: "",
            uuid = c.getString(17)
        )
    }
}
