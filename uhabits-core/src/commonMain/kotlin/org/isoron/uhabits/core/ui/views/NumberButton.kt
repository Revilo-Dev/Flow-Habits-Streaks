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

package org.isoron.uhabits.core.ui.views

import org.isoron.platform.gui.Canvas
import org.isoron.platform.gui.Color
import org.isoron.platform.gui.Font
import org.isoron.platform.gui.View
import kotlin.math.round

fun Double.toShortString(): String = when {
    this >= 1e9 -> "%.1fG".format(this / 1e9)
    this >= 1e8 -> "%.0fM".format(this / 1e6)
    this >= 1e7 -> "%.1fM".format(this / 1e6)
    this >= 1e6 -> "%.1fM".format(this / 1e6)
    this >= 1e5 -> "%.0fk".format(this / 1e3)
    this >= 1e4 -> "%.1fk".format(this / 1e3)
    this >= 1e3 -> "%.1fk".format(this / 1e3)
    this >= 1e2 -> "%.0f".format(this)
    this >= 1e1 -> when {
        round(this) == this -> "%.0f".format(this)
        else -> "%.1f".format(this)
    }
    else -> when {
        round(this) == this -> "%.0f".format(this)
        round(this * 10) == this * 10 -> "%.1f".format(this)
        else -> "%.2f".format(this)
    }
}

class NumberButton(
    val color: Color,
    val value: Double,
    val threshold: Double,
    val units: String,
    val theme: Theme
) : View {

    override fun draw(canvas: Canvas) {
        val width = canvas.getWidth()
        val height = canvas.getHeight()
        val em = theme.smallTextSize

        canvas.setColor(
            when {
                value >= threshold -> color
                value >= 0.01 -> theme.mediumContrastTextColor
                else -> theme.lowContrastTextColor
            }
        )

        canvas.setFontSize(theme.regularTextSize)
        canvas.setFont(Font.BOLD)
        canvas.drawText(value.toShortString(), width / 2, height / 2 - 0.6 * em)

        canvas.setFontSize(theme.smallTextSize)
        canvas.setFont(Font.REGULAR)
        canvas.drawText(units, width / 2, height / 2 + 0.6 * em)
    }
}
