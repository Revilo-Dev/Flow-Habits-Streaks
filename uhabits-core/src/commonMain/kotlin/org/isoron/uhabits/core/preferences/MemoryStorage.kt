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

package org.isoron.uhabits.core.preferences

class MemoryStorage : Preferences.Storage {
    private val map = HashMap<String, String>()

    override fun clear() {
        map.clear()
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        map[key]?.toBoolean() ?: defValue

    override fun getInt(key: String, defValue: Int): Int =
        map[key]?.toIntOrNull() ?: defValue

    override fun getLong(key: String, defValue: Long): Long =
        map[key]?.toLongOrNull() ?: defValue

    override fun getString(key: String, defValue: String): String =
        map[key] ?: defValue

    override fun onAttached(preferences: Preferences) {}

    override fun putBoolean(key: String, value: Boolean) {
        map[key] = value.toString()
    }

    override fun putInt(key: String, value: Int) {
        map[key] = value.toString()
    }

    override fun putLong(key: String, value: Long) {
        map[key] = value.toString()
    }

    override fun putString(key: String, value: String) {
        map[key] = value
    }

    override fun remove(key: String) {
        map.remove(key)
    }
}
