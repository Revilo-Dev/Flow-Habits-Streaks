package org.isoron.platform.io

import org.isoron.platform.gui.Canvas
import org.isoron.platform.gui.JavaCanvas
import org.isoron.platform.time.JavaLocalDateFormatter
import org.isoron.platform.time.LocalDateFormatter
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.util.Locale

actual fun createTestFileOpener(): FileOpener = JavaFileOpener()
actual fun createTestDatabaseOpener(): DatabaseOpener = JavaDatabaseOpener()
actual suspend fun createTestDatabaseOpenerSuspend(): DatabaseOpener = JavaDatabaseOpener()

actual fun createTestCanvas(width: Int, height: Int): Canvas {
    return JavaCanvas(BufferedImage(2 * width, 2 * height, TYPE_INT_ARGB), 2.0)
}

actual fun createTestDateFormatter(): LocalDateFormatter {
    return JavaLocalDateFormatter(Locale.US)
}

actual suspend fun ensureFontsLoaded() {}
