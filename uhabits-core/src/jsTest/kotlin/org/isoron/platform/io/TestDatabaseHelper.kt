package org.isoron.platform.io

import org.isoron.uhabits.core.DATABASE_VERSION
import org.isoron.uhabits.core.database.SQLParser
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual object TestDatabaseHelper {
    private var sqlJs: dynamic = null
    private val migrationCache = mutableMapOf<Int, String>()

    private suspend fun ensureInitialized() {
        if (sqlJs == null) {
            sqlJs = suspendCoroutine<dynamic> { cont ->
                initSqlJs().then(
                    { result: dynamic -> cont.resume(result) },
                    { err: dynamic -> cont.resumeWithException(RuntimeException("$err")) }
                )
            }
            for (v in 9..DATABASE_VERSION) {
                migrationCache[v] = fetchMigrationSQL(v)
            }
        }
    }

    actual suspend fun createEmptyDatabase(): Database {
        ensureInitialized()
        val db = JsDatabaseOpener(sqlJs).open(":memory:")
        db.setVersion(8)
        for (v in 9..DATABASE_VERSION) {
            val commands = SQLParser.parse(migrationCache[v]!!)
            for (cmd in commands) db.run(cmd)
            db.setVersion(v)
        }
        return db
    }

    suspend fun getInitializedSqlJs(): dynamic {
        ensureInitialized()
        return sqlJs
    }

    actual suspend fun loadMigrationSQL(version: Int): String {
        ensureInitialized()
        return migrationCache[version]
            ?: error("Migration SQL for version $version not found")
    }

    private suspend fun fetchMigrationSQL(version: Int): String {
        val path = "migrations/${version.toString().padStart(2, '0')}.sql"
        return suspendCoroutine { cont ->
            kotlinx.browser.window.fetch(path).then(
                { response ->
                    if (!response.ok) {
                        cont.resumeWithException(RuntimeException("Failed to fetch $path: ${response.status}"))
                    } else {
                        response.text().then(
                            { text: String -> cont.resume(text) },
                            { err: dynamic -> cont.resumeWithException(RuntimeException("$err")) }
                        )
                    }
                },
                { err: dynamic -> cont.resumeWithException(RuntimeException("$err")) }
            )
        }
    }
}
