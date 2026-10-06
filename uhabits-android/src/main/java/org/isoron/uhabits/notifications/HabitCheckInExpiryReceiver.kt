package org.isoron.uhabits.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.isoron.uhabits.HabitsApplication

class HabitCheckInExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val occurrence = intent.getLongExtra("occurrence", 0)
        if (occurrence != 0L) {
            (context.applicationContext as HabitsApplication).checkInManager.expire(occurrence)
        }
    }
}
