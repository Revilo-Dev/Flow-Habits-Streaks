package org.isoron.platform.io

actual class ZipReader actual constructor(bytes: ByteArray) {
    private val data = bytes

    actual fun entries(): List<ZipEntry> {
        val result = mutableListOf<ZipEntry>()
        var pos = 0
        while (pos + 30 <= data.size) {
            val sig = readInt(pos)
            if (sig != 0x04034b50) break
            val compressionMethod = readShort(pos + 8)
            val compressedSize = readInt(pos + 18)
            val nameLen = readShort(pos + 26)
            val extraLen = readShort(pos + 28)
            val nameStart = pos + 30
            val name = data.copyOfRange(nameStart, nameStart + nameLen).decodeToString()
            val dataStart = nameStart + nameLen + extraLen
            if (compressionMethod != 0) {
                throw UnsupportedOperationException(
                    "Only STORED ZIP entries are supported, got method $compressionMethod"
                )
            }
            val content = data.copyOfRange(dataStart, dataStart + compressedSize).decodeToString()
            result.add(ZipEntry(name, content))
            pos = dataStart + compressedSize
        }
        return result
    }

    private fun readInt(offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)

    private fun readShort(offset: Int): Int =
        (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)
}
