package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.platform.time.DayOfWeek
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Streak
import org.isoron.uhabits.core.models.StreakList
import org.isoron.uhabits.core.models.memory.MemoryModelFactory
import org.isoron.uhabits.core.ui.screens.habits.show.views.CurrentStreakCardPresenter

data class HabitStreakProgress(val habit: Habit, val current: Int, val streaks: List<Streak>) {
    val longest: Int get() = streaks.maxOfOrNull { it.length } ?: 0
}

data class StreakProgress(
    val today: LocalDate,
    val firstDate: LocalDate,
    val perfectDays: Set<LocalDate>,
    val streaks: List<Streak>,
    val habits: List<HabitStreakProgress>
) {
    val current: Streak? get() = streaks.firstOrNull()?.takeIf { it.end >= today.minus(1) }
    val longest: Int get() = streaks.maxOfOrNull { it.length } ?: 0
    val previous: List<Streak> get() = streaks.filter { it != current }

    fun toCardHabit(): Habit = MemoryModelFactory().buildHabit().apply {
        // Supply an unsaved daily series to the shared read only habit cards
        color = PaletteColor(11)
        for (day in firstDate.daysSince2000..today.daysSince2000) {
            val date = LocalDate(day)
            computedEntries.add(Entry(date, if (date in perfectDays) Entry.YES_MANUAL else Entry.NO))
        }
    }

    companion object {
        fun build(
            allHabits: Iterable<Habit>,
            firstWeekday: DayOfWeek,
            countSkips: Boolean = false
        ): StreakProgress {
            val today = getToday()
            val habits = allHabits.filter { !it.isArchived }
            val firstDate = habits.mapNotNull { it.computedEntries.getKnown().lastOrNull()?.date }
                .minOrNull()?.coerceAtMost(today) ?: today
            val perfectDays = mutableSetOf<LocalDate>()
            val streaks = mutableListOf<Streak>()
            var start: LocalDate? = null
            for (day in firstDate.daysSince2000..today.daysSince2000) {
                val date = LocalDate(day)
                if (PerfectDays.isPerfect(habits, date, countSkips)) {
                    perfectDays.add(date)
                    if (start == null) start = date
                } else if (start != null) {
                    streaks.add(Streak(start, date.minus(1)))
                    start = null
                }
            }
            if (start != null) streaks.add(Streak(start, today))
            val habitProgress = habits.map { habit ->
                val history = StreakList().apply {
                    recompute(
                        habit.computedEntries,
                        firstDate,
                        today,
                        habit.isNumerical,
                        habit.targetValue,
                        habit.targetType,
                        habit.skipsCountTowardsStreaks
                    )
                }.getBest(Int.MAX_VALUE)
                HabitStreakProgress(
                    habit,
                    CurrentStreakCardPresenter.buildState(habit, firstWeekday).currentStreak,
                    history
                )
            }
            return StreakProgress(today, firstDate, perfectDays, streaks.asReversed(), habitProgress)
        }
    }
}
