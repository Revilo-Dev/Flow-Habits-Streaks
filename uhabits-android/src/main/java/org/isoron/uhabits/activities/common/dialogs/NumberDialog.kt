package org.isoron.uhabits.activities.common.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.provider.Settings
import android.text.method.DigitsKeyListener
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
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
import kotlin.math.roundToInt

class NumberDialog : AppCompatDialogFragment() {

    var onToggle: ((Double, String) -> Unit)? = null
    var onDismiss: () -> Unit = {}

    private var originalNotes: String = ""
    private var originalValue: Double = 0.0
    private var valueEdited = false
    private lateinit var view: CheckmarkPopupBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val appComponent = (requireActivity().application as HabitsApplication).component
        val prefs = appComponent.preferences
        view = CheckmarkPopupBinding.inflate(LayoutInflater.from(context))
        view.prepareEntryActions(requireActivity(), requireArguments())
        if (!prefs.isSkipEnabled) view.skipBtnNumber.visibility = View.GONE
        view.valueLabel.visibility = View.VISIBLE
        view.numberButtons.visibility = View.VISIBLE
        view.numberFooter.visibility = View.VISIBLE
        fixDecimalSeparator(view)
        originalNotes = requireArguments().getString("notes")!!
        originalValue = requireArguments().getDouble("value")
        view.notes.setText(savedInstanceState?.getString("draftNotes") ?: originalNotes)
        view.value.setText(
            savedInstanceState?.getString("draftValue") ?: when {
                originalValue == Entry.UNKNOWN / 1000.0 || originalValue == Entry.SKIP / 1000.0 -> ""
                else -> DecimalFormat("#.##").format(originalValue)
            }
        )
        valueEdited = savedInstanceState?.getBoolean("valueEdited") ?: false
        view.value.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { valueEdited = true }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        view.value.setOnKeyListener { _, keyCode, event ->
            if (event.action == MotionEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                save()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
        view.saveBtn.setOnClickListener {
            save()
        }
        view.dismissBtn.setOnClickListener {
            requireDialog().dismiss()
        }
        view.skipBtnNumber.setOnClickListener {
            submit(Entry.SKIP.toDouble() / 1000, view.notes.text.toString().trim())
            requireDialog().dismiss()
        }

        view.notes.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                save()
                true
            } else {
                false
            }
        }
        view.value.requestFocusWithKeyboard()
        val dialog = Dialog(requireContext())
        dialog.setContentView(view.root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        onDismiss()
    }

    private fun fixDecimalSeparator(view: CheckmarkPopupBinding) {
        // Accept the decimal separator used by the current locale
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator
        view.value.keyListener = DigitsKeyListener.getInstance("0123456789$separator")

        // Some keyboards need text input to allow the local decimal separator
        val currKeyboard = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        if (currKeyboard.orEmpty().contains("swiftkey") || currKeyboard.orEmpty().contains("samsung")) {
            view.value.inputType = EditorInfo.TYPE_CLASS_TEXT
        }
    }

    override fun onStart() {
        super.onStart()
        requireDialog().prepareEntryWindow(requireActivity())
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        if (::view.isInitialized) {
            state.putString("draftNotes", view.notes.text.toString())
            state.putString("draftValue", view.value.text.toString())
            state.putBoolean("valueEdited", valueEdited)
        }
    }

    private fun submit(value: Double, notes: String) {
        val callback = onToggle
        if (callback != null) {
            callback(value, notes)
        } else {
            requireArguments().saveRestoredEntry(requireContext(), (value * 1000).roundToInt(), notes)
        }
    }

    fun save() {
        val text = view.value.text.toString().trim()
        val position = java.text.ParsePosition(0)
        val parsed = when {
            text.isEmpty() && !valueEdited && originalValue == Entry.SKIP / 1000.0 -> originalValue
            text.isEmpty() -> Entry.UNKNOWN / 1000.0
            else -> NumberFormat.getInstance().parse(text, position)?.toDouble()
        }
        if (parsed == null || !parsed.isFinite() ||
            (text.isNotEmpty() && (position.index != text.length || parsed < 0 || parsed > Int.MAX_VALUE / 1000.0))
        ) {
            view.value.error = getString(R.string.flow_invalid_number)
            view.value.requestFocus()
            return
        }
        submit(parsed, view.notes.text.toString().trim())
        requireDialog().dismiss()
    }
}
