package org.isoron.platform.gui

import org.isoron.platform.io.createTestCanvas
import org.isoron.platform.io.createTestFileOpener
import org.isoron.platform.io.ensureFontsLoaded
import kotlin.test.fail

suspend fun assertRenders(
    path: String,
    canvas: Canvas
) {
    val actualImage = canvas.toImage()
    val failedActualPath = "/tmp/failed/$path"
    val failedExpectedPath = failedActualPath.replace(".png", ".expected.png")
    val failedDiffPath = failedActualPath.replace(".png", ".diff.png")
    val fileOpener = createTestFileOpener()
    val expectedFile = fileOpener.openResourceFile(path)
    if (expectedFile.exists()) {
        val expectedImage = expectedFile.toImage()
        val diffImage = expectedFile.toImage()
        diffImage.diff(actualImage)
        val distance = diffImage.averageLuminosity * 100
        if (distance >= 1.0) {
            expectedImage.export(failedExpectedPath)
            actualImage.export(failedActualPath)
            diffImage.export(failedDiffPath)
            fail("Images differ (distance=$distance)")
        }
    } else {
        actualImage.export(failedActualPath)
        fail("Expected image file is missing. Actual image: $failedActualPath")
    }
}

suspend fun assertRenders(
    width: Int,
    height: Int,
    expectedPath: String,
    view: View
) {
    ensureFontsLoaded()
    val canvas = createTestCanvas(width, height)
    view.draw(canvas)
    assertRenders(expectedPath, canvas)
}
