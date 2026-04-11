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
package org.isoron.uhabits

import android.content.Context
import kotlinx.coroutines.Dispatchers
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.tasks.CoroutineTaskRunner
import org.isoron.uhabits.core.tasks.TaskRunner
import org.isoron.uhabits.inject.AppContext
import org.isoron.uhabits.inject.HabitsApplicationComponent
import org.isoron.uhabits.intents.IntentScheduler
import java.io.File

@AppScope
@Component
abstract class HabitsApplicationTestComponent(
    @get:Provides @get:AppContext
    appContext: Context,
    @get:Provides dbFile: File
) : HabitsApplicationComponent(appContext, dbFile) {

    abstract val intentScheduler: IntentScheduler?

    @AppScope
    @Provides
    override fun taskRunner(): TaskRunner = CoroutineTaskRunner(
        mainDispatcher = Dispatchers.Main,
        ioDispatcher = Dispatchers.IO
    )
}
