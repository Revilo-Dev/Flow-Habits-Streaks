package org.isoron.uhabits.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.DateUtils
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.NumericalHabitType
import java.util.Calendar

/** A daily check-in notification for active habits. */
class HabitCheckInManager(
    private val context: Context,
    private val habits: HabitList,
    private val commands: CommandRunner
) : CommandRunner.Listener {
    private val prefs = context.getSharedPreferences("habit_check_in", Context.MODE_PRIVATE)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)

    fun start() {
        commands.addListener(this)
        configure()
    }

    fun stop() = commands.removeListener(this)

    @Synchronized
    fun configure() {
        alarms.cancel(dailyIntent())
        if (!settings.getBoolean(KEY_ENABLED, false)) {
            finish()
            return
        }
        scheduleDaily()
        refresh()
    }

    @Synchronized
    fun onDailyAlarm() {
        if (!settings.getBoolean(KEY_ENABLED, false)) return
        showNow()
        scheduleDaily()
    }

    @Synchronized
    fun showNow() {
        if (!settings.getBoolean(KEY_ENABLED, false)) return
        val reminderTime = System.currentTimeMillis()
        val due = dueHabits()
        if (due.isEmpty()) return

        val current = prefs.getLong(KEY_OCCURRENCE, 0)
        if (current != reminderTime) {
            cancelExpiry()
            prefs.edit().putLong(KEY_OCCURRENCE, reminderTime).remove(KEY_COMPLETION_END).apply()
            Log.i(TAG, "Starting check-in")
        }
        refresh()
    }

    @Synchronized
    fun expire(occurrence: Long) {
        if (prefs.getLong(KEY_OCCURRENCE, 0) == occurrence) finish()
    }

    override fun onCommandFinished(command: Command) = refresh()

    @Synchronized
    fun refresh() {
        if (!settings.getBoolean(KEY_ENABLED, false)) {
            finish()
            return
        }
        val occurrence = prefs.getLong(KEY_OCCURRENCE, 0)
        if (occurrence == 0L) return
        val now = System.currentTimeMillis()
        val completionEnd = prefs.getLong(KEY_COMPLETION_END, 0)
        if (completionEnd != 0L) {
            if (now >= completionEnd) finish() else scheduleExpiry(occurrence, completionEnd)
            return
        }
        if (now >= occurrence + WINDOW_MILLIS || now < occurrence - 60_000) {
            finish()
            return
        }
        val date = localDate(occurrence)
        val due = dueHabits()
        val done = due.count { completed(it, date) }
        if (due.isEmpty()) {
            finish()
            return
        }
        if (done == due.size) {
            showCompleted(occurrence, due.size)
            return
        }
        scheduleExpiry(occurrence, occurrence + WINDOW_MILLIS)
        if (!canPost()) return
        createChannel()
        val remaining = due.size - done
        val title = when (Calendar.getInstance().apply { timeInMillis = occurrence }.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> context.getString(R.string.flow_checkin_morning)
            in 12..16 -> context.getString(R.string.flow_checkin_afternoon)
            else -> context.getString(R.string.flow_checkin_evening)
        }
        val launch = PendingIntent.getActivity(
            context,
            0,
            Intent(context, ListHabitsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.flow_checkin_progress, done, due.size))
            .setSubText(context.resources.getQuantityString(R.plurals.flow_checkin_remaining, remaining, remaining))
            .setContentIntent(launch)
            .addAction(R.drawable.ic_action_check, context.getString(R.string.flow_checkin_open), launch)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setProgress(due.size, done, false)

        if (Build.VERSION.SDK_INT >= 36) {
            // Native ProgressStyle is required for the Android 16 Live Update presentation.
            val promotion = Bundle().apply { putBoolean("android.requestPromotedOngoing", true) }
            val native = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.flow_checkin_progress, done, due.size))
                .setSubText(context.resources.getQuantityString(R.plurals.flow_checkin_remaining, remaining, remaining))
                .setContentIntent(launch)
                .addAction(Notification.Action.Builder(null, context.getString(R.string.flow_checkin_open), launch).build())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setShortCriticalText(context.getString(R.string.flow_checkin_chip))
                .setStyle(Notification.ProgressStyle().setProgressSegments(
                    listOf(Notification.ProgressStyle.Segment(due.size))
                ).setProgress(done))
                .addExtras(promotion)
                .build()
            notifications.notify(NOTIFICATION_ID, native)
        } else {
            notifications.notify(NOTIFICATION_ID, builder.build())
        }
        Log.d(TAG, "Check-in updated: $done/${due.size}")
    }

    private fun dueHabits(): List<Habit> = habits.filter { !it.isArchived }

    private fun dailyIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, HabitCheckInDailyReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun scheduleDaily() {
        val minuteOfDay = settings.getInt(KEY_TIME, DEFAULT_TIME)
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val intent = dailyIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
        } else {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
            } catch (e: SecurityException) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
            }
        }
    }

    private fun localDate(occurrence: Long): LocalDate =
        LocalDate.fromUnixTime(DateUtils.removeTimezone(occurrence))

    private fun completed(habit: Habit, date: LocalDate): Boolean {
        val entry = habit.computedEntries.get(date)
        return if (!habit.isNumerical) {
            entry.value == org.isoron.uhabits.core.models.Entry.YES_MANUAL ||
                entry.value == org.isoron.uhabits.core.models.Entry.YES_AUTO
        } else if (habit.targetType == NumericalHabitType.AT_MOST) {
            entry.value != org.isoron.uhabits.core.models.Entry.UNKNOWN
        } else {
            entry.value / 1000.0 >= habit.targetValue
        }
    }

    private fun canPost() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun showCompleted(occurrence: Long, total: Int) {
        val end = System.currentTimeMillis() + COMPLETION_DISPLAY_MILLIS
        prefs.edit().putLong(KEY_COMPLETION_END, end).apply()
        scheduleExpiry(occurrence, end)
        if (!canPost()) return
        createChannel()
        val launch = PendingIntent.getActivity(
            context, 0, Intent(context, ListHabitsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        notifications.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.flow_checkin_complete))
                .setContentText(context.getString(R.string.flow_checkin_progress, total, total))
                .setContentIntent(launch)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setProgress(total, total, false)
                .build()
        )
        Log.i(TAG, "Check-in complete")
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.flow_checkin_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.flow_checkin_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
        notifications.createNotificationChannel(channel)
    }

    private fun expiryIntent(occurrence: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, HabitCheckInExpiryReceiver::class.java).putExtra(KEY_OCCURRENCE, occurrence),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun scheduleExpiry(occurrence: Long, time: Long) {
        val intent = expiryIntent(occurrence)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarms.canScheduleExactAlarms()) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
        } else {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
            } catch (e: SecurityException) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, intent)
            }
        }
    }

    private fun cancelExpiry() {
        alarms.cancel(expiryIntent(0))
    }

    private fun finish() {
        notifications.cancel(NOTIFICATION_ID)
        cancelExpiry()
        prefs.edit().remove(KEY_OCCURRENCE).remove(KEY_COMPLETION_END).apply()
        Log.i(TAG, "Ending check-in")
    }

    companion object {
        private const val TAG = "FlowCheckIn"
        private const val CHANNEL_ID = "FLOW_CHECK_IN"
        private const val NOTIFICATION_ID = 0x464C4F57
        private const val KEY_OCCURRENCE = "occurrence"
        private const val KEY_COMPLETION_END = "completion_end"
        private const val WINDOW_MILLIS = 60 * 60 * 1000L
        private const val COMPLETION_DISPLAY_MILLIS = 10_000L
        const val KEY_ENABLED = "pref_nowbar_enabled"
        const val KEY_TIME = "pref_nowbar_time"
        const val DEFAULT_TIME = 9 * 60
    }
}
