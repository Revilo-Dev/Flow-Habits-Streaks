package org.isoron.uhabits.core.database

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.TestDatabaseHelper
import org.isoron.platform.io.queryLong
import org.isoron.platform.io.run
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntryRepositoryTest {
    private fun insertTestHabit(db: org.isoron.platform.io.Database): Long {
        db.run(
            """
            insert into Habits(name, freq_num, freq_den, color, position, archived, type)
            values ('Test', 1, 1, 0, 0, 0, 0)
            """
        )
        return db.queryLong("select last_insert_rowid()")
    }

    @Test
    fun testInsertAndFindAll() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val repo = EntryRepository(db)
        val habitId = insertTestHabit(db)

        assertEquals(0, repo.findAllByHabitId(habitId).size)

        val d1 = EntryData(habitId = habitId, timestamp = 1700000000000, value = 2, notes = "")
        val d2 = EntryData(habitId = habitId, timestamp = 1700100000000, value = 2, notes = "good")
        val d3 = EntryData(habitId = habitId, timestamp = 1700200000000, value = 1, notes = "")
        repo.insert(d1)
        repo.insert(d2)
        repo.insert(d3)

        val all = repo.findAllByHabitId(habitId)
        assertEquals(3, all.size)
        assertEquals(1700200000000, all[0].timestamp)
        assertEquals(1700100000000, all[1].timestamp)
        assertEquals(1700000000000, all[2].timestamp)
        assertEquals("good", all[1].notes)

        db.close()
    }

    @Test
    fun testDeleteByHabitIdAndTimestamp() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val repo = EntryRepository(db)
        val habitId = insertTestHabit(db)

        repo.insert(EntryData(habitId = habitId, timestamp = 1000, value = 2))
        repo.insert(EntryData(habitId = habitId, timestamp = 2000, value = 2))
        repo.insert(EntryData(habitId = habitId, timestamp = 3000, value = 2))

        repo.deleteByHabitIdAndTimestamp(habitId, 2000)
        val remaining = repo.findAllByHabitId(habitId)
        assertEquals(2, remaining.size)
        assertTrue(remaining.none { it.timestamp == 2000L })

        db.close()
    }

    @Test
    fun testDeleteByHabitId() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val repo = EntryRepository(db)
        val habitA = insertTestHabit(db)
        val habitB = insertTestHabit(db)

        repo.insert(EntryData(habitId = habitA, timestamp = 1000, value = 2))
        repo.insert(EntryData(habitId = habitA, timestamp = 2000, value = 2))
        repo.insert(EntryData(habitId = habitB, timestamp = 3000, value = 2))

        repo.deleteByHabitId(habitA)
        assertEquals(0, repo.findAllByHabitId(habitA).size)
        assertEquals(1, repo.findAllByHabitId(habitB).size)

        db.close()
    }

    @Test
    fun testIsolationBetweenHabits() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val repo = EntryRepository(db)
        val habitA = insertTestHabit(db)
        val habitB = insertTestHabit(db)

        repo.insert(EntryData(habitId = habitA, timestamp = 1000, value = 2))
        repo.insert(EntryData(habitId = habitB, timestamp = 2000, value = 1))

        assertEquals(1, repo.findAllByHabitId(habitA).size)
        assertEquals(1, repo.findAllByHabitId(habitB).size)
        assertEquals(0, repo.findAllByHabitId(9999).size)

        db.close()
    }

    @Test
    fun testAllFieldsSurviveRoundTrip() = runTest {
        val db = TestDatabaseHelper.createEmptyDatabase()
        val repo = EntryRepository(db)

        db.run(
            """
            insert into Habits(name, freq_num, freq_den, color, position, archived, type)
            values ('Test', 1, 1, 0, 0, 0, 0)
            """
        )
        val habitId = db.queryLong("select last_insert_rowid()")

        val original = EntryData(
            habitId = habitId,
            timestamp = 1700000000000,
            value = 2,
            notes = "Felt great today"
        )
        original.id = repo.insert(original)

        val loaded = repo.findAllByHabitId(habitId).single()
        assertEquals(original.habitId, loaded.habitId)
        assertEquals(original.timestamp, loaded.timestamp)
        assertEquals(original.value, loaded.value)
        assertEquals(original.notes, loaded.notes)

        db.close()
    }
}
