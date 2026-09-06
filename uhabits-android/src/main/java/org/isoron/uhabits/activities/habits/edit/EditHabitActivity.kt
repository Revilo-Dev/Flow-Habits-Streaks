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

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.icu.text.BreakIterator
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.text.Editable
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.DialogFragment
import com.android.datetimepicker.time.RadialPickerLayout
import com.android.datetimepicker.time.TimePickerDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.activities.common.dialogs.ColorPickerDialogFactory
import org.isoron.uhabits.activities.common.dialogs.FrequencyPickerDialog
import org.isoron.uhabits.activities.common.dialogs.WeekdayPickerDialog
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.commands.CreateHabitCommand
import org.isoron.uhabits.core.commands.EditHabitCommand
import org.isoron.uhabits.core.models.Frequency
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitType
import org.isoron.uhabits.core.models.NumericalHabitType
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.models.Reminder
import org.isoron.uhabits.core.models.WeekdayList
import org.isoron.uhabits.databinding.ActivityEditHabitBinding
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.dismissCurrentAndShow
import org.isoron.uhabits.utils.formatTime
import org.isoron.uhabits.utils.requestFocusWithKeyboard
import org.isoron.uhabits.utils.toFormattedString
import org.isoron.uhabits.utils.updateFlowStickyControls
import java.util.Locale

fun formatFrequency(freqNum: Int, freqDen: Int, resources: Resources) = when {
    freqNum == 1 && (freqDen == 30 || freqDen == 31) -> resources.getString(R.string.every_month)
    freqDen == 30 || freqDen == 31 -> resources.getString(R.string.x_times_per_month, freqNum)
    freqNum == 1 && freqDen == 1 -> resources.getString(R.string.every_day)
    freqNum == 1 && freqDen == 7 -> resources.getString(R.string.every_week)
    freqNum == 1 && freqDen > 1 -> resources.getString(R.string.every_x_days, freqDen)
    freqDen == 7 -> resources.getString(R.string.x_times_per_week, freqNum)
    else -> resources.getString(R.string.x_times_per_y_days, freqNum, freqDen)
}

class EditHabitActivity : AppCompatActivity() {

    private lateinit var themeSwitcher: AndroidThemeSwitcher
    private lateinit var binding: ActivityEditHabitBinding
    private lateinit var commandRunner: CommandRunner

    var habitId = -1L
    lateinit var habitType: HabitType
    var icon = ""
    var unit = ""
    var color = PaletteColor(11)
    var androidColor = 0
    var freqNum = 1
    var freqDen = 1
    var reminderHour = -1
    var reminderMin = -1
    var reminderDays: WeekdayList = WeekdayList.EVERY_DAY
    var targetType = NumericalHabitType.AT_LEAST

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        val component = (application as HabitsApplication).component
        themeSwitcher = AndroidThemeSwitcher(this, component.preferences)
        themeSwitcher.apply()

