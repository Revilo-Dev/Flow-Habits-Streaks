package org.isoron.platform.io

@JsModule("sprintf-js")
@JsNonModule
external object SprintfJs {
    fun sprintf(format: String, vararg args: Any?): String
}

actual fun format(format: String, arg: String): String =
    SprintfJs.sprintf(format, arg)

actual fun format(format: String, arg: Int): String =
    SprintfJs.sprintf(format, arg)

actual fun format(format: String, arg: Double): String =
    SprintfJs.sprintf(format, arg)
