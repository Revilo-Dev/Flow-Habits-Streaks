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

import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.isoron.platform.io.Database
import org.isoron.platform.io.JavaFileOpener
import org.isoron.platform.io.migrateTo
import org.isoron.platform.io.query
import org.isoron.uhabits.core.JvmBaseUnitTest
import org.junit.Test

class Version23Test : JvmBaseUnitTest() {

    private lateinit var db: Database

    override fun setUp() {
        super.setUp()
        db = openDatabaseResource("/databases/022.db")
    }

    private val fileOpener = JavaFileOpener()

    private fun migrateTo(version: Int) = runBlocking {
        db.migrateTo(version) { v ->
            val path = "migrations/%02d.sql".format(v)
            fileOpener.openResourceFile(path).lines().joinToString("\n")
        }
    }

    @Test
    fun `test migrate to 23 creates question column`() {
        migrateTo(23)
        db.query("select question from Habits") {}
    }

    @Test
    fun `test migrate to 23 moves description to question column`() {
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
            assertThat(questions[i], equalTo(descriptions[i]))
        }
    }

    @Test
    fun `test migrate to 23 sets description to null`() {
        migrateTo(23)
        db.query("select description from Habits") { stmt ->
            assertThat(stmt.getTextOrNull(0), equalTo(""))
        }
    }
}
