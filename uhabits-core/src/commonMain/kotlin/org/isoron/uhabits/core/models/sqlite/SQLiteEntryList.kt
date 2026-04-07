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

package org.isoron.uhabits.core.models.sqlite

import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.database.EntryData
import org.isoron.uhabits.core.database.EntryRepository
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.EntryList
import org.isoron.uhabits.core.models.Frequency

class SQLiteEntryList(val repository: EntryRepository) : EntryList() {
    var habitId: Long? = null
    var isLoaded = false

    private fun loadRecords() {
        if (isLoaded) return
        val habitId = habitId ?: throw IllegalStateException("habitId must be set")
        val records = repository.findAllByHabitId(habitId)
        for (rec in records) {
            super.add(Entry(LocalDate.fromUnixTime(rec.timestamp), rec.value, rec.notes))
        }
        isLoaded = true
    }

    override fun get(date: LocalDate): Entry {
        loadRecords()
        return super.get(date)
    }

    override fun getByInterval(from: LocalDate, to: LocalDate): List<Entry> {
        loadRecords()
        return super.getByInterval(from, to)
    }

    override fun add(entry: Entry) {
        loadRecords()
        val habitId = habitId ?: throw IllegalStateException("habitId must be set")

        repository.deleteByHabitIdAndTimestamp(habitId, entry.date.unixTime)

        val data = EntryData(
            habitId = habitId,
            timestamp = entry.date.unixTime,
            value = entry.value,
            notes = entry.notes
        )
        repository.insert(data)

        super.add(entry)
    }

    override fun getKnown(): List<Entry> {
        loadRecords()
        return super.getKnown()
    }

    override fun recomputeFrom(originalEntries: EntryList, frequency: Frequency, isNumerical: Boolean) {
        throw UnsupportedOperationException()
    }

    override fun clear() {
        super.clear()
        repository.deleteByHabitId(habitId!!)
    }
}
