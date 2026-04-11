package org.isoron.platform.gui

import kotlinx.coroutines.test.runTest
import org.isoron.platform.io.createTestCanvas
import org.isoron.platform.io.ensureFontsLoaded
import kotlin.test.Test

class CanvasTest {
    @Test
    fun testDrawTestImage() = runTest {
        ensureFontsLoaded()
        val canvas = createTestCanvas(500, 400)
        canvas.drawTestImage()
        assertRenders("views/CanvasTest.png", canvas)
    }
}
