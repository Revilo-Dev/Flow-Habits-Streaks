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

package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.text.format.DateFormat
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import org.isoron.platform.time.getToday
import org.isoron.platform.time.toGregorianCalendar
import org.isoron.uhabits.R
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache
import org.isoron.uhabits.core.utils.MidnightTimer
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.sres
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class FlowLargeHeaderView(
    context: Context,
    private val midnightTimer: MidnightTimer,
    preferences: Preferences
) : LinearLayout(context), MidnightTimer.MidnightListener {

    private val dateView = TextView(context).apply {
        setTextAppearance(R.style.TextAppearance_Flow_Supporting)
        setTextColor(sres.getColor(R.attr.flowTextSecondaryColor))
    }

    private val summaryView = TextView(context).apply {
        setTextAppearance(R.style.TextAppearance_Flow_Supporting)
        setTextColor(sres.getColor(R.attr.flowAccentColor))
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            topMargin = dim(R.dimen.flow_medium_spacing).toInt()
        }
    }

    private val perfectStreakView = FlowPerfectStreakView(context, preferences)

    init {
        orientation = VERTICAL
        setPadding(
            dim(R.dimen.flow_screen_padding).toInt(),
            dim(R.dimen.flow_header_top_spacing).toInt(),
            dim(R.dimen.flow_screen_padding).toInt(),
            dim(R.dimen.flow_header_bottom_spacing).toInt()
        )

        val titleView = TextView(context).apply {
            text = resources.getString(R.string.flow_app_title)
            setTextAppearance(R.style.TextAppearance_Flow_ScreenTitle)
        }
        ViewCompat.setAccessibilityHeading(titleView, true)

        addView(titleView, LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        addView(
            dateView,
            LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = dim(R.dimen.flow_small_spacing).toInt()
            }
        )
        addView(summaryView)
        addView(
            perfectStreakView,
            LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                topMargin = dim(R.dimen.flow_large_spacing).toInt()
            }
        )
        updateDate()
        updateSummary(
            HabitCardListCache.CompletionSummary(0, 0),
            HabitCardListCache.PerfectStreakSummary(0, List(7) { false })
        )
    }

    fun updateSummary(
        summary: HabitCardListCache.CompletionSummary,
        perfectStreak: HabitCardListCache.PerfectStreakSummary
    ) {
        summaryView.visibility = if (summary.total == 0) View.GONE else View.VISIBLE
        summaryView.text = resources.getQuantityString(
            R.plurals.flow_habits_completed_today,
            summary.total,
            summary.completed,
            summary.total
        )
        perfectStreakView.visibility = if (summary.total == 0) View.GONE else View.VISIBLE
        perfectStreakView.update(perfectStreak)
    }

    override fun atMidnight() {
        post { updateDate() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        midnightTimer.addListener(this)
        updateDate()
    }

    override fun onDetachedFromWindow() {
        midnightTimer.removeListener(this)
        super.onDetachedFromWindow()
    }

    private fun updateDate() {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEE d MMMM")
        val formatter = SimpleDateFormat(pattern, locale).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        dateView.text = formatter.format(getToday().toGregorianCalendar().time)
    }
}
