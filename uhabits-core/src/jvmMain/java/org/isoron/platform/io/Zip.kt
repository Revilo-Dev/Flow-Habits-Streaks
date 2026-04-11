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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

actual class ZipReader actual constructor(bytes: ByteArray) {
    private val data = bytes

    actual suspend fun entries(): List<ZipEntry> {
        val result = mutableListOf<ZipEntry>()
        val zis = ZipInputStream(ByteArrayInputStream(data))
        var entry = zis.nextEntry
        while (entry != null) {
            result.add(ZipEntry(entry.name, zis.readBytes().decodeToString()))
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        return result
    }
}

actual class ZipWriter {
    private val baos = ByteArrayOutputStream()
    private val zos = ZipOutputStream(baos)

    actual fun addEntry(name: String, content: String) {
        zos.putNextEntry(java.util.zip.ZipEntry(name))
        zos.write(content.toByteArray())
        zos.closeEntry()
    }

    actual suspend fun toBytes(): ByteArray {
        zos.close()
        return baos.toByteArray()
    }
}
