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

package org.isoron.uhabits.activities.habits.edit

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.ViewCompat
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.dialogs.configureFlowDialogBlur
import org.isoron.uhabits.activities.common.dialogs.configureHabitDialogSize
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.databinding.SelectHabitTypeBinding
import org.isoron.uhabits.intents.IntentFactory

class HabitTypeDialog : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = SelectHabitTypeBinding.inflate(LayoutInflater.from(requireActivity()))
        ViewCompat.setAccessibilityHeading(binding.title, true)

        binding.buttonYesNo.setOnClickListener {
            val intent = IntentFactory().startEditActivity(requireActivity(), HabitType.YES_NO.value)
            startActivity(intent)
            dismiss()
        }

        binding.buttonMeasurable.setOnClickListener {
            val intent = IntentFactory().startEditActivity(requireActivity(), HabitType.NUMERICAL.value)
            startActivity(intent)
            dismiss()
        }

        binding.buttonCancel.setOnClickListener { dismiss() }

        return Dialog(requireContext(), R.style.FlowEntryDialog).apply {
            setContentView(binding.root)
            setCanceledOnTouchOutside(true)
            configureFlowDialogBlur(requireActivity().findViewById(android.R.id.content))
            window?.setWindowAnimations(R.style.Animation_Flow_NewHabit)
        }
    }

    override fun onStart() {
        configureHabitDialogSize(centered = false)
        super.onStart()
        (activity as? ListHabitsActivity)?.rootView?.setHabitTypeDialogVisible(true)
    }

    override fun onDismiss(dialog: DialogInterface) {
        (activity as? ListHabitsActivity)?.rootView?.setHabitTypeDialogVisible(false)
        super.onDismiss(dialog)
    }
}
