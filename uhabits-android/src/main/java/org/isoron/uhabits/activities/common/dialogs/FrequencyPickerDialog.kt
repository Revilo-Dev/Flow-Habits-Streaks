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

package org.isoron.uhabits.activities.common.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import org.isoron.uhabits.R
import org.isoron.uhabits.databinding.FrequencyPickerDialogBinding

class FrequencyPickerDialog(
    var freqNumerator: Int,
    var freqDenominator: Int
) : AppCompatDialogFragment() {
    private var _binding: FrequencyPickerDialogBinding? = null
    private val binding get() = _binding!!

    var onFrequencyPicked: (num: Int, den: Int) -> Unit = { _, _ -> }

    constructor() : this(1, 1)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        freqNumerator = savedInstanceState?.getInt("numerator") ?: freqNumerator
        freqDenominator = savedInstanceState?.getInt("denominator") ?: freqDenominator
        _binding = FrequencyPickerDialogBinding.inflate(LayoutInflater.from(requireActivity()))

        addBeforeAfterText(
            this.getString(R.string.every_x_days),
            binding.everyXDaysContainer
        )

        addBeforeAfterText(
            this.getString(R.string.x_times_per_week),
            binding.xTimesPerWeekContainer
        )

        addBeforeAfterText(
            this.getString(R.string.x_times_per_month),
            binding.xTimesPerMonthContainer
        )

        addBeforeAfterText(
            this.getString(R.string.x_times_per_y_days),
            binding.xTimesPerYDaysContainer
        )

        binding.everyDayRadioButton.setOnClickListener {
            check(binding.everyDayRadioButton)
        }

        binding.everyXDaysRadioButton.setOnClickListener {
            check(binding.everyXDaysRadioButton)
            val everyXDaysTextView = binding.everyXDaysTextView
            selectInputField(everyXDaysTextView)
        }

        binding.everyXDaysTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.everyXDaysRadioButton)
        }

        binding.xTimesPerWeekRadioButton.setOnClickListener {
            check(binding.xTimesPerWeekRadioButton)
            selectInputField(binding.xTimesPerWeekTextView)
        }

        binding.xTimesPerWeekTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.xTimesPerWeekRadioButton)
        }

        binding.xTimesPerMonthRadioButton.setOnClickListener {
            check(binding.xTimesPerMonthRadioButton)
            selectInputField(binding.xTimesPerMonthTextView)
        }

        binding.xTimesPerMonthTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.xTimesPerMonthRadioButton)
        }

        binding.xTimesPerYDaysRadioButton.setOnClickListener {
            check(binding.xTimesPerYDaysRadioButton)
            selectInputField(binding.xTimesPerYDaysXTextView)
        }

        binding.xTimesPerYDaysXTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.xTimesPerYDaysRadioButton)
        }

        binding.xTimesPerYDaysYTextView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) check(binding.xTimesPerYDaysRadioButton)
        }

        populateViews()
        return AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        (requireDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { onSaveClicked() }
    }

    private fun addBeforeAfterText(
        str: String,
        container: LinearLayout
    ) {
        val parts = str.split("%d")
        for (i in parts.indices) {
            container.addView(
                TextView(activity).apply { text = parts[i].trim() },
                2 * i + 1
            )
        }
    }

    private fun onSaveClicked() {
        fun positiveValue(field: EditText, maximum: Int = Int.MAX_VALUE): Int? {
            val value = field.text.toString().trim().toIntOrNull()
            if (value == null || value !in 1..maximum) {
                field.error = getString(R.string.flow_invalid_frequency, maximum)
                field.requestFocus()
                return null
            }
            return value
        }
        var numerator = 1
        var denominator = 1
        when {
            binding.everyXDaysRadioButton.isChecked -> {
                denominator = positiveValue(binding.everyXDaysTextView) ?: return
            }
            binding.xTimesPerWeekRadioButton.isChecked -> {
                numerator = positiveValue(binding.xTimesPerWeekTextView, 7) ?: return
                denominator = 7
            }
            binding.xTimesPerMonthRadioButton.isChecked -> {
                numerator = positiveValue(binding.xTimesPerMonthTextView, 30) ?: return
                denominator = 30
            }
            binding.xTimesPerYDaysRadioButton.isChecked -> {
                denominator = positiveValue(binding.xTimesPerYDaysYTextView) ?: return
                numerator = positiveValue(binding.xTimesPerYDaysXTextView, denominator) ?: return
            }
        }
        if (numerator == denominator) {
            numerator = 1
            denominator = 1
        }
        onFrequencyPicked(numerator, denominator)
        dismiss()
    }

    private fun check(view: RadioButton) {
        uncheckAll()
        view.isChecked = true
        view.requestFocus()
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        state.putInt("numerator", freqNumerator)
        state.putInt("denominator", freqDenominator)
    }

    private fun populateViews() {
        uncheckAll()
        if (freqDenominator == 30 || freqDenominator == 31) {
            binding.xTimesPerMonthRadioButton.isChecked = true
            binding.xTimesPerMonthTextView.setText(freqNumerator.toString())
            selectInputField(binding.xTimesPerMonthTextView)
        } else {
            if (freqNumerator == 1) {
                if (freqDenominator == 1) {
                    binding.everyDayRadioButton.isChecked = true
                } else {
                    binding.everyXDaysRadioButton.isChecked = true
                    binding.everyXDaysTextView.setText(freqDenominator.toString())
                    selectInputField(binding.everyXDaysTextView)
                }
            } else {
                if (freqDenominator == 7) {
                    binding.xTimesPerWeekRadioButton.isChecked = true
                    binding.xTimesPerWeekTextView.setText(freqNumerator.toString())
                    selectInputField(binding.xTimesPerWeekTextView)
                } else {
                    binding.xTimesPerYDaysRadioButton.isChecked = true
                    binding.xTimesPerYDaysXTextView.setText(freqNumerator.toString())
                    binding.xTimesPerYDaysYTextView.setText(freqDenominator.toString())
                }
            }
        }
    }

    private fun selectInputField(view: EditText) {
        view.setSelection(view.text.length)
    }

    private fun uncheckAll() {
        binding.everyDayRadioButton.isChecked = false
        binding.everyXDaysRadioButton.isChecked = false
        binding.xTimesPerWeekRadioButton.isChecked = false
        binding.xTimesPerMonthRadioButton.isChecked = false
        binding.xTimesPerYDaysRadioButton.isChecked = false
    }
}
