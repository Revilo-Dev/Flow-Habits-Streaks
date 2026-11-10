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
import org.isoron.uhabits.core.models.Entry.Companion.NO
import org.isoron.uhabits.core.models.Entry.Companion.SKIP
import org.isoron.uhabits.core.models.Entry.Companion.YES_MANUAL
import org.isoron.uhabits.databinding.CheckmarkPopupBinding

class CheckmarkDialog : AppCompatDialogFragment() {
    var onToggle: ((Int, String) -> Unit)? = null
    var onDismiss: () -> Unit = {}
    private lateinit var view: CheckmarkPopupBinding
    private var committed = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val preferences = (requireActivity().application as HabitsApplication).component.preferences
        view = CheckmarkPopupBinding.inflate(LayoutInflater.from(context))
        view.notes.setText(
            savedInstanceState?.getString("draftNotes") ?: requireArguments().getString("notes", "")
        )
        view.skipBtnNumber.visibility = if (preferences.isSkipEnabled) VISIBLE else GONE
        view.yesBtn.setOnClickListener { commit(YES_MANUAL) }
        view.noBtn.setOnClickListener { commit(NO) }
        view.skipBtnNumber.setOnClickListener { commit(SKIP) }
        view.dismissBtn.setOnClickListener { dismiss() }
        return createEntryDialog(view)
    }

    override fun onStart() {
        super.onStart()
        configureEntryWindow()
    }

    private fun commit(value: Int) {
        if (committed) return
        val notes = view.notes.text.toString().trim()
        val callback = onToggle
        if (callback == null && !saveRestoredEntry(value, notes)) return
        committed = true
        callback?.invoke(value, notes)
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::view.isInitialized) outState.putString("draftNotes", view.notes.text.toString())
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss()
    }
}
