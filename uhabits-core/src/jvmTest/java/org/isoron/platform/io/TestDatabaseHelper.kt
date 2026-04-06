package org.isoron.platform.io

import org.isoron.uhabits.core.DATABASE_VERSION

actual object TestDatabaseHelper {
    actual fun createEmptyDatabase(): Database {
        val db = JavaDatabaseOpener().open(":memory:")
        db.setVersion(8)
        db.migrateTo(DATABASE_VERSION) { v -> loadMigrationSQL(v) }
        return db
    }

    actual fun loadMigrationSQL(version: Int): String {
        val filename = "/migrations/%02d.sql".format(version)
        return TestDatabaseHelper::class.java
            .getResourceAsStream(filename)!!
            .bufferedReader().readText()
    }
}
