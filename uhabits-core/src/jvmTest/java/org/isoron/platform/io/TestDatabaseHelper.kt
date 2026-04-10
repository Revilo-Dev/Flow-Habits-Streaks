package org.isoron.platform.io

import org.isoron.uhabits.core.DATABASE_VERSION

actual object TestDatabaseHelper {
    private val fileOpener = JavaFileOpener()

    actual suspend fun createEmptyDatabase(): Database {
        val db = JavaDatabaseOpener().open(":memory:")
        db.setVersion(8)
        db.migrateTo(DATABASE_VERSION) { v -> loadMigrationSQL(v) }
        return db
    }

    actual suspend fun loadMigrationSQL(version: Int): String {
        val path = "migrations/%02d.sql".format(version)
        return fileOpener.openResourceFile(path).lines().joinToString("\n")
    }
}
