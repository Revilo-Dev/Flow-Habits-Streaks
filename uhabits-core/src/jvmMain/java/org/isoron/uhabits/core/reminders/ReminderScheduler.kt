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
package org.isoron.uhabits.core.reminders

import me.tatarka.inject.annotations.Inject
import org.isoron.platform.time.DateUtils
import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.commands.ChangeHabitColorCommand
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.commands.DeleteHabitsCommand
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.preferences.WidgetPreferences
import java.util.Calendar
import java.util.TimeZone

@AppScope
@Inject
open class ReminderScheduler(
    private val commandRunner: CommandRunner,
    private val habitList: HabitList,
    private val sys: SystemScheduler,
    private val widgetPreferences: WidgetPreferences
) : CommandRunner.Listener {
    @Synchronized
    override fun onCommandFinished(command: Command) {
        if (command is CreateRepetitionCommand) return
        if (command is ChangeHabitColorCommand) return
        if (command is DeleteHabitsCommand) command.selected.forEach { sys.cancelShowReminder(it) }
        scheduleAll()
    }

    @Synchronized
    open fun schedule(habit: Habit) {
        if (habit.id == null) {
            sys.log("ReminderScheduler", "Habit has null id. Returning.")
            return
        }
        if (!habit.hasReminder()) {
            sys.cancelShowReminder(habit)
            sys.log("ReminderScheduler", "habit=" + habit.id + " has no reminder. Skipping.")
            return
        }
        if (habit.isArchived) {
            sys.cancelShowReminder(habit)
            return
        }
        val reminder = habit.reminder!!
        val tz = DateUtils.fixedTimeZone ?: TimeZone.getDefault()
        val now = DateUtils.applyTimezone(DateUtils.getLocalTime(tz), tz)
        val calendar = Calendar.getInstance(tz).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val days = reminder.days.toArray()
        if (days.none { it }) {
            sys.cancelShowReminder(habit)
            return
        }
        for (offset in 0..7) {
            val weekday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
            if (calendar.timeInMillis > now && days[weekday]) break
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        var reminderTime = calendar.timeInMillis
        val snoozeReminderTime = widgetPreferences.getSnoozeTime(habit.id!!)
        if (snoozeReminderTime != 0L) {
            val now = DateUtils.applyTimezone(DateUtils.getLocalTime())
            sys.log("ReminderScheduler", "Habit ${habit.id} has been snoozed until $snoozeReminderTime")
            if (snoozeReminderTime > now) {
                sys.log("ReminderScheduler", "Snooze time is in the future. Accepting.")
                reminderTime = snoozeReminderTime
            } else {
                sys.log("ReminderScheduler", "Snooze time is in the past. Discarding.")
                widgetPreferences.removeSnoozeTime(habit.id!!)
            }
        }
        scheduleAtTime(habit, reminderTime)
    }

    @Synchronized
    open fun scheduleAtTime(habit: Habit, reminderTime: Long) {
        sys.log("ReminderScheduler", "Scheduling alarm for habit=" + habit.id)
        if (!habit.hasReminder()) {
            sys.log("ReminderScheduler", "habit=" + habit.id + " has no reminder. Skipping.")
            return
        }
        if (habit.isArchived) {
            sys.log("ReminderScheduler", "habit=" + habit.id + " is archived. Skipping.")
            return
        }
        val timestamp = DateUtils.getStartOfDayWithOffset(DateUtils.removeTimezone(reminderTime), 0, 0)
        sys.log("ReminderScheduler", "reminderTime=$reminderTime removeTimezone=${DateUtils.removeTimezone(reminderTime)} timestamp=$timestamp")
        sys.scheduleShowReminder(reminderTime, habit, timestamp)
    }

    @Synchronized
    open fun scheduleAll() {
        sys.log("ReminderScheduler", "Scheduling all alarms")
        for (habit in habitList) schedule(habit)
    }

    @Synchronized
    open fun hasHabitsWithReminders(): Boolean {
        return !habitList.getFiltered(HabitMatcher.WITH_ALARM).isEmpty
    }

    @Synchronized
    open fun startListening() {
        commandRunner.addListener(this)
    }

    @Synchronized
    open fun stopListening() {
        commandRunner.removeListener(this)
    }

    @Synchronized
    open fun snoozeReminder(habit: Habit, minutes: Long) {
        val now = DateUtils.applyTimezone(DateUtils.getLocalTime())
        val snoozedUntil = now + minutes * 60 * 1000
        widgetPreferences.setSnoozeTime(habit.id!!, snoozedUntil)
        schedule(habit)
    }

    interface SystemScheduler {
        fun cancelShowReminder(habit: Habit) {}
        fun scheduleShowReminder(
            reminderTime: Long,
            habit: Habit,
            timestamp: Long
        ): SchedulerResult

        fun scheduleWidgetUpdate(updateTime: Long): SchedulerResult?
        fun log(componentName: String, msg: String)
    }

    enum class SchedulerResult {
        IGNORED, OK
    }
}
