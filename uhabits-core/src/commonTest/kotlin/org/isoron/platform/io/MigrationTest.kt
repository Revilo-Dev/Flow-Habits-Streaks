package org.isoron.platform.io

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MigrationTest {
    @Test
    fun testMigrateFromScratch() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        assertEquals(27, db.getVersion())

        db.run(
            """
            insert into Habits(name, freq_num, freq_den, color, position, archived, type)
            values ('Test', 1, 1, 0, 0, 0, 0)
            """
        )
        db.run(
            """
            insert into Repetitions(habit, timestamp, value)
            values (1, 1000000, 2)
            """
        )

        val stmt = db.prepareStatement("select name, icon, skips_count_towards_streaks from Habits where id = 1")
        assertEquals(StepResult.ROW, stmt.step())
        assertEquals("Test", stmt.getText(0))
        assertEquals("", stmt.getText(1))
        assertEquals(0, stmt.getInt(2))
        stmt.finalize()

        db.close()
    }

    @Test
    fun testMigrateIdempotent() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val version = db.getVersion()
        db.migrateTo(version) { v -> TestDatabaseHelper.loadMigrationSQL(v) }
        assertEquals(version, db.getVersion())
        db.close()
    }

    @Test
    fun legacyHabitsKeepTheirDataAndDefaultToUncountedSkips() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        db.run("ALTER TABLE Habits DROP COLUMN skips_count_towards_streaks")
        db.setVersion(26)
        db.run("INSERT INTO Habits(name, icon) VALUES ('Walking', 'W')")

        db.migrateTo(27) { version -> TestDatabaseHelper.loadMigrationSQL(version) }

        val stmt = db.prepareStatement("SELECT name, icon, skips_count_towards_streaks FROM Habits")
        assertEquals(StepResult.ROW, stmt.step())
        assertEquals("Walking", stmt.getText(0))
        assertEquals("W", stmt.getText(1))
        assertEquals(0, stmt.getInt(2))
        stmt.finalize()
        db.close()
    }
}
