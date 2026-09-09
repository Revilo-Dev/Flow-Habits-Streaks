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
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatDialogFragment
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Entry.Companion.NO
import org.isoron.uhabits.core.models.Entry.Companion.SKIP
import org.isoron.uhabits.core.models.Entry.Companion.YES_MANUAL
import org.isoron.uhabits.databinding.CheckmarkPopupBinding
import org.isoron.uhabits.utils.InterfaceUtils.getFontAwesome
import org.isoron.uhabits.utils.sres

class CheckmarkDialog : AppCompatDialogFragment() {
    var onToggle: (Int, String) -> Unit = { _, _ -> }
    var onDismiss: () -> Unit = {}

    private var originalNotes: String = ""
    private var originalValue: Int = 0
    private lateinit var view: CheckmarkPopupBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val appComponent = (requireActivity().application as HabitsApplication).component
        val prefs = appComponent.preferences
        view = CheckmarkPopupBinding.inflate(LayoutInflater.from(context))
        val color = requireArguments().getInt("color")
        arrayOf(view.yesBtn).forEach {
            it.setTextColor(color)
        }
        arrayOf(view.noBtn).forEach {
            it.setTextColor(view.root.sres.getColor(R.attr.contrast60))
        }
        arrayOf(view.yesBtn, view.noBtn).forEach {
            it.typeface = getFontAwesome(requireContext())
        }
        originalNotes = requireArguments().getString("notes")!!
        originalValue = requireArguments().getInt("value")
        view.notes.setText(originalNotes)
        view.booleanButtons.visibility = VISIBLE
        view.numberFooter.visibility = VISIBLE
        view.skipBtnNumber.visibility = if (prefs.isSkipEnabled) VISIBLE else GONE
        val dialog = Dialog(requireContext())
        dialog.setContentView(view.root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        fun onClick(v: Int) {
            val notes = view.notes.text.toString().trim()
            onToggle(v, notes)
            requireDialog().dismiss()
        }
        var selectedValue = originalValue
        fun select(value: Int) {
            selectedValue = value
            view.yesBtn.alpha = if (value == YES_MANUAL) 1f else 0.45f
            view.noBtn.alpha = if (value == NO) 1f else 0.45f
        }
        select(originalValue)
        view.yesBtn.setOnClickListener { select(YES_MANUAL) }
        view.noBtn.setOnClickListener { select(NO) }
        view.dismissBtn.setOnClickListener { requireDialog().dismiss() }
        view.skipBtnNumber.setOnClickListener { onClick(SKIP) }
        view.saveBtn.setOnClickListener { onClick(selectedValue) }
        view.notes.setOnEditorActionListener { v, actionId, event ->
            onClick(selectedValue)
            true
        }

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        onDismiss()
    }
}
