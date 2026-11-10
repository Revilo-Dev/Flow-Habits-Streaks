package org.isoron.uhabits.activities.common.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.provider.Settings
import android.text.method.DigitsKeyListener
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatDialogFragment
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Entry
import org.isoron.uhabits.databinding.CheckmarkPopupBinding
import org.isoron.uhabits.utils.requestFocusWithKeyboard
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParsePosition
import kotlin.math.roundToInt

class NumberDialog : AppCompatDialogFragment() {
    var onToggle: ((Double, String) -> Unit)? = null
    var onDismiss: () -> Unit = {}
    private lateinit var view: CheckmarkPopupBinding
    private var committed = false
    private var restoreNotesFocus = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val preferences = (requireActivity().application as HabitsApplication).component.preferences
        view = CheckmarkPopupBinding.inflate(LayoutInflater.from(context))
        view.numberButtons.visibility = View.VISIBLE
        view.skipBtnNumber.visibility = if (preferences.isSkipEnabled) View.VISIBLE else View.GONE
        view.yesBtn.contentDescription = getString(R.string.flow_entry_save_value)
        view.noBtn.contentDescription = getString(R.string.flow_entry_save_zero)
        view.yesBtn.setText(R.string.save)
        view.noBtn.setText(R.string.flow_entry_zero)
        fixDecimalSeparator()
        view.notes.setText(
            savedInstanceState?.getString("draftNotes") ?: requireArguments().getString("notes", "")
        )
        val originalValue = requireArguments().getDouble("value")
        val initialValue = if (originalValue < 0 || originalValue == Entry.SKIP / 1000.0) {
            "0"
        } else {
            DecimalFormat("#.###").format(originalValue)
        }
        view.value.setText(savedInstanceState?.getString("draftValue") ?: initialValue)
        restoreNotesFocus = savedInstanceState?.getBoolean("notesFocused") == true
        view.yesBtn.setOnClickListener { save() }
        view.noBtn.setOnClickListener { commit(0.0) }
        view.skipBtnNumber.setOnClickListener { commit(Entry.SKIP / 1000.0) }
        view.dismissBtn.setOnClickListener { dismiss() }
        view.value.setOnEditorActionListener { _, action, event ->
            val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (action == EditorInfo.IME_ACTION_DONE || enter) {
                save()
                true
            } else {
                false
            }
        }
        return createEntryDialog(view)
    }

    override fun onStart() {
        super.onStart()
        configureEntryWindow()
        if (restoreNotesFocus) view.notes.requestFocusWithKeyboard() else view.value.requestFocusWithKeyboard()
    }

    private fun fixDecimalSeparator() {
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator
        view.value.keyListener = DigitsKeyListener.getInstance("0123456789$separator")
        val keyboard = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ).orEmpty()
        if (keyboard.contains("swiftkey") || keyboard.contains("samsung")) {
            view.value.inputType = EditorInfo.TYPE_CLASS_TEXT
        }
        view.value.imeOptions = EditorInfo.IME_ACTION_DONE
    }

    fun save() {
        val text = view.value.text.toString().trim()
        if (text.isEmpty()) {
            commit(Entry.UNKNOWN / 1000.0)
            return
        }
        val position = ParsePosition(0)
        val format = NumberFormat.getInstance().apply { isGroupingUsed = false }
        val value = format.parse(text, position)?.toDouble()
        if (
            value == null || position.index != text.length || !value.isFinite() ||
            value < 0 || value * 1000 > Int.MAX_VALUE
        ) {
            view.value.error = getString(R.string.flow_entry_invalid_number)
            return
        }
        commit(value)
    }

    private fun commit(value: Double) {
        if (committed) return
        val notes = view.notes.text.toString().trim()
        val callback = onToggle
        if (callback == null && !saveRestoredEntry((value * 1000).roundToInt(), notes)) return
        committed = true
        callback?.invoke(value, notes)
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::view.isInitialized) {
            outState.putString("draftNotes", view.notes.text.toString())
            outState.putString("draftValue", view.value.text.toString())
            outState.putBoolean("notesFocused", view.notes.hasFocus())
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss()
    }
}
