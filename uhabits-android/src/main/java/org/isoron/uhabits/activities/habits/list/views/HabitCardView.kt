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
import android.graphics.PointF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.text.LineBreaker.BREAK_STRATEGY_BALANCED
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import me.tatarka.inject.annotations.Inject
import org.isoron.platform.gui.toInt
import org.isoron.platform.time.LocalDate
import org.isoron.platform.time.getToday
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.RingView
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsBehavior
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.utils.currentTheme
import org.isoron.uhabits.utils.sres
import java.util.Locale

@Inject
class HabitCardViewFactory(
    @ActivityContext val context: Context,
    private val checkmarkPanelFactory: CheckmarkPanelViewFactory,
    private val numberPanelFactory: NumberPanelViewFactory,
    private val behavior: ListHabitsBehavior
) {
    fun create() = HabitCardView(context, checkmarkPanelFactory, numberPanelFactory, behavior)
}

class HabitCardView(
    @ActivityContext context: Context,
    checkmarkPanelFactory: CheckmarkPanelViewFactory,
    numberPanelFactory: NumberPanelViewFactory,
    private val behavior: ListHabitsBehavior
) : FrameLayout(context),
    ModelObservable.Listener {

    var buttonCount
        get() = checkmarkPanel.buttonCount
        set(value) {
            checkmarkPanel.buttonCount = value
            numberPanel.buttonCount = value
        }

    var dataOffset = 0
        set(value) {
            field = value
            checkmarkPanel.dataOffset = value
            numberPanel.dataOffset = value
        }

    var habit: Habit? = null
        set(newHabit) {
            if (isAttachedToWindow) {
                field?.observable?.removeListener(this)
                newHabit?.observable?.addListener(this)
            }
            field = newHabit
            if (newHabit != null) copyAttributesFrom(newHabit)
        }

    var score
        get() = scoreRing.getPercentage().toDouble()
        set(value) {
            scoreRing.setPercentage(value.toFloat())
            scoreRing.setPrecision(1.0f / 16)
        }

    var currentStreak: Int = 0
        set(value) {
            field = value
            updateStreakLabel()
        }

    var isSelectionMode: Boolean = false
        set(value) {
            field = value
            updateSelectionVisuals(isSelected)
        }

    var unit
        get() = numberPanel.units
        set(value) {
            numberPanel.units = value
        }

    var values
        get() = checkmarkPanel.values
        set(values) {
            checkmarkPanel.values = values
            numberPanel.values = values.map { it / 1000.0 }.toDoubleArray()
        }

    var threshold: Double
        get() = numberPanel.threshold
        set(value) {
            numberPanel.threshold = value
        }

    var notes
        get() = checkmarkPanel.notes
        set(values) {
            checkmarkPanel.notes = values
            numberPanel.notes = values
        }

    var checkmarkPanel: CheckmarkPanelView
    private var habitInitial: TextView
    private var numberPanel: NumberPanelView
    private var innerFrame: LinearLayout
    private var label: TextView
    private var streakLabel: TextView
    private var scoreRing: RingView
    private var selectionCheck: ImageView
    private var dragHandle: ImageView

    private var currentToggleTaskId = 0

    init {
        scoreRing = RingView(context).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            setThickness(resources.getDimension(R.dimen.flow_icon_ring_thickness))
            setIsTransparencyEnabled(true)
        }

        habitInitial = TextView(context).apply {
            gravity = Gravity.CENTER
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.flow_text_supporting)
            )
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        selectionCheck = ImageView(context).apply {
            val size = resources.getDimensionPixelSize(R.dimen.flow_icon_container_size)
            layoutParams = FrameLayout.LayoutParams(
                size,
                size,
                Gravity.CENTER
            )
            contentDescription = resources.getString(R.string.flow_selected)
            setImageResource(R.drawable.flow_ic_select_check)
            setColorFilter(sres.getColor(R.attr.flowOnAccentColor))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(sres.getColor(R.attr.flowAccentColor))
            }
            setPadding(
                resources.getDimensionPixelSize(R.dimen.flow_medium_spacing),
                resources.getDimensionPixelSize(R.dimen.flow_medium_spacing),
                resources.getDimensionPixelSize(R.dimen.flow_medium_spacing),
                resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
            )
            visibility = GONE
        }

        val habitIcon = FrameLayout(context).apply {
            val size = resources.getDimensionPixelSize(R.dimen.flow_icon_container_size)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                val margin = resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
                marginStart = margin
                marginEnd = resources.getDimensionPixelSize(R.dimen.flow_card_spacing)
                gravity = Gravity.CENTER_VERTICAL
            }
            addView(habitInitial)
            addView(scoreRing)
            addView(selectionCheck)
        }

        label = TextView(context).apply {
            setTextAppearance(R.style.TextAppearance_Flow_Body)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            if (SDK_INT >= Build.VERSION_CODES.Q) {
                breakStrategy = BREAK_STRATEGY_BALANCED
            }
        }

        streakLabel = TextView(context).apply {
            setTextAppearance(R.style.TextAppearance_Flow_Supporting)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        val labelContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
            }
            addView(label, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            addView(
                streakLabel,
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.flow_small_spacing)
                }
            )
        }

        checkmarkPanel = checkmarkPanelFactory.create().apply {
            onToggle = { date, value, notes ->
                triggerRipple(date)
                val location = getAbsoluteButtonLocation(date)
                habit?.let {
                    behavior.onToggle(
                        it,
                        date,
                        value,
                        notes,
                        location.x,
                        location.y
                    )
                }
            }
            onEdit = { date ->
                triggerRipple(date)
                val location = getAbsoluteButtonLocation(date)
                habit?.let { behavior.onEdit(it, date, location.x, location.y) }
            }
        }

        numberPanel = numberPanelFactory.create().apply {
            visibility = GONE
            onEdit = { date ->
                triggerRipple(date)
                val location = getAbsoluteButtonLocation(date)
                habit?.let { behavior.onEdit(it, date, location.x, location.y) }
            }
        }

        dragHandle = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.flow_min_touch_target),
                resources.getDimensionPixelSize(R.dimen.flow_min_touch_target),
                Gravity.END or Gravity.CENTER_VERTICAL
            ).apply {
                marginEnd = -resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
            }
            contentDescription = resources.getString(R.string.flow_reorder_habit)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.flow_medium_spacing),
                resources.getDimensionPixelSize(R.dimen.flow_medium_spacing),
                resources.getDimensionPixelSize(R.dimen.flow_medium_spacing),
                resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
            )
            setImageResource(R.drawable.drag)
            visibility = GONE
        }

        innerFrame = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            minimumHeight = resources.getDimensionPixelSize(R.dimen.flow_row_height)
            clipToOutline = true
            val verticalPadding = resources.getDimensionPixelSize(R.dimen.flow_medium_spacing)
            setPadding(0, verticalPadding, 0, verticalPadding)

            addView(habitIcon)
            addView(labelContainer)
            addView(checkmarkPanel)
            addView(numberPanel)

            setOnTouchListener { v, event ->
                v.background.setHotspot(event.x, event.y)
                false
            }
        }

        clipToPadding = false
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        val horizontalMargin = resources.getDimensionPixelSize(R.dimen.flow_body_padding)
        val verticalMargin = resources.getDimensionPixelSize(R.dimen.flow_card_spacing) / 2
        setPadding(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin)
        addView(innerFrame)
        addView(dragHandle)
        updateBackground(false)
    }

    override fun onModelChange() {
        Handler(Looper.getMainLooper()).post {
            habit?.let { copyAttributesFrom(it) }
        }
    }

    override fun setSelected(isSelected: Boolean) {
        super.setSelected(isSelected)
        updateBackground(isSelected)
        updateSelectionVisuals(isSelected)
    }

    fun triggerRipple(date: LocalDate) {
        val location = getRelativeButtonLocation(date)
        triggerRipple(location.x, location.y)
    }

    private fun getRelativeButtonLocation(date: LocalDate): PointF {
        val today = getToday()
        val offset = date.daysUntil(today) - dataOffset
        val panel = when (habit!!.isNumerical) {
            true -> numberPanel
            false -> checkmarkPanel
        }
        val button = panel.buttons[offset]
        val y = button.height / 2.0f
        val x = panel.x + button.x + (button.width / 2).toFloat()
        return PointF(x, y)
    }

    private fun getAbsoluteButtonLocation(date: LocalDate): PointF {
        val containerLocation = IntArray(2)
        this.getLocationInWindow(containerLocation)
        val relButtonLocation = getRelativeButtonLocation(date)
        val windowInsets = rootWindowInsets
        val xInset = windowInsets?.displayCutout?.safeInsetLeft ?: 0
        val yInset = if (SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            windowInsets?.systemWindowInsetTop ?: 0
        } else {
            0
        }
        return PointF(
            containerLocation[0].toFloat() + relButtonLocation.x - xInset,
            containerLocation[1].toFloat() + relButtonLocation.y - yInset
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        habit?.observable?.addListener(this)
    }

    override fun onDetachedFromWindow() {
        habit?.observable?.removeListener(this)
        super.onDetachedFromWindow()
    }

    private fun copyAttributesFrom(h: Habit) {
        fun getActiveColor(habit: Habit): Int {
            return when (habit.isArchived) {
                true -> sres.getColor(R.attr.contrast60)
                false -> currentTheme().color(habit.color).toInt()
            }
        }

        val c = getActiveColor(h)
        val labelColorAttr = if (h.isArchived) {
            R.attr.flowTextSecondaryColor
        } else {
            R.attr.flowTextPrimaryColor
        }
        label.apply {
            text = h.name
            setTextColor(sres.getColor(labelColorAttr))
        }
        habitInitial.apply {
            val hasIcon = h.icon.isNotBlank()
            text = if (hasIcon) h.icon else h.name.trim().take(1).uppercase(Locale.getDefault())
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(
                    if (hasIcon) R.dimen.flow_text_emoji else R.dimen.flow_text_supporting
                )
            )
            setTypeface(Typeface.DEFAULT, if (hasIcon) Typeface.NORMAL else Typeface.BOLD)
            setTextColor(c)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ColorUtils.setAlphaComponent(c, 38))
            }
            ViewCompat.setElevation(
                this,
                if (ColorUtils.calculateLuminance(sres.getColor(R.attr.flowBackgroundColor)) > 0.5) {
                    resources.getDimension(R.dimen.flow_card_elevation)
                } else {
                    0f
                }
            )
        }
        val streakColorAttr = if (h.isArchived) {
            R.attr.flowTextTertiaryColor
        } else {
            R.attr.flowTextSecondaryColor
        }
        streakLabel.setTextColor(sres.getColor(streakColorAttr))
        scoreRing.apply {
            setColor(c)
        }
        updateStreakLabel()
        checkmarkPanel.apply {
            color = c
            visibility = when (h.isNumerical) {
                true -> View.GONE
                false -> View.VISIBLE
            }
        }
        numberPanel.apply {
            color = c
            units = h.unit
            targetType = h.targetType
            threshold = h.targetValue
            visibility = when (h.isNumerical) {
                true -> View.VISIBLE
                false -> View.GONE
            }
        }
        updateSelectionVisuals(isSelected)
    }

    private fun triggerRipple(x: Float, y: Float) {
        val background = innerFrame.background
        background.setHotspot(x, y)
        background.state = intArrayOf(
            android.R.attr.state_pressed,
            android.R.attr.state_enabled
        )
        Handler().postDelayed({ background.state = intArrayOf() }, 25)
    }

    private fun updateBackground(isSelected: Boolean) {
        val background = when (isSelected) {
            true -> R.drawable.flow_card_selected_background
            false -> R.drawable.flow_card_selectable_background
        }
        innerFrame.setBackgroundResource(background)
    }

    private fun updateSelectionVisuals(isSelected: Boolean) {
        val numerical = habit?.isNumerical == true
        val selectionActive = isSelectionMode
        checkmarkPanel.visibility = if (!selectionActive && !numerical) VISIBLE else GONE
        numberPanel.visibility = if (!selectionActive && numerical) VISIBLE else GONE
        dragHandle.visibility = if (selectionActive) VISIBLE else GONE
        streakLabel.visibility = if (selectionActive) GONE else VISIBLE

        val showSelectionCheck = selectionActive && isSelected
        if (showSelectionCheck) {
            if (selectionCheck.visibility != VISIBLE) {
                habitInitial.animate().cancel()
                scoreRing.animate().cancel()
                habitInitial.animate().alpha(0f).scaleX(0.82f).scaleY(0.82f).setDuration(110).start()
                scoreRing.animate().alpha(0f).scaleX(0.82f).scaleY(0.82f).setDuration(110).start()
                selectionCheck.apply {
                    visibility = VISIBLE
                    alpha = 0f
                    scaleX = 0.74f
                    scaleY = 0.74f
                    animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(170).start()
                }
            }
        } else {
            selectionCheck.animate().cancel()
            selectionCheck.visibility = GONE
            habitInitial.animate().cancel()
            scoreRing.animate().cancel()
            habitInitial.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(110).start()
            scoreRing.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(110).start()
        }
    }

    private fun updateStreakLabel() {
        streakLabel.text = resources.getString(R.string.flow_day_number, currentStreak)
    }

    companion object {
        fun (() -> Unit).delay(delayInMillis: Long) {
            Handler(Looper.getMainLooper()).postDelayed(this, delayInMillis)
        }
    }
}
