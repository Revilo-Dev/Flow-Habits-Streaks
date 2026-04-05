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

package org.isoron.uhabits.inject

import android.content.Context
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.activities.HabitsDirFinder
import org.isoron.uhabits.activities.common.dialogs.ColorPickerDialogFactory
import org.isoron.uhabits.activities.habits.list.ListHabitsMenu
import org.isoron.uhabits.activities.habits.list.ListHabitsModule
import org.isoron.uhabits.activities.habits.list.ListHabitsRootView
import org.isoron.uhabits.activities.habits.list.ListHabitsScreen
import org.isoron.uhabits.activities.habits.list.ListHabitsSelectionMenu
import org.isoron.uhabits.activities.habits.list.views.HabitCardListAdapter
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.ThemeSwitcher
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsBehavior
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsMenuBehavior
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsSelectionMenuBehavior

@ActivityScope
@Component
abstract class HabitsActivityComponent(
    @Component val parent: HabitsApplicationComponent,
    @get:Provides @get:ActivityContext
    val activityContext: Context
) {
    abstract val colorPickerDialogFactory: ColorPickerDialogFactory
    abstract val habitCardListAdapter: HabitCardListAdapter
    abstract val listHabitsBehavior: ListHabitsBehavior
    abstract val listHabitsMenu: ListHabitsMenu
    abstract val listHabitsRootView: ListHabitsRootView
    abstract val listHabitsScreen: ListHabitsScreen
    abstract val listHabitsSelectionMenu: ListHabitsSelectionMenu
    abstract val themeSwitcher: ThemeSwitcher

    @ActivityScope
    @Provides
    open fun themeSwitcher(
        @ActivityContext context: Context,
        prefs: Preferences
    ): ThemeSwitcher = AndroidThemeSwitcher(context, prefs)

    open val HabitCardListAdapter.bindAdapter: ListHabitsMenuBehavior.Adapter
        @Provides get() = this

    open val ListHabitsModule.bindBugReporter: ListHabitsBehavior.BugReporter
        @Provides get() = this

    open val ListHabitsScreen.bindMenuScreen: ListHabitsMenuBehavior.Screen
        @Provides get() = this

    open val ListHabitsScreen.bindScreen: ListHabitsBehavior.Screen
        @Provides get() = this

    open val HabitCardListAdapter.bindSelMenuAdapter: ListHabitsSelectionMenuBehavior.Adapter
        @Provides get() = this

    open val ListHabitsScreen.bindSelMenuScreen: ListHabitsSelectionMenuBehavior.Screen
        @Provides get() = this

    open val HabitsDirFinder.bindDirFinder: ListHabitsBehavior.DirFinder
        @Provides get() = this
}
