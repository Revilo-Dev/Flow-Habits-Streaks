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

package org.isoron.uhabits.activities

import org.isoron.platform.gui.Color
import org.isoron.uhabits.core.ui.views.DarkTheme
import org.isoron.uhabits.core.ui.views.LightTheme

/** Keeps canvas-based charts on the same surfaces as Flow's XML cards. */
class FlowLightTheme : LightTheme() {
    override val appBackgroundColor = Color(0xF3F3F5)
    override val cardBackgroundColor = Color(0xFCFCFF)
    override val headerBackgroundColor = Color(0xF3F3F5)
    override val headerBorderColor = Color(0xE4E4E7)
    override val headerTextColor = Color(0x848487)
    override val highContrastTextColor = Color(0x000000)
    override val itemBackgroundColor = Color(0xFCFCFF)
    override val lowContrastTextColor = Color(0xE4E4E7)
    override val mediumContrastTextColor = Color(0x606067)
    override val statusBarBackgroundColor = Color(0xF3F3F5)
    override val toolbarBackgroundColor = Color(0xF3F3F5)
    override val toolbarColor = Color(0xFCFCFF)
}

open class FlowDarkTheme : DarkTheme() {
    override val appBackgroundColor = Color(0x000000)
    override val cardBackgroundColor = Color(0x1C1C1E)
    override val headerBackgroundColor = Color(0x000000)
    override val headerBorderColor = Color(0x303033)
    override val headerTextColor = Color(0x85858A)
    override val highContrastTextColor = Color(0xFFFFFF)
    override val itemBackgroundColor = Color(0x1C1C1E)
    override val lowContrastTextColor = Color(0x303033)
    override val mediumContrastTextColor = Color(0xA6A6AA)
    override val statusBarBackgroundColor = Color(0x000000)
    override val toolbarBackgroundColor = Color(0x000000)
    override val toolbarColor = Color(0x1C1C1E)
}

class FlowPureBlackTheme : FlowDarkTheme() {
    override val cardBackgroundColor = Color(0x121214)
    override val itemBackgroundColor = Color(0x121214)
    override val toolbarColor = Color(0x121214)
}
