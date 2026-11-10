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
package org.isoron.uhabits.activities.habits.show.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import org.isoron.platform.gui.toInt
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.uhabits.core.ui.screens.habits.show.views.BarCardPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.views.BarCardState
import org.isoron.uhabits.core.ui.views.BarChart
import org.isoron.uhabits.databinding.ShowHabitBarBinding
import org.isoron.uhabits.utils.styleFlowPopup
import java.util.Locale

class BarCardView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private var binding = ShowHabitBarBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.boolSpinner.styleFlowPopup()
        binding.numericalSpinner.styleFlowPopup()
    }

    fun setState(state: BarCardState) {
        val androidColor = state.theme.color(state.color).toInt()
        binding.chart.view = BarChart(state.theme, JavaLocalDateFormatter(Locale.getDefault())).apply {
            series = mutableListOf(state.entries.map { it.value / 1000.0 })
            colors = mutableListOf(theme.color(state.color.paletteIndex))
            axis = state.entries.map { it.date }
        }
        binding.chart.resetDataOffset()
        binding.chart.postInvalidate()

        binding.title.setTextColor(androidColor)
        binding.title.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(androidColor)
        if (state.isNumerical) {
            binding.boolSpinner.visibility = GONE
        } else {
            binding.numericalSpinner.visibility = GONE
        }

        binding.numericalSpinner.setSelection(state.numericalSpinnerPosition)
        binding.boolSpinner.setSelection(state.boolSpinnerPosition)
    }

    fun setListener(presenter: BarCardPresenter) {
        binding.boolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                presenter.onBoolSpinnerPosition(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        binding.numericalSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    presenter.onNumericalSpinnerPosition(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
    }

    fun setOnBucketSelected(listener: (Int) -> Unit) {
        binding.boolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                listener(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }
}
