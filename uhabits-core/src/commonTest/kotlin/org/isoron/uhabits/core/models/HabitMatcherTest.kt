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
package org.isoron.uhabits.core.models

import org.isoron.uhabits.core.BaseUnitTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HabitMatcherTest : BaseUnitTest() {

    private fun buildHabit(
        name: String,
        question: String = "",
        description: String = ""
    ): Habit {
        val habit = modelFactory.buildHabit()
        habit.name = name
        habit.question = question
        habit.description = description
        return habit
    }

    @Test
    fun testSearch() {
        val yogaPractice = buildHabit("Yoga practice")
        val running = buildHabit("Running", question = "Did you run today?", description = "daily jog")
        val exercise = buildHabit("Exercise", question = "Did you do yoga today?")
        val mediter = buildHabit("Méditer", description = "mindfulness session")
        val stretching = buildHabit("🧘 Stretching")
        val run10k = buildHabit("10k Run")

        val habits = listOf(yogaPractice, running, exercise, mediter, stretching, run10k)

        // Match by name
        assertMatches(habits, "yoga", listOf(yogaPractice, exercise))

        // Match by question only
        assertMatches(habits, "run", listOf(running, run10k))

        // Match by description only
        assertMatches(habits, "jog", listOf(running))

        // Case-insensitive
        assertMatches(habits, "YOGA", listOf(yogaPractice, exercise))

        // Leading/trailing whitespace
        assertMatches(habits, "  yoga  ", listOf(yogaPractice, exercise))

        // Empty query matches all
        assertMatches(habits, "", listOf(yogaPractice, running, exercise, mediter, stretching, run10k))

        // Whitespace-only query matches all
        assertMatches(habits, "   ", listOf(yogaPractice, running, exercise, mediter, stretching, run10k))

        // Accented character
        assertMatches(habits, "méditer", listOf(mediter))

        // Case-insensitive accented
        assertMatches(habits, "MÉDITER", listOf(mediter))

        // Unaccented query does NOT match accented habit
        assertMatches(habits, "mediter", emptyList())

        // Emoji in habit name doesn't block text match
        assertMatches(habits, "stretching", listOf(stretching))

        // Numbers in name
        assertMatches(habits, "10k", listOf(run10k))

        // Single character
        assertMatches(habits, "y", listOf(yogaPractice, running, exercise))

        // Query longer than all fields
        assertMatches(habits, "a".repeat(100), emptyList())

        // No match
        assertMatches(habits, "swimming", emptyList())
    }

    private fun assertMatches(habits: List<Habit>, query: String, expected: List<Habit>) {
        val matcher = HabitMatcher(searchQuery = query)
        val actual = habits.filter(matcher::matches)
        assertEquals(expected.toSet(), actual.toSet(), "query '$query'")
    }
}
