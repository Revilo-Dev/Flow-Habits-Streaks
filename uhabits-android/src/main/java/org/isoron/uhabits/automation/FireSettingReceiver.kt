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

package org.isoron.uhabits.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.tatarka.inject.annotations.Component
import org.isoron.platform.time.getToday
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.ui.widgets.WidgetBehavior
import org.isoron.uhabits.inject.HabitsApplicationComponent
import org.isoron.uhabits.receivers.ReceiverScope

const val ACTION_CHECK = 0
const val ACTION_UNCHECK = 1
const val ACTION_TOGGLE = 2
const val ACTION_INCREMENT = 3
const val ACTION_DECREMENT = 4

const val EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE"
const val EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB"

@ReceiverScope
@Component
internal abstract class FireSettingReceiverComponent(
    @Component val parent: HabitsApplicationComponent
) {
    abstract val widgetController: WidgetBehavior
}

class FireSettingReceiver : BroadcastReceiver() {

    private lateinit var allHabits: HabitList

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as HabitsApplication
        val component = FireSettingReceiverComponent::class.create(app.component)
        allHabits = app.component.habitList
        val args = SettingUtils.parseIntent(intent, allHabits) ?: return
        val today = getToday()
        val controller = component.widgetController

        when (args.action) {
            ACTION_CHECK -> controller.onAddRepetition(args.habit, today)
            ACTION_UNCHECK -> controller.onRemoveRepetition(args.habit, today)
            ACTION_TOGGLE -> controller.onToggleRepetition(args.habit, today)
            ACTION_INCREMENT -> controller.onIncrement(args.habit, today, 1000)
            ACTION_DECREMENT -> controller.onDecrement(args.habit, today, 1000)
        }
    }
}
