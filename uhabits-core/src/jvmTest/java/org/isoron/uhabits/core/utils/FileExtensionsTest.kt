package org.isoron.uhabits.core.utils

import kotlinx.coroutines.runBlocking
import org.isoron.platform.io.JavaUserFile
import org.isoron.uhabits.core.JvmBaseUnitTest
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class FileExtensionsTest : JvmBaseUnitTest() {

    @Test
    fun testIsSQLite3File() = runBlocking {
        val file = File.createTempFile("asset", "")
        copyAssetToFile("loop.db", file)
        val userFile = JavaUserFile(file.toPath())
        val isSqlite3File = isSQLite3File(userFile)
        assertTrue(isSqlite3File)
    }
}
