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
package org.isoron.uhabits.activities.about

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.isoron.uhabits.BuildConfig
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.databinding.AboutBinding
import org.isoron.uhabits.utils.applyBottomInset
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.currentTheme
import org.isoron.uhabits.utils.setupToolbar
import org.isoron.uhabits.utils.sres
import org.isoron.uhabits.utils.updateFlowStickyControls

@SuppressLint("ViewConstructor")
class AboutView(
    context: Context,
    private val screen: AboutScreen
) : FrameLayout(context) {

    private var binding = AboutBinding.inflate(LayoutInflater.from(context))

    init {
        addView(binding.root)
        setupToolbar(
            toolbar = binding.toolbar,
            color = PaletteColor(11),
            title = "",
            theme = currentTheme(),
            applyTopInset = false
        )
        binding.collapsingToolbar.title = resources.getString(R.string.about)
        binding.toolbar.setNavigationIcon(R.drawable.flow_ic_back)
        binding.toolbar.setNavigationOnClickListener { (context as android.app.Activity).finish() }
        binding.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.toolbar.elevation = 0f
        binding.appBar.applyToolbarInsets()
        binding.appBar.addOnOffsetChangedListener(
            com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener { bar, offset ->
                binding.toolbar.updateFlowStickyControls(kotlin.math.abs(offset) >= bar.totalScrollRange)
            }
        )
        (context as android.app.Activity).window.statusBarColor =
            sres.getColor(R.attr.flowBackgroundColor)
        binding.tvContributors.setOnClickListener { screen.showCodeContributorsWebsite() }
        binding.tvFeedback.setOnClickListener { screen.showSendFeedbackScreen() }
        binding.tvPrivacy.setOnClickListener { screen.showPrivacyPolicyWebsite() }
        binding.tvRate.setOnClickListener { screen.showRateAppWebsite() }
        binding.tvSource.setOnClickListener { screen.showSourceCodeWebsite() }
        binding.tvTranslate.setOnClickListener { screen.showTranslationWebsite() }
        binding.tvVersion.setOnClickListener { screen.onPressDeveloperCountdown() }
        binding.tvVersion.text = resources.getString(R.string.version_n, "1.0.0")
        binding.outerLinearLayout.applyBottomInset()
        applyRootViewInsets()
    }
}
