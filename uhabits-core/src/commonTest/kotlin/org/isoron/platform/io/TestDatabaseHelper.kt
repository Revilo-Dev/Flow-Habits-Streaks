package org.isoron.platform.io

expect object TestDatabaseHelper {
    fun createEmptyDatabase(): Database
    fun loadMigrationSQL(version: Int): String
}
