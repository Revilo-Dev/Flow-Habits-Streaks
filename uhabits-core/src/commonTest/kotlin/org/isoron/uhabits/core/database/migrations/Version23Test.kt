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

package org.isoron.uhabits.core.database.migrations

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.Database
import org.isoron.platform.io.format
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.query
import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals

class Version23Test : BaseUnitTest() {

    private lateinit var db: Database

    private suspend fun initDb() {
        db = openDatabaseResource("/databases/022.db")
    }

    private suspend fun migrateTo(version: Int) {
        db.migrateTo(version) { v ->
            val path = "migrations/${format("%02d.sql", v)}"
            fileOpener.openResourceFile(path).lines().joinToString("\n")
        }
    }

    private fun dbTest(block: suspend () -> Unit) = runTest {
        initDb()
        block()
    }

    @Test
    fun testMigrateTo23CreatesQuestionColumn() = dbTest {
        migrateTo(23)
        db.query("select question from Habits") {}
    }

    @Test
    fun testMigrateTo23MovesDescriptionToQuestionColumn() = dbTest {
        val descriptions = mutableListOf<String?>()
        db.query("select description from Habits") { stmt ->
            descriptions.add(stmt.getTextOrNull(0))
        }

        migrateTo(23)

        val questions = mutableListOf<String?>()
        db.query("select question from Habits") { stmt ->
            questions.add(stmt.getTextOrNull(0))
        }

        for (i in descriptions.indices) {
            assertEquals(descriptions[i], questions[i])
        }
    }

    @Test
    fun testMigrateTo23SetsDescriptionToNull() = dbTest {
        migrateTo(23)
        db.query("select description from Habits") { stmt ->
            assertEquals("", stmt.getTextOrNull(0))
        }
    }
}