        binding = ActivityEditHabitBinding.inflate(layoutInflater)
        binding.root.applyRootViewInsets()
        binding.appBar.applyToolbarInsets()
        val baseActionMargin = resources.getDimensionPixelSize(R.dimen.flow_large_spacing)
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionBar) { actionBar, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            actionBar.layoutParams =
                (actionBar.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams).apply {
                    bottomMargin = baseActionMargin + systemBars.bottom
                }
            insets
        }
        setContentView(binding.root)

        if (intent.hasExtra("habitId")) {
            binding.collapsingToolbar.title = getString(R.string.edit_habit)
            habitId = intent.getLongExtra("habitId", -1)
            val habit = component.habitList.getById(habitId)!!
            habitType = habit.type
            color = habit.color
            freqNum = habit.frequency.numerator
            freqDen = habit.frequency.denominator
            targetType = habit.targetType
            icon = habit.icon
            habit.reminder?.let {
                reminderHour = it.hour
                reminderMin = it.minute
                reminderDays = it.days
            }
            binding.nameInput.setText(habit.name)
            binding.questionInput.setText(habit.question)
            binding.notesInput.setText(habit.description)
            binding.unitInput.setText(habit.unit)
            binding.targetInput.setText(habit.targetValue.toString())
        } else {
            binding.collapsingToolbar.title = getString(R.string.create_habit)
            habitType = HabitType.fromInt(intent.getIntExtra("habitType", HabitType.YES_NO.value))
        }

        if (state != null) {
            habitId = state.getLong("habitId")
            habitType = HabitType.fromInt(state.getInt("habitType"))
            color = PaletteColor(state.getInt("paletteColor"))
            freqNum = state.getInt("freqNum")
            freqDen = state.getInt("freqDen")
            reminderHour = state.getInt("reminderHour")
            reminderMin = state.getInt("reminderMin")
            reminderDays = WeekdayList(state.getInt("reminderDays"))
            icon = state.getString("icon", "")
        }

        updateColors()
        updateIconButton()

        when (habitType) {
            HabitType.YES_NO -> {
                binding.unitOuterBox.visibility = View.GONE
                binding.targetOuterBox.visibility = View.GONE
                binding.targetTypeOuterBox.visibility = View.GONE
            }
            HabitType.NUMERICAL -> {
                binding.nameInput.hint = getString(R.string.measurable_short_example)
                binding.questionInput.hint = getString(R.string.measurable_question_example)
                binding.frequencyOuterBox.visibility = View.GONE
            }
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""
        supportActionBar?.elevation = 0f
        binding.toolbar.setNavigationIcon(R.drawable.flow_ic_back)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.appBar.addOnOffsetChangedListener(
            com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener { bar, offset ->
                binding.toolbar.updateFlowStickyControls(kotlin.math.abs(offset) >= bar.totalScrollRange)
            }
        )

        binding.iconButton.setOnClickListener { showEmojiPicker() }

        val colorPickerDialogFactory = ColorPickerDialogFactory(this)
        binding.colorButton.setOnClickListener {
            val picker = colorPickerDialogFactory.create(color, themeSwitcher.currentTheme)
            picker.setListener { paletteColor ->
                this.color = paletteColor
                updateColors()
            }
            picker.dismissCurrentAndShow(supportFragmentManager, "colorPicker")
        }

        populateFrequency()
        binding.booleanFrequencyPicker.setOnClickListener {
            val picker = FrequencyPickerDialog(freqNum, freqDen)
            picker.onFrequencyPicked = { num, den ->
                freqNum = num
                freqDen = den
                populateFrequency()
            }
            picker.dismissCurrentAndShow(supportFragmentManager, "frequencyPicker")
        }

        populateTargetType()
        binding.targetTypePicker.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
            arrayAdapter.add(getString(R.string.target_type_at_least))
            arrayAdapter.add(getString(R.string.target_type_at_most))
            builder.setAdapter(arrayAdapter) { dialog, which ->
                targetType = when (which) {
                    0 -> NumericalHabitType.AT_LEAST
                    else -> NumericalHabitType.AT_MOST
                }
                populateTargetType()
                dialog.dismiss()
            }
            val dialog = builder.create()
            dialog.dismissCurrentAndShow()
        }

        binding.numericalFrequencyPicker.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
            arrayAdapter.add(getString(R.string.every_day))
            arrayAdapter.add(getString(R.string.every_week))
            arrayAdapter.add(getString(R.string.every_month))
            builder.setAdapter(arrayAdapter) { dialog, which ->
                freqDen = when (which) {
                    1 -> 7
                    2 -> 30
                    else -> 1
                }
                populateFrequency()
                dialog.dismiss()
            }
            builder.show()
        }

        populateReminder()
        binding.reminderTimePicker.setOnClickListener {
            val currentHour = if (reminderHour >= 0) reminderHour else 8
            val currentMin = if (reminderMin >= 0) reminderMin else 0
            val is24HourMode = DateFormat.is24HourFormat(this)
            val dialog = TimePickerDialog.newInstance(
                object : TimePickerDialog.OnTimeSetListener {
                    override fun onTimeSet(view: RadialPickerLayout?, hourOfDay: Int, minute: Int) {
                        reminderHour = hourOfDay
                        reminderMin = minute
                        populateReminder()
                    }

                    override fun onTimeCleared(view: RadialPickerLayout?) {
                        reminderHour = -1
                        reminderMin = -1
                        reminderDays = WeekdayList.EVERY_DAY
                        populateReminder()
                    }
                },
                currentHour,
                currentMin,
                is24HourMode,
                androidColor
            )
            dialog.dismissCurrentAndShow(supportFragmentManager, "timePicker")
        }

        binding.reminderDatePicker.setOnClickListener {
            val dialog = WeekdayPickerDialog()

            dialog.setListener { days: WeekdayList ->
                reminderDays = days
                if (reminderDays.isEmpty) reminderDays = WeekdayList.EVERY_DAY
                populateReminder()
            }
            dialog.setSelectedDays(reminderDays)
            dialog.dismissCurrentAndShow(supportFragmentManager, "dayPicker")
        }

        binding.buttonSave.setOnClickListener {
            if (validate()) save()
        }
        binding.buttonDiscard.setOnClickListener { finish() }

        for (fragment in supportFragmentManager.fragments) {
            (fragment as DialogFragment).dismiss()
        }
    }

    private fun save() {
        val component = (application as HabitsApplication).component
        val habit = component.modelFactory.buildHabit()

        var original: Habit? = null
        if (habitId >= 0) {
            original = component.habitList.getById(habitId)!!
            habit.copyFrom(original)
        }

        habit.name = binding.nameInput.text.trim().toString()
        habit.question = binding.questionInput.text.trim().toString()
        habit.description = binding.notesInput.text.trim().toString()
        habit.icon = icon.ifBlank { firstGrapheme(habit.name) }
        habit.color = color
        if (reminderHour >= 0) {
            habit.reminder = Reminder(reminderHour, reminderMin, reminderDays)
        } else {
            habit.reminder = null
        }

        habit.frequency = Frequency(freqNum, freqDen)
        if (habitType == HabitType.NUMERICAL) {
            habit.targetValue = binding.targetInput.text.toString().toDouble()
            habit.targetType = targetType
            habit.unit = binding.unitInput.text.trim().toString()
        }
        habit.type = habitType

        val command = if (habitId >= 0) {
            EditHabitCommand(
                component.habitList,
                habitId,
                habit
            )
        } else {
            CreateHabitCommand(
                component.modelFactory,
                component.habitList,
                habit
            )
        }
        component.commandRunner.run(command)
        finish()
    }

    private fun validate(): Boolean {
        var isValid = true
        if (binding.nameInput.text.isEmpty()) {
            binding.nameInput.error = getFormattedValidationError(R.string.validation_cannot_be_blank)
            isValid = false
        }
        if (habitType == HabitType.NUMERICAL) {
            if (binding.targetInput.text.isEmpty()) {
                binding.targetInput.error = getString(R.string.validation_cannot_be_blank)
                isValid = false
            }
        }
        return isValid
    }

    private fun populateReminder() {
        if (reminderHour < 0) {
            binding.reminderTimePicker.text = getString(R.string.reminder_off)
            binding.reminderDatePicker.visibility = View.GONE
            binding.reminderDivider.visibility = View.GONE
        } else {
            val time = formatTime(this, reminderHour, reminderMin)
            binding.reminderTimePicker.text = time
            binding.reminderDatePicker.visibility = View.VISIBLE
            binding.reminderDivider.visibility = View.VISIBLE
            binding.reminderDatePicker.text = reminderDays.toFormattedString(this)
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun populateFrequency() {
        binding.booleanFrequencyPicker.text = formatFrequency(freqNum, freqDen, resources)
        binding.numericalFrequencyPicker.text = when (freqDen) {
            1 -> getString(R.string.every_day)
            7 -> getString(R.string.every_week)
            30 -> getString(R.string.every_month)
            else -> "$freqNum/$freqDen"
        }
    }

    private fun populateTargetType() {
        binding.targetTypePicker.text = when (targetType) {
            NumericalHabitType.AT_MOST -> getString(R.string.target_type_at_most)
            else -> getString(R.string.target_type_at_least)
        }
    }

    private fun updateColors() {
        androidColor = themeSwitcher.currentTheme.color(color).toInt()
        binding.colorButton.backgroundTintList = ColorStateList.valueOf(androidColor)
        val flowBackground = StyledResources(this).getColor(R.attr.flowBackgroundColor)
        window.statusBarColor = flowBackground
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
        binding.bottomActionBar.cardElevation = if (ColorUtils.calculateLuminance(flowBackground) > 0.5) {
            resources.getDimension(R.dimen.flow_fab_elevation)
        } else {
            0f
        }
        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars =
            ColorUtils.calculateLuminance(flowBackground) > 0.5
    }

    private fun updateIconButton() {
        val defaultIcon = firstGrapheme(binding.nameInput.text.toString())
        binding.iconButton.text = when {
            icon.isNotBlank() -> getString(R.string.flow_change_emoji, icon)
            defaultIcon.isNotBlank() -> getString(R.string.flow_change_emoji, defaultIcon)
            else -> {
            getString(R.string.flow_choose_emoji)
            }
        }
    }

    private fun showEmojiPicker() {
        val smallSpacing = resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
        val largeSpacing = resources.getDimensionPixelSize(R.dimen.flow_large_spacing)
        val bodyPadding = resources.getDimensionPixelSize(R.dimen.flow_body_padding)
        val styledResources = StyledResources(this)
        val secondarySurface = styledResources.getColor(R.attr.flowSurfaceSecondaryColor)
        val secondaryText = styledResources.getColor(R.attr.flowTextSecondaryColor)
        val tertiaryText = styledResources.getColor(R.attr.flowTextTertiaryColor)
        val accent = styledResources.getColor(R.attr.flowAccentColor)

        val input = EditText(this).apply {
            gravity = Gravity.CENTER
            hint = "😀"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
            maxLines = 1
            textSize = 32f
            minHeight = resources.getDimensionPixelSize(R.dimen.flow_emoji_preview_height)
            setPadding(largeSpacing, smallSpacing, largeSpacing, smallSpacing)
            setTextColor(styledResources.getColor(R.attr.flowTextPrimaryColor))
            setHintTextColor(tertiaryText)
            setBackgroundResource(R.drawable.flow_surface_secondary_background)
            setText(icon.ifBlank { firstGrapheme(binding.nameInput.text.toString()) })
            setSelection(text.length)
        }
        input.addTextChangedListener(object : TextWatcher {
            private var updating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(editable: Editable?) {
                if (updating) return
                val singleCharacter = firstGrapheme(editable?.toString().orEmpty())
                if (editable?.toString() == singleCharacter) return
                updating = true
                input.setText(singleCharacter)
                input.setSelection(singleCharacter.length)
                updating = false
            }
        })
        val emojiKeyboardButton = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            contentDescription = getString(R.string.flow_open_emoji_keyboard)
            setIconResource(R.drawable.flow_ic_emoji)
            iconSize = resources.getDimensionPixelSize(R.dimen.flow_icon_size)
            minimumWidth = 0
            minWidth = 0
            minimumHeight = 0
            minHeight = 0
            insetTop = 0
            insetBottom = 0
            cornerRadius = resources.getDimensionPixelSize(R.dimen.flow_control_radius)
            backgroundTintList = ColorStateList.valueOf(secondarySurface)
            rippleColor = ColorStateList.valueOf(styledResources.getColor(R.attr.flowRippleColor))
            strokeWidth = 0
            setOnClickListener {
                input.requestFocus()
                input.requestFocusWithKeyboard()
            }
        }
        val inputRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            addView(input, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(
                emojiKeyboardButton,
                LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.flow_min_touch_target),
                    resources.getDimensionPixelSize(R.dimen.flow_min_touch_target)
                ).apply {
                    marginStart = smallSpacing
                }
            )
        }
        val dragHandle = View(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = resources.displayMetrics.density * 2
                setColor(ColorUtils.setAlphaComponent(tertiaryText, 150))
            }
        }
        val title = TextView(this).apply {
            text = getString(R.string.flow_emoji_picker_title)
            setTextColor(styledResources.getColor(R.attr.flowTextPrimaryColor))
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.flow_text_sheet_title)
            )
            setTypeface(typeface, Typeface.BOLD)
        }
        val hint = TextView(this).apply {
            text = getString(R.string.flow_emoji_picker_hint)
            setTextAppearance(R.style.TextAppearance_Flow_Supporting)
        }
        val suggestionsTitle = TextView(this).apply {
            text = getString(R.string.flow_suggested_icons)
            setTextAppearance(R.style.TextAppearance_Flow_SectionTitle)
        }
        val suggestions = GridLayout(this).apply {
            columnCount = 5
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        resources.getStringArray(R.array.flow_emoji_suggestions).forEach { emoji ->
            suggestions.addView(
                MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    gravity = Gravity.CENTER
                    text = emoji
                    textSize = 24f
                    isAllCaps = false
                    contentDescription = emoji
                    minimumWidth = 0
                    minWidth = 0
                    minimumHeight = 0
                    minHeight = 0
                    insetTop = 0
                    insetBottom = 0
                    cornerRadius = resources.getDimensionPixelSize(R.dimen.flow_control_radius)
                    backgroundTintList = ColorStateList.valueOf(secondarySurface)
                    rippleColor = ColorStateList.valueOf(styledResources.getColor(R.attr.flowRippleColor))
                    strokeWidth = 0
                    setOnClickListener {
                        input.setText(emoji)
                        input.setSelection(input.text.length)
                    }
                },
                GridLayout.LayoutParams().apply {
                    width = (resources.displayMetrics.density * 52).toInt()
                    height = (resources.displayMetrics.density * 52).toInt()
                    setMargins(smallSpacing / 2, smallSpacing / 2, smallSpacing / 2, smallSpacing / 2)
                }
            )
        }
        val removeButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle).apply {
            text = getString(R.string.flow_remove_icon)
            isAllCaps = false
            setTextColor(secondaryText)
            backgroundTintList = ColorStateList.valueOf(secondarySurface)
            rippleColor = ColorStateList.valueOf(styledResources.getColor(R.attr.flowRippleColor))
            cornerRadius = resources.getDimensionPixelSize(R.dimen.flow_control_radius)
        }
        val useButton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonStyle).apply {
            text = getString(R.string.flow_use_icon)
            isAllCaps = false
            setTextColor(styledResources.getColor(R.attr.flowOnAccentColor))
            backgroundTintList = ColorStateList.valueOf(accent)
            rippleColor = ColorStateList.valueOf(styledResources.getColor(R.attr.flowRippleColor))
            cornerRadius = resources.getDimensionPixelSize(R.dimen.flow_control_radius)
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(removeButton, LinearLayout.LayoutParams(0, resources.getDimensionPixelSize(R.dimen.flow_min_touch_target), 1f))
            addView(
                useButton,
                LinearLayout.LayoutParams(0, resources.getDimensionPixelSize(R.dimen.flow_min_touch_target), 1f).apply {
                    marginStart = smallSpacing
                }
            )
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(bodyPadding, smallSpacing, bodyPadding, bodyPadding)
            setBackgroundResource(R.drawable.flow_bottom_sheet_background)
            addView(
                dragHandle,
                LinearLayout.LayoutParams(
                    resources.displayMetrics.density.times(36).toInt(),
                    resources.displayMetrics.density.times(4).toInt()
                ).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = largeSpacing
                }
            )
            addView(title)
            addView(
                hint,
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = smallSpacing
                    bottomMargin = largeSpacing
                }
            )
            addView(inputRow, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(
                suggestionsTitle,
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = largeSpacing
                    bottomMargin = smallSpacing
                }
            )
            addView(
                suggestions,
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            )
            addView(
                actions,
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = largeSpacing
                }
            )
        }
        val dialog = BottomSheetDialog(this, R.style.FlowBottomSheet).apply {
            setContentView(content)
            setOnShowListener {
                window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
        useButton.setOnClickListener {
            val selectedIcon = firstGrapheme(input.text.toString())
            icon = selectedIcon
            updateIconButton()
            dialog.dismiss()
        }
        removeButton.setOnClickListener {
            icon = ""
            updateIconButton()
            dialog.dismiss()
        }
        dialog.dismissCurrentAndShow()
    }

    private fun firstGrapheme(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return ""
        val iterator = BreakIterator.getCharacterInstance(Locale.getDefault())
        iterator.setText(trimmed)
        val end = iterator.following(0)
        return if (end == BreakIterator.DONE) "" else trimmed.substring(0, end)
    }

    private fun getFormattedValidationError(@StringRes resId: Int): Spanned {
        val html = "<font color=#FFFFFF>${getString(resId)}</font>"
        return Html.fromHtml(html)
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        with(state) {
            putLong("habitId", habitId)
            putInt("habitType", habitType.value)
            putInt("paletteColor", color.paletteIndex)
            putInt("androidColor", androidColor)
            putInt("freqNum", freqNum)
            putInt("freqDen", freqDen)
            putInt("reminderHour", reminderHour)
            putInt("reminderMin", reminderMin)
            putInt("reminderDays", reminderDays.toInteger())
            putString("icon", icon)
        }
    }
}
