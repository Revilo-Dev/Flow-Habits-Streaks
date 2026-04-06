package org.isoron.platform.io

import kotlinx.coroutines.runBlocking
import org.isoron.uhabits.core.DATABASE_VERSION

actual object TestDatabaseHelper {
    private val fileOpener = JavaFileOpener()

    actual fun createEmptyDatabase(): Database {
        val db = JavaDatabaseOpener().open(":memory:")
        db.setVersion(8)
        runBlocking {
            db.migrateTo(DATABASE_VERSION) { v -> loadMigrationSQL(v) }
        }
        return db
    }

    actual fun loadMigrationSQL(version: Int): String {
        val path = "migrations/%02d.sql".format(version)
        return runBlocking {
            fileOpener.openResourceFile(path).lines().joinToString("\n")
        }
    }
}
