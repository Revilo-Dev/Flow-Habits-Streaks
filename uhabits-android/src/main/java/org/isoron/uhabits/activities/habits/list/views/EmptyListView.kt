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

package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import org.isoron.uhabits.R
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.sres
import org.isoron.uhabits.utils.str

class EmptyListView(context: Context) : LinearLayout(context) {
    var textTextView: TextView
    var iconView: AppCompatImageView

    init {
        orientation = VERTICAL
        gravity = CENTER
        visibility = View.GONE

        iconView = AppCompatImageView(context).apply {
            setImageResource(R.drawable.flow_ic_calendar_task)
            imageTintList = ColorStateList.valueOf(sres.getColor(R.attr.flowTextSecondaryColor))
            contentDescription = null
        }

        addView(
            iconView,
            MATCH_PARENT,
            dp(48.0f).toInt()
        )

        textTextView = TextView(context).apply {
            text = str(R.string.no_habits_found)
            gravity = CENTER
            setPadding(0, dp(20.0f).toInt(), 0, 0)
            setTextColor(sres.getColor(R.attr.contrast60))
        }
        addView(
            textTextView,
            MATCH_PARENT,
            WRAP_CONTENT
        )
    }

    fun showDone() {
        visibility = VISIBLE
        iconView.setImageResource(R.drawable.checkbox_checked)
        textTextView.text = str(R.string.no_habits_left_to_do)
    }

    fun showEmpty() {
        visibility = VISIBLE
        iconView.setImageResource(R.drawable.flow_ic_calendar_task)
        textTextView.text = str(R.string.no_habits_found)
    }

    fun hide() {
        visibility = GONE
    }
}
