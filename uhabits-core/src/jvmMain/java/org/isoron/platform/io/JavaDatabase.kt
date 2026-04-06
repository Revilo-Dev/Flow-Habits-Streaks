package org.isoron.platform.io

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types

class JavaPreparedStatement(
    private val stmt: java.sql.PreparedStatement
) : PreparedStatement {
    private var resultSet: ResultSet? = null

    override fun step(): StepResult {
        if (resultSet == null) {
            return if (stmt.execute()) {
                resultSet = stmt.resultSet
                if (resultSet!!.next()) StepResult.ROW else StepResult.DONE
            } else {
                StepResult.DONE
            }
        }
        return if (resultSet!!.next()) StepResult.ROW else StepResult.DONE
    }

    override fun getInt(index: Int): Int = resultSet!!.getInt(index + 1)
    override fun getLong(index: Int): Long = resultSet!!.getLong(index + 1)
    override fun getReal(index: Int): Double = resultSet!!.getDouble(index + 1)
    override fun getText(index: Int): String = resultSet!!.getString(index + 1)

    override fun getIntOrNull(index: Int): Int? {
        val v = resultSet!!.getInt(index + 1)
        return if (resultSet!!.wasNull()) null else v
    }

    override fun getLongOrNull(index: Int): Long? {
        val v = resultSet!!.getLong(index + 1)
        return if (resultSet!!.wasNull()) null else v
    }

    override fun getRealOrNull(index: Int): Double? {
        val v = resultSet!!.getDouble(index + 1)
        return if (resultSet!!.wasNull()) null else v
    }

    override fun getTextOrNull(index: Int): String? {
        return resultSet!!.getString(index + 1)
    }

    override fun bindInt(index: Int, value: Int) = stmt.setInt(index, value)
    override fun bindLong(index: Int, value: Long) = stmt.setLong(index, value)
    override fun bindReal(index: Int, value: Double) = stmt.setDouble(index, value)
    override fun bindText(index: Int, value: String) = stmt.setString(index, value)
    override fun bindNull(index: Int) = stmt.setNull(index, Types.NULL)

    override fun reset() {
        resultSet?.close()
        resultSet = null
        stmt.clearParameters()
    }

    override fun finalize() {
        resultSet?.close()
        stmt.close()
    }
}

class JavaDatabase(private val conn: Connection) : Database {
    private var transactionDepth = 0

    override fun prepareStatement(sql: String): PreparedStatement {
        val trimmed = sql.trimStart().uppercase()
        if (trimmed.startsWith("BEGIN")) {
            return object : NoOpStatement() {
                override fun step(): StepResult {
                    if (transactionDepth == 0) conn.autoCommit = false
                    transactionDepth++
                    return StepResult.DONE
                }
            }
        }
        if (trimmed.startsWith("COMMIT")) {
            return object : NoOpStatement() {
                override fun step(): StepResult {
                    transactionDepth--
                    if (transactionDepth == 0) {
                        conn.commit()
                        conn.autoCommit = true
                    }
                    return StepResult.DONE
                }
            }
        }
        return JavaPreparedStatement(conn.prepareStatement(sql))
    }

    override fun close() = conn.close()
}

private abstract class NoOpStatement : PreparedStatement {
    override fun getInt(index: Int) = throw UnsupportedOperationException()
    override fun getLong(index: Int) = throw UnsupportedOperationException()
    override fun getReal(index: Int) = throw UnsupportedOperationException()
    override fun getText(index: Int) = throw UnsupportedOperationException()
    override fun getIntOrNull(index: Int) = throw UnsupportedOperationException()
    override fun getLongOrNull(index: Int) = throw UnsupportedOperationException()
    override fun getRealOrNull(index: Int) = throw UnsupportedOperationException()
    override fun getTextOrNull(index: Int) = throw UnsupportedOperationException()
    override fun bindInt(index: Int, value: Int) = throw UnsupportedOperationException()
    override fun bindLong(index: Int, value: Long) = throw UnsupportedOperationException()
    override fun bindReal(index: Int, value: Double) = throw UnsupportedOperationException()
    override fun bindText(index: Int, value: String) = throw UnsupportedOperationException()
    override fun bindNull(index: Int) = throw UnsupportedOperationException()
    override fun reset() {}
    override fun finalize() {}
}

class JavaDatabaseOpener : DatabaseOpener {
    override fun open(path: String): Database {
        val conn = DriverManager.getConnection("jdbc:sqlite:$path")
        return JavaDatabase(conn)
    }
}
