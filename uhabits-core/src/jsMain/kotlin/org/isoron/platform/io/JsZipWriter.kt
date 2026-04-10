package org.isoron.platform.io

actual class ZipWriter {
    private val entries = mutableListOf<Pair<String, ByteArray>>()

    actual fun addEntry(name: String, content: String) {
        entries.add(name to content.encodeToByteArray())
    }

    actual suspend fun toBytes(): ByteArray {
        val buf = ZipBuffer()
        val offsets = mutableListOf<Int>()

        for ((name, data) in entries) {
            offsets.add(buf.size)
            val nameBytes = name.encodeToByteArray()
            val crc = crc32(data)
            buf.writeInt(0x04034b50)
            buf.writeShort(20)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeInt(crc)
            buf.writeInt(data.size)
            buf.writeInt(data.size)
            buf.writeShort(nameBytes.size)
            buf.writeShort(0)
            buf.writeBytes(nameBytes)
            buf.writeBytes(data)
        }

        val centralDirOffset = buf.size
        for (i in entries.indices) {
            val (name, data) = entries[i]
            val nameBytes = name.encodeToByteArray()
            val crc = crc32(data)
            buf.writeInt(0x02014b50)
            buf.writeShort(20)
            buf.writeShort(20)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeInt(crc)
            buf.writeInt(data.size)
            buf.writeInt(data.size)
            buf.writeShort(nameBytes.size)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeShort(0)
            buf.writeInt(0)
            buf.writeInt(offsets[i])
            buf.writeBytes(nameBytes)
        }
        val centralDirSize = buf.size - centralDirOffset

        buf.writeInt(0x06054b50)
        buf.writeShort(0)
        buf.writeShort(0)
        buf.writeShort(entries.size)
        buf.writeShort(entries.size)
        buf.writeInt(centralDirSize)
        buf.writeInt(centralDirOffset)
        buf.writeShort(0)

        return buf.toByteArray()
    }
}

private class ZipBuffer {
    private var data = ByteArray(4096)
    var size = 0
        private set

    fun writeShort(v: Int) {
        ensureCapacity(2)
        data[size++] = (v and 0xFF).toByte()
        data[size++] = ((v shr 8) and 0xFF).toByte()
    }

    fun writeInt(v: Int) {
        ensureCapacity(4)
        data[size++] = (v and 0xFF).toByte()
        data[size++] = ((v shr 8) and 0xFF).toByte()
        data[size++] = ((v shr 16) and 0xFF).toByte()
        data[size++] = ((v shr 24) and 0xFF).toByte()
    }

    fun writeBytes(bytes: ByteArray) {
        ensureCapacity(bytes.size)
        bytes.copyInto(data, size)
        size += bytes.size
    }

    fun toByteArray(): ByteArray = data.copyOf(size)

    private fun ensureCapacity(needed: Int) {
        if (size + needed > data.size) {
            data = data.copyOf(maxOf(data.size * 2, size + needed))
        }
    }
}

private val crcTable = IntArray(256).also { table ->
    for (n in 0..255) {
        var c = n
        repeat(8) {
            c = if (c and 1 != 0) (c ushr 1) xor 0xEDB88320.toInt() else c ushr 1
        }
        table[n] = c
    }
}

private fun crc32(data: ByteArray): Int {
    var crc = -1
    for (b in data) {
        crc = (crc ushr 8) xor crcTable[(crc xor b.toInt()) and 0xFF]
    }
    return crc xor -1
}
