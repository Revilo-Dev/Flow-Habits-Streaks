package org.isoron.platform.gui

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.fetch.RequestInit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.json

class JsImage(private val canvas: HTMLCanvasElement) : Image {
    private val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

    override val width: Int get() = canvas.width
    override val height: Int get() = canvas.height

    override fun getPixel(x: Int, y: Int): Color {
        val data = ctx.getImageData(x.toDouble(), y.toDouble(), 1.0, 1.0)
        val d = data.data.asDynamic()
        val r = (d[0] as Number).toDouble() / 255.0
        val g = (d[1] as Number).toDouble() / 255.0
        val b = (d[2] as Number).toDouble() / 255.0
        val a = (d[3] as Number).toDouble() / 255.0
        return Color(r, g, b, a)
    }

    override fun setPixel(x: Int, y: Int, color: Color) {
        val data = ctx.createImageData(1.0, 1.0)
        val d = data.data.asDynamic()
        d[0] = (color.red * 255).toInt()
        d[1] = (color.green * 255).toInt()
        d[2] = (color.blue * 255).toInt()
        d[3] = (color.alpha * 255).toInt()
        ctx.putImageData(data, x.toDouble(), y.toDouble())
    }

    override suspend fun export(path: String) {
        val blob = suspendCoroutine<dynamic> { cont ->
            canvas.asDynamic().toBlob { b: dynamic -> cont.resume(b) }
        }
        val init = RequestInit(
            method = "POST",
            body = blob,
            headers = json("X-File-Path" to path)
        )
        suspendCoroutine<Unit> { cont ->
            window.fetch("/save-file", init).then(
                { cont.resume(Unit) },
                { err -> cont.resumeWithException(RuntimeException("$err")) }
            )
        }
    }

    companion object {
        fun create(width: Int, height: Int): JsImage {
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = width
            canvas.height = height
            return JsImage(canvas)
        }
    }
}
