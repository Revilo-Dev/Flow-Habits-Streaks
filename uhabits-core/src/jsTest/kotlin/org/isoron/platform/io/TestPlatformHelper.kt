package org.isoron.platform.io

import kotlinx.browser.document
import org.isoron.platform.gui.Canvas
import org.isoron.platform.gui.JsCanvas
import org.isoron.platform.time.JsLocalDateFormatter
import org.isoron.platform.time.LocalDateFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private var currentTestStorage = JsFileStorage()

actual fun createTestFileOpener(): FileOpener {
    currentTestStorage = JsFileStorage()
    return JsFileOpener(currentTestStorage)
}

@Deprecated(
    "Use createTestDatabaseOpenerSuspend() instead",
    level = DeprecationLevel.ERROR
)
actual fun createTestDatabaseOpener(): DatabaseOpener {
    throw UnsupportedOperationException(
        "createTestDatabaseOpener() requires async sql.js init. " +
            "Use createTestDatabaseOpenerSuspend() instead."
    )
}

actual suspend fun createTestDatabaseOpenerSuspend(): DatabaseOpener {
    val sqlJs = TestDatabaseHelper.getInitializedSqlJs()
    return JsDatabaseOpener(sqlJs, currentTestStorage)
}

actual fun createTestCanvas(width: Int, height: Int): Canvas {
    return JsCanvas.create(width, height, 2.0)
}

actual fun createTestDateFormatter(): LocalDateFormatter {
    return JsLocalDateFormatter("en-US")
}

private var fontsLoaded = false

actual suspend fun ensureFontsLoaded() {
    if (fontsLoaded) return
    val style = document.createElement("style")
    style.textContent = buildString {
        appendLine("@font-face { font-family: 'NotoSans'; src: url('/fonts/NotoSans-Regular.ttf') format('truetype'); }")
        appendLine("@font-face { font-family: 'NotoSansBold'; src: url('/fonts/NotoSans-Bold.ttf') format('truetype'); }")
        appendLine("@font-face { font-family: 'FontAwesome'; src: url('/fonts/FontAwesome.ttf') format('truetype'); }")
    }
    document.head?.appendChild(style)
    loadFontAsync("12px NotoSans")
    loadFontAsync("bold 12px NotoSansBold")
    loadFontAsync("12px FontAwesome")
    fontsLoaded = true
}

private suspend fun loadFontAsync(fontSpec: String) {
    suspendCoroutine<Unit> { cont ->
        val promise = document.asDynamic().fonts.load(fontSpec)
        promise.then(
            { _: dynamic -> cont.resume(Unit) },
            { err: dynamic -> cont.resumeWithException(RuntimeException("Failed to load font '$fontSpec': $err")) }
        )
    }
    val loaded = document.asDynamic().fonts.check(fontSpec) as Boolean
    if (!loaded) error("Font not available after loading: '$fontSpec'")
}
