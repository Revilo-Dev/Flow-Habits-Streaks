package org.isoron.platform.io

actual fun createTestFileOpener(): FileOpener = JavaFileOpener()
actual fun createTestDatabaseOpener(): DatabaseOpener = JavaDatabaseOpener()
