package org.isoron.platform.io

import kotlinx.browser.window
import org.isoron.platform.gui.Image
import org.isoron.platform.gui.JsImage
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class JsFileStorage {
    private val store = mutableMapOf<String, ByteArray>()

    fun write(path: String, bytes: ByteArray) {
        store[path] = bytes
    }

    fun read(path: String): ByteArray? = store[path]

    fun exists(path: String): Boolean = path in store

    fun delete(path: String) {
        store.remove(path)
    }

    fun listKeys(prefix: String): List<String> =
        store.keys.filter { it.startsWith(prefix) }
}

class JsUserFile(
    private val storage: JsFileStorage,
    override val pathString: String
) : UserFile {
    override suspend fun delete() {
        storage.delete(pathString)
    }

    override suspend fun exists(): Boolean = storage.exists(pathString)

    override suspend fun lines(): List<String> {
        val bytes = storage.read(pathString)
            ?: error("File not found: $pathString")
        val result = bytes.decodeToString().lines()
        return if (result.lastOrNull() == "") result.dropLast(1) else result
    }

    override suspend fun writeString(content: String) {
        storage.write(pathString, content.encodeToByteArray())
    }

    override suspend fun writeBytes(bytes: ByteArray) {
        storage.write(pathString, bytes)
    }

    override suspend fun readBytes(limit: Int): ByteArray {
        val bytes = storage.read(pathString) ?: return ByteArray(0)
        return if (bytes.size <= limit) bytes else bytes.copyOf(limit)
    }

    override fun resolve(child: String): UserFile {
        val resolved = if (pathString.endsWith("/")) {
            "$pathString$child"
        } else {
            "$pathString/$child"
        }
        return JsUserFile(storage, resolved)
    }

    override suspend fun listFiles(): List<UserFile>? {
        val prefix = if (pathString.endsWith("/")) pathString else "$pathString/"
        val keys = storage.listKeys(prefix)
        if (keys.isEmpty()) return null
        val children = mutableSetOf<String>()
        for (key in keys) {
            val rest = key.removePrefix(prefix)
            val child = rest.split("/").first()
            children.add(child)
        }
        return children.map { JsUserFile(storage, "$prefix$it") }
    }

    override suspend fun mkdirs() {
        // No-op for in-memory storage
    }
}

class JsResourceFile(private val path: String) : ResourceFile {
    override suspend fun copyTo(dest: UserFile) {
        val bytes = fetchBytes(path)
        dest.writeBytes(bytes)
    }

    override suspend fun lines(): List<String> {
        val text = fetchText(path)
        val result = text.lines()
        return if (result.lastOrNull() == "") result.dropLast(1) else result
    }

    override suspend fun exists(): Boolean {
        return try {
            fetchText(path)
            true
        } catch (e: Throwable) {
            false
        }
    }

    override suspend fun toImage(): Image {
        return suspendCoroutine { cont ->
            val img = org.w3c.dom.Image()
            img.onload = {
                val canvas = (
                    kotlinx.browser.document.createElement("canvas")
                        as HTMLCanvasElement
                    )
                canvas.width = img.width
                canvas.height = img.height
                val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
                ctx.drawImage(img, 0.0, 0.0)
                cont.resume(JsImage(canvas))
            }
            img.onerror = { _, _, _, _, _ ->
                cont.resumeWithException(RuntimeException("Failed to load image: $path"))
            }
            img.src = path
        }
    }
}

private suspend fun fetchText(path: String): String = suspendCoroutine { cont ->
    window.fetch(path).then(
        { response ->
            if (!response.ok) {
                cont.resumeWithException(RuntimeException("Failed to fetch $path: ${response.status}"))
            } else {
                response.text().then(
                    { text -> cont.resume(text) },
                    { err -> cont.resumeWithException(RuntimeException("Failed to read $path: $err")) }
                )
            }
        },
        { err -> cont.resumeWithException(RuntimeException("Failed to fetch $path: $err")) }
    )
}

private suspend fun fetchBytes(path: String): ByteArray = suspendCoroutine { cont ->
    window.fetch(path).then(
        { response ->
            if (!response.ok) {
                cont.resumeWithException(RuntimeException("Failed to fetch $path: ${response.status}"))
            } else {
                response.arrayBuffer().then(
                    { buffer ->
                        val uint8Array = js("new Uint8Array(buffer)")
                        val length = uint8Array.length as Int
                        val bytes = ByteArray(length)
                        for (i in 0 until length) {
                            bytes[i] = (uint8Array[i] as Number).toByte()
                        }
                        cont.resume(bytes)
                    },
                    { err -> cont.resumeWithException(RuntimeException("Failed to read $path: $err")) }
                )
            }
        },
        { err -> cont.resumeWithException(RuntimeException("Failed to fetch $path: $err")) }
    )
}

class JsFileOpener(private val storage: JsFileStorage = JsFileStorage()) : FileOpener {
    override fun openResourceFile(path: String): ResourceFile {
        val url = if (path.startsWith("migrations/")) path else "test-assets/$path"
        return JsResourceFile(url)
    }

    override fun openUserFile(path: String): UserFile = JsUserFile(storage, path)
}
