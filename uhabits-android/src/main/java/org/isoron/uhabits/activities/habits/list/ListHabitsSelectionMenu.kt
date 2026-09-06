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

package org.isoron.uhabits.activities.habits.list

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import me.tatarka.inject.annotations.Inject
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.views.HabitCardListAdapter
import org.isoron.uhabits.activities.habits.list.views.FlowSelectionActionBar
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsSelectionMenuBehavior
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.inject.ActivityScope

@Inject
@ActivityScope
class ListHabitsSelectionMenu(
    @ActivityContext context: Context,
    private val listAdapter: HabitCardListAdapter,
    private val behavior: ListHabitsSelectionMenuBehavior
) {

    val activity = (context as AppCompatActivity)

    fun onSelectionStart() {
        updateActions()
    }

    fun onSelectionChange() {
        updateActions()
    }

    fun onSelectionFinish() {
        selectionActions()?.hide()
        reorderPrompt()?.visibility = View.GONE
        rootView()?.alignSelectionControls(false)
    }

    private fun updateActions() {
        if (listAdapter.isSelectionEmpty) {
            onSelectionFinish()
            return
        }
        selectionActions()?.apply {
            setActions(
                canEdit = behavior.canEdit(),
                canArchive = behavior.canArchive(),
                canUnarchive = behavior.canUnarchive(),
                onEdit = behavior::onEditHabits,
                onColor = behavior::onChangeColor,
                onArchive = behavior::onArchiveHabits,
                onUnarchive = behavior::onUnarchiveHabits,
                onDelete = behavior::onDeleteHabits
            )
            show()
        }
        rootView()?.alignSelectionControls(true)
        reorderPrompt()?.visibility = View.VISIBLE
    }

    private fun selectionActions(): FlowSelectionActionBar? {
        return activity.findViewById(R.id.flowSelectionActions)
    }

    private fun reorderPrompt(): TextView? {
        return activity.findViewById(R.id.flowReorderPrompt)
    }

    private fun rootView(): ListHabitsRootView? =
        (activity as? ListHabitsActivity)?.rootView
}
