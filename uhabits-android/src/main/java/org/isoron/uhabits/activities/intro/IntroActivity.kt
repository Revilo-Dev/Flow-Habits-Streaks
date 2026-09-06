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

package org.isoron.uhabits.activities.intro

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro2
import com.github.appintro.AppIntroFragment
import org.isoron.uhabits.R
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.utils.StyledResources

/**
 * Activity that introduces the app to the user, shown only after the app is
 * launched for the first time.
 */
class IntroActivity : AppIntro2() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showStatusBar(false)
        val themeSwitcher = AndroidThemeSwitcher(
            this,
            (application as HabitsApplication).component.preferences
        )
        themeSwitcher.apply()
        val colors = StyledResources(this)
        val background = colors.getColor(R.attr.flowBackgroundColor)
        val secondarySurface = colors.getColor(R.attr.flowSurfaceSecondaryColor)
        val primaryText = colors.getColor(R.attr.flowTextPrimaryColor)
        val secondaryText = colors.getColor(R.attr.flowTextSecondaryColor)
        val accent = colors.getColor(R.attr.flowAccentColor)
        setBarColor(secondarySurface)
        setNextArrowColor(accent)
        setSkipArrowColor(primaryText)
        setIndicatorColor(accent, secondaryText)
        findViewById<ImageButton>(com.github.appintro.R.id.next).apply {
            setImageResource(R.drawable.control_next)
            setColorFilter(accent)
            contentDescription = getString(R.string.flow_onboarding_next)
        }
        findViewById<ImageButton>(com.github.appintro.R.id.back).apply {
            setImageResource(R.drawable.flow_ic_back)
            setColorFilter(primaryText)
            contentDescription = getString(R.string.flow_onboarding_back)
        }
        setImageSkipButton(getDrawable(R.drawable.flow_ic_close)!!)
        setImageDoneButton(getDrawable(R.drawable.checkbox_checked)!!)
        findViewById<ImageButton>(com.github.appintro.R.id.skip).contentDescription =
            getString(R.string.flow_onboarding_skip)
        findViewById<ImageButton>(com.github.appintro.R.id.done).contentDescription =
            getString(R.string.flow_onboarding_done)
        isButtonsEnabled = false
        findViewById<View>(com.github.appintro.R.id.bottom).visibility = View.GONE
        findViewById<View>(com.github.appintro.R.id.view_pager).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) goToNextSlide()
            true
        }

        addSlide(
            AppIntroFragment.newInstance(
                title = getString(R.string.intro_title_1),
                description = getString(R.string.intro_description_1),
                imageDrawable = R.drawable.onboarding_appicon,
                backgroundColor = background,
                titleColor = primaryText,
                descriptionColor = secondaryText
            )
        )

        addSlide(
            AppIntroFragment.newInstance(
                title = getString(R.string.intro_title_2),
                description = getString(R.string.intro_description_2),
                imageDrawable = R.drawable.onboarding_createhabits,
                backgroundColor = background,
                titleColor = primaryText,
                descriptionColor = secondaryText
            )
        )

        addSlide(
            AppIntroFragment.newInstance(
                title = getString(R.string.intro_title_4),
                description = getString(R.string.intro_description_4),
                imageDrawable = R.drawable.onboarding_trackprogress,
                backgroundColor = background,
                titleColor = primaryText,
                descriptionColor = secondaryText
            )
        )
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        finish()
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        finish()
    }
}
