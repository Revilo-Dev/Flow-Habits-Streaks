package org.isoron.platform.io

import org.isoron.platform.gui.Canvas
import org.isoron.platform.time.LocalDateFormatter

expect fun createTestFileOpener(): FileOpener
expect fun createTestDatabaseOpener(): DatabaseOpener
expect suspend fun createTestDatabaseOpenerSuspend(): DatabaseOpener
expect fun createTestCanvas(width: Int, height: Int): Canvas
expect fun createTestDateFormatter(): LocalDateFormatter
expect suspend fun ensureFontsLoaded()
