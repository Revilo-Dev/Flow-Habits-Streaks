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
package org.isoron.uhabits.activities.settings

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.databinding.SettingsActivityBinding
import org.isoron.uhabits.utils.StyledResources
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.setupToolbar
import org.isoron.uhabits.utils.updateFlowStickyControls

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val component = (application as HabitsApplication).component
        val themeSwitcher = AndroidThemeSwitcher(this, component.preferences)
        themeSwitcher.apply()

        val binding = SettingsActivityBinding.inflate(LayoutInflater.from(this))
        binding.root.setupToolbar(
            toolbar = binding.toolbar,
            title = "",
            color = PaletteColor(11),
            theme = themeSwitcher.currentTheme,
            applyTopInset = false
        )
        binding.toolbar.setNavigationIcon(R.drawable.flow_ic_back)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.collapsingToolbar.title = resources.getString(R.string.settings)
        binding.collapsingToolbar.setContentScrimColor(Color.TRANSPARENT)
        binding.collapsingToolbar.setStatusBarScrimColor(Color.TRANSPARENT)
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
        binding.toolbar.elevation = 0f
        binding.appBar.addOnOffsetChangedListener(
            com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener { bar, offset ->
                binding.toolbar.updateFlowStickyControls(kotlin.math.abs(offset) >= bar.totalScrollRange)
            }
        )
        val flowBackground = StyledResources(this).getColor(R.attr.flowBackgroundColor)
        window.statusBarColor = flowBackground
        WindowInsetsControllerCompat(window, binding.root).isAppearanceLightStatusBars =
            ColorUtils.calculateLuminance(flowBackground) > 0.5
        binding.root.applyRootViewInsets()
        binding.appBar.applyToolbarInsets()
        setContentView(binding.root)
    }
}
