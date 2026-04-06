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
package org.isoron.platform.io

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import org.isoron.platform.gui.AndroidImage
import org.isoron.platform.gui.Image
import java.io.File
import java.io.IOException

class AndroidResourceFile(
    private val assets: AssetManager,
    private val path: String
) : ResourceFile {
    override suspend fun lines(): List<String> {
        return assets.open(path).bufferedReader().readLines()
    }

    override suspend fun exists(): Boolean {
        return try {
            assets.open(path).close()
            true
        } catch (e: IOException) {
            false
        }
    }

    override suspend fun copyTo(dest: UserFile) {
        val bytes = assets.open(path).use { it.readBytes() }
        dest.writeBytes(bytes)
    }

    override suspend fun toImage(): Image {
        val bitmap = assets.open(path).use { BitmapFactory.decodeStream(it) }
        return AndroidImage(bitmap)
    }
}

class AndroidFileOpener(
    private val assets: AssetManager,
    private val filesDir: File
) : FileOpener {
    override fun openResourceFile(path: String): ResourceFile {
        return AndroidResourceFile(assets, path)
    }

    override fun openUserFile(path: String): UserFile {
        return JavaUserFile(File(filesDir, path).toPath())
    }
}
