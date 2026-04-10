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
package org.isoron.uhabits.core.tasks

import org.isoron.platform.io.UserFile
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.io.HabitsCSVExporter
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList

class ExportCSVTask(
    private val habitList: HabitList,
    private val selectedHabits: List<Habit>,
    private val outputDir: UserFile,
    private val listener: ExportCSVListener
) : Task {
    private var archiveFilename: String? = null
    override suspend fun doInBackground() {
        try {
            val exporter = HabitsCSVExporter(habitList, selectedHabits)
            val bytes = exporter.writeArchive()
            val date = getToday().toCSVString()
            val zipFile = outputDir.resolve("Loop Habits CSV $date.zip")
            zipFile.writeBytes(bytes)
            archiveFilename = zipFile.pathString
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPostExecute() {
        listener.onExportCSVFinished(archiveFilename)
    }
}
