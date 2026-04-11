package org.isoron.platform.io

import kotlinx.coroutines.await
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.Promise
import kotlin.js.json

@JsModule("jszip")
@JsNonModule
external class JSZip {
    fun loadAsync(data: dynamic): Promise<JSZip>
    fun file(name: String, data: String): JSZip
    fun forEach(callback: (String, dynamic) -> Unit)
    fun generateAsync(options: dynamic): Promise<dynamic>
}

actual class ZipReader actual constructor(bytes: ByteArray) {
    private val data = bytes

    actual suspend fun entries(): List<ZipEntry> {
        val uint8 = Uint8Array(data.size)
        for (i in data.indices) uint8[i] = data[i]
        val zip = JSZip().loadAsync(uint8).await()
        val result = mutableListOf<ZipEntry>()
        val files = mutableListOf<Pair<String, dynamic>>()
        zip.forEach { path, file ->
            if (!(file.dir as Boolean)) {
                files.add(path to file)
            }
        }
        for ((name, file) in files) {
            val content = (file.async("string") as Promise<String>).await()
            result.add(ZipEntry(name, content))
        }
        return result
    }
}

actual class ZipWriter {
    private val zip = JSZip()

    actual fun addEntry(name: String, content: String) {
        zip.file(name, content)
    }

    actual suspend fun toBytes(): ByteArray {
        val uint8 =
            (zip.generateAsync(json("type" to "uint8array", "compression" to "DEFLATE")) as Promise<Uint8Array>).await()
        return ByteArray(uint8.length) { uint8[it] }
    }
}
