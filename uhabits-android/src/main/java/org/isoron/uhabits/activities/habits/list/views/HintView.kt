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

import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.uhabits.R
import org.isoron.uhabits.core.ui.screens.habits.list.HintList
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.sres

class HintView(
    context: Context,
    private val hintList: HintList
) : FrameLayout(context) {

    val hintContent: TextView

    init {
        isClickable = true
        visibility = GONE
        val padding = dp(16.0f).toInt()
        setPadding(padding, padding, padding, padding)
        setBackgroundResource(R.drawable.flow_surface_secondary_background)

        val hintTitle = TextView(context).apply {
            setTextColor(sres.getColor(R.attr.flowTextPrimaryColor))
            setTypeface(null, Typeface.BOLD)
            text = resources.getString(R.string.hint_title)
        }

        hintContent = TextView(context).apply {
            setTextColor(sres.getColor(R.attr.flowTextSecondaryColor))
            setPadding(0, dp(5.0f).toInt(), 0, 0)
        }

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(hintTitle, WRAP_CONTENT, WRAP_CONTENT)
            addView(hintContent, WRAP_CONTENT, WRAP_CONTENT)
        }, LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { marginEnd = dp(28f).toInt() })
        addView(ImageButton(context).apply {
            setImageResource(R.drawable.flow_ic_close)
            background = null
            contentDescription = resources.getString(android.R.string.cancel)
            setOnClickListener { dismiss() }
        }, LayoutParams(dp(32f).toInt(), dp(32f).toInt(), android.view.Gravity.TOP or android.view.Gravity.END))
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        showNext()
    }

    fun showNext() {
        if (!hintList.shouldShow()) return
        val hint = hintList.pop() ?: return

        hintContent.text = hint
        requestLayout()

        alpha = 0.0f
        visibility = View.VISIBLE
        animate().alpha(1f).duration = 500
    }

    private fun dismiss() {
        animate().alpha(0f).setDuration(500).setListener(DismissAnimator())
    }

    private inner class DismissAnimator : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) {
            visibility = View.GONE
        }
    }
}
