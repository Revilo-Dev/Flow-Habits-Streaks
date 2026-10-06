package org.isoron.uhabits.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.isoron.uhabits.HabitsApplication

class HabitCheckInDailyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as HabitsApplication).checkInManager.onDailyAlarm()
    }
}
