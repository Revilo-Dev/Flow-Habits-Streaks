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
import org.isoron.platform.io.Database
import org.isoron.platform.io.DatabaseOpener
import org.isoron.platform.io.UserFile
import org.isoron.platform.io.begin
import org.isoron.platform.io.commit
import org.isoron.platform.io.query
import org.isoron.platform.io.querySingle
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.ModelFactory
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.core.utils.isSQLite3File

/**
 * Class that imports database files exported by Rewire.
 */
@Inject
class RewireDBImporter(
    private val habitList: HabitList,
    private val modelFactory: ModelFactory,
    private val opener: DatabaseOpener
) : AbstractImporter() {

    override suspend fun canHandle(file: UserFile): Boolean {
        if (!isSQLite3File(file)) return false
        val db = opener.open(file.pathString)
        val count = db.querySingle(
            "select count(*) from SQLITE_MASTER where name='CHECKINS' or name='UNIT'"
        ) { it.getInt(0) }
        db.close()
        return count == 2
    }

    override suspend fun importHabitsFromFile(file: UserFile) {
        val db = opener.open(file.pathString)
        db.begin()
        createHabits(db)
        db.commit()
        db.close()
    }

    private fun createHabits(db: Database) {
        db.query(
            "select _id, name, description, schedule, " +
                "active_days, repeating_count, days, period " +
                "from habits"
        ) { stmt ->
            val id = stmt.getInt(0)
            val name = stmt.getText(1)
            val description = stmt.getTextOrNull(2) ?: ""
            val schedule = stmt.getInt(3)
            val activeDays = stmt.getTextOrNull(4)
            val repeatingCount = stmt.getInt(5)
            val days = stmt.getInt(6)
            val periodIndex = stmt.getInt(7)

            val habit = modelFactory.buildHabit()
            habit.name = name
            habit.description = description
            val periods = intArrayOf(7, 31, 365)
            var numerator: Int
            var denominator: Int
            when (schedule) {
                0 -> {
                    numerator = activeDays!!.split(",").toTypedArray().size
                    denominator = 7
                }
                1 -> {
                    numerator = days
                    denominator = periods[periodIndex]
                }
                2 -> {
                    numerator = 1
                    denominator = repeatingCount
                }
                else -> throw IllegalStateException()
            }
            habit.frequency = Frequency(numerator, denominator)
            habitList.add(habit)
            createReminder(db, habit, id)
            createCheckmarks(db, habit, id)
        }
    }

    private fun createCheckmarks(db: Database, habit: Habit, rewireHabitId: Int) {
        db.query(
            "select distinct date from checkins where habit_id=? and type=2",
            rewireHabitId.toString()
        ) { stmt ->
            val dateStr = stmt.getText(0)
            val year = dateStr.substring(0, 4).toInt()
            val month = dateStr.substring(4, 6).toInt()
            val day = dateStr.substring(6, 8).toInt()
            habit.originalEntries.add(Entry(LocalDate(year, month, day), Entry.YES_MANUAL))
        }
    }

    private fun createReminder(db: Database, habit: Habit, rewireHabitId: Int) {
        val reminder = db.querySingle(
            "select time, active_days from reminders where habit_id=? limit 1",
            rewireHabitId.toString()
        ) { stmt ->
            val rewireReminder = stmt.getInt(0)
            if (rewireReminder <= 0 || rewireReminder >= 1440) return@querySingle null
            val reminderDays = BooleanArray(7)
            val activeDaysStr = stmt.getText(1).split(",").toTypedArray()
            for (d in activeDaysStr) {
                val idx = (d.toInt() + 1) % 7
                reminderDays[idx] = true
            }
            val hour = rewireReminder / 60
            val minute = rewireReminder % 60
            Reminder(hour, minute, WeekdayList(reminderDays))
        }
        if (reminder != null) {
            habit.reminder = reminder
            habitList.update(habit)
        }
    }
}
