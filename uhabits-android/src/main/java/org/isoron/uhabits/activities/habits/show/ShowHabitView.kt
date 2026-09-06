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

package org.isoron.uhabits.activities.habits.show

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.AppBarLayout
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.core.ui.screens.habits.show.ShowHabitPresenter
import org.isoron.uhabits.core.ui.screens.habits.show.ShowHabitState
import org.isoron.uhabits.databinding.ShowHabitBinding
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.dim
import org.isoron.uhabits.utils.setupToolbar
import org.isoron.uhabits.utils.sres
import org.isoron.uhabits.utils.updateFlowStickyControls
import kotlin.math.abs

class ShowHabitView(context: Context) : FrameLayout(context) {
    private val binding = ShowHabitBinding.inflate(LayoutInflater.from(context))

    init {
        addView(binding.root)
        binding.appBar.applyToolbarInsets()
        val contentTop = resources.getDimensionPixelSize(R.dimen.flow_large_spacing)
        val contentBottom = resources.getDimensionPixelSize(R.dimen.flow_large_spacing)
        ViewCompat.setOnApplyWindowInsetsListener(binding.linearLayout) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(0, contentTop, 0, contentBottom + maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        binding.appBar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBar, verticalOffset ->
                val isSticky = abs(verticalOffset) >= appBar.totalScrollRange
                binding.largeHeader.visibility = if (isSticky) {
                    INVISIBLE
                } else {
                    VISIBLE
                }
                binding.toolbar.updateFlowStickyControls(isSticky)
            }
        )
    }

    fun setState(data: ShowHabitState) {
        val backgroundColor = sres.getColor(R.attr.flowBackgroundColor)
        val habitColor = data.theme.color(data.color).toInt()
        val isLight = ColorUtils.calculateLuminance(backgroundColor) > 0.5
        setupToolbar(
            binding.toolbar,
            title = "",
            color = data.color,
            theme = data.theme,
            applyTopInset = false
        )
        binding.collapsingToolbar.title = ""
        binding.collapsingToolbar.setContentScrimColor(Color.TRANSPARENT)
        binding.collapsingToolbar.setStatusBarScrimColor(Color.TRANSPARENT)
        binding.toolbar.background = ColorDrawable(Color.TRANSPARENT)
        binding.toolbar.elevation = 0f
        binding.toolbar.overflowIcon = AppCompatResources.getDrawable(context, R.drawable.more)
        binding.toolbar.setNavigationOnClickListener { (context as Activity).finish() }
        binding.habitTitle.text = data.title
        binding.habitIcon.apply {
            text = data.icon.ifBlank { data.title.take(1).uppercase() }
            setTextColor(habitColor)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ColorUtils.setAlphaComponent(habitColor, if (isLight) 32 else 64))
            }
        }
        val window = (context as Activity).window
        window.statusBarColor = backgroundColor
        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars = isLight

        binding.subtitleCard.setState(data.subtitle)
        binding.overviewCard.setState(data.overview)
        binding.notesCard.setState(data.notes)
        binding.targetCard.setState(data.target)
        binding.currentStreakCard.setState(data.currentStreak)
        binding.bestStreakCard.setState(data.bestStreaks)
        binding.scoreCard.setState(data.scores)
        binding.frequencyCard.setState(data.frequency)
        binding.historyCard.setState(data.history)
        binding.barCard.setState(data.bar)
        binding.overviewCard.visibility = if (data.isNumerical) GONE else VISIBLE
        binding.targetCard.visibility = if (data.isNumerical) VISIBLE else GONE

        val expandedHeight = if (data.isNumerical) {
            R.dimen.flow_show_habit_header_numerical_height
        } else {
            R.dimen.flow_show_habit_header_expanded_height
        }
        (binding.collapsingToolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            height = resources.getDimensionPixelSize(expandedHeight)
            binding.collapsingToolbar.layoutParams = this
        }
        val cardElevation = if (isLight) dim(R.dimen.flow_card_elevation) else 0f
        listOf(
            binding.overviewCard,
            binding.currentStreakCard,
            binding.bestStreakCard,
            binding.historyCard,
            binding.scoreCard,
            binding.barCard,
            binding.notesCard,
            binding.targetCard,
            binding.frequencyCard
        ).forEach {
            it.clipToOutline = true
            ViewCompat.setElevation(it, cardElevation)
        }
    }

    fun setListener(presenter: ShowHabitPresenter) {
        binding.scoreCard.setListener(presenter.scoreCardPresenter)
        binding.historyCard.setListener(presenter.historyCardPresenter)
        binding.barCard.setListener(presenter.barCardPresenter)
    }
}
