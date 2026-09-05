/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 */

package org.isoron.uhabits.activities.habits.show.views

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.platform.time.getToday
import org.isoron.uhabits.R
import org.isoron.uhabits.core.ui.screens.habits.show.views.CurrentStreakCardState
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.sres
import java.util.Locale
import kotlin.math.absoluteValue

class FlowHabitStreakView(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {
    private val titleView = TextView(context).apply {
        setTextAppearance(R.style.TextAppearance_Flow_Body)
        setTypeface(typeface, Typeface.BOLD)
        text = resources.getString(R.string.flow_current_streak)
    }
    private val streakView = TextView(context).apply {
        setTextAppearance(R.style.TextAppearance_Flow_Supporting)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.END
    }
    private val messageView = TextView(context).apply {
        setTextAppearance(R.style.TextAppearance_Flow_Supporting)
    }
    private val dayLabels = mutableListOf<TextView>()
    private val dayMarks = mutableListOf<ImageView>()
    private val dayFormatter = JavaLocalDateFormatter(Locale.getDefault())

    init {
        orientation = VERTICAL
        val padding = dim(R.dimen.flow_large_spacing).toInt()
        setPadding(padding, padding, padding, padding)
        clipToOutline = true
        ViewCompat.setElevation(
            this,
            if (ColorUtils.calculateLuminance(sres.getColor(R.attr.flowBackgroundColor)) > 0.5) {
                dim(R.dimen.flow_card_elevation)
            } else {
                0f
            }
        )

        val heading = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(titleView, LayoutParams(0, WRAP_CONTENT, 1f))
            addView(streakView, LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
        addView(heading, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(
            messageView,
            LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = dim(R.dimen.flow_small_spacing).toInt()
            }
        )

        val week = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        repeat(7) {
            val dayLabel = TextView(context).apply {
                gravity = Gravity.CENTER
                setTextAppearance(R.style.TextAppearance_Flow_Metadata)
            }
            val dayMark = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER
            }
            val day = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                addView(dayLabel, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                addView(
                    dayMark,
                    LayoutParams(
                        dim(R.dimen.flow_perfect_day_size).toInt(),
                        dim(R.dimen.flow_perfect_day_size).toInt()
                    ).apply {
                        topMargin = dim(R.dimen.flow_small_spacing).toInt()
                    }
                )
            }
            week.addView(day, LayoutParams(0, WRAP_CONTENT, 1f))
            dayLabels.add(dayLabel)
            dayMarks.add(dayMark)
        }
        addView(
            week,
            LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = dim(R.dimen.flow_medium_spacing).toInt()
            }
        )
    }

    fun setState(state: CurrentStreakCardState) {
        val accent = sres.getColor(R.attr.flowAccentColor)
        val gradientEnd = sres.getColor(R.attr.flowAccentGradientEndColor)
        val surface = sres.getColor(R.attr.flowSurfaceSecondaryColor)
        val isLight = ColorUtils.calculateLuminance(sres.getColor(R.attr.flowBackgroundColor)) > 0.5
        val inactiveStart = if (isLight) {
            ColorUtils.blendARGB(surface, Color.BLACK, 0.52f)
        } else {
            ColorUtils.blendARGB(surface, Color.WHITE, 0.14f)
        }
        val inactiveEnd = if (isLight) {
            ColorUtils.blendARGB(surface, Color.BLACK, 0.64f)
        } else {
            ColorUtils.blendARGB(surface, Color.WHITE, 0.06f)
        }
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            if (state.todayComplete) intArrayOf(accent, gradientEnd) else intArrayOf(inactiveStart, inactiveEnd)
        ).apply {
            cornerRadius = dim(R.dimen.flow_card_radius)
        }

        titleView.setTextColor(Color.WHITE)
        streakView.setTextColor(Color.WHITE)
        messageView.setTextColor(Color.WHITE)
        streakView.text = when (state.currentStreak) {
            0 -> resources.getString(R.string.flow_perfect_streak_start)
            else -> resources.getString(R.string.flow_day_number, state.currentStreak)
        }
        messageView.text = if (state.todayComplete) {
            val messages = resources.getStringArray(R.array.flow_habit_streak_messages)
            messages[getToday().daysSince2000.absoluteValue % messages.size]
        } else {
            resources.getString(R.string.flow_habit_streak_incomplete_message)
        }

        val today = getToday()
        val recentCompletions = state.lastSevenDays.mapIndexed { index, complete ->
            today.minus(6 - index) to complete
        }.toMap()
        val weekStart = today.startOfWeek(state.firstWeekday)
        (0..6).map { offset ->
            val date = weekStart.plus(offset)
            date to (recentCompletions[date] ?: false)
        }.forEachIndexed { index, (date, complete) ->
            val weekday = dayFormatter.shortWeekdayName(date)
            dayLabels[index].apply {
                text = weekday.take(1).uppercase(Locale.getDefault())
                setTextColor(Color.WHITE)
            }
            dayMarks[index].apply {
                setImageResource(
                    if (complete) R.drawable.checkbox_checked else R.drawable.checkbox_unchecked
                )
                contentDescription = resources.getString(
                    if (complete) R.string.flow_day_completed else R.string.flow_day_not_completed,
                    weekday
                )
            }
        }
    }
}
