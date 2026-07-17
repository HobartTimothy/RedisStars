package org.roberthu.rs.redis

import io.lettuce.core.api.StatefulConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes standalone Redis SELECT handling across shared string and binary connections.
 *
 * Both connections stay on the same logical database. [selectedDatabase] is the session
 * database used by commands that do not specify one explicitly.
 */
internal class RedisDatabaseSession {
    private val mutex = Mutex()
    private var connectionDatabase: Int = 0

    var selectedDatabase: Int = 0
        private set

    fun initialize(database: Int) {
        connectionDatabase = database
        selectedDatabase = database
    }

    suspend fun <T> withExclusiveLock(block: suspend () -> T): T = mutex.withLock { block() }

    suspend fun <T> withLock(
        isCluster: Boolean,
        stringConnection: StatefulConnection<String, String>?,
        binaryConnection: StatefulConnection<ByteArray, ByteArray>?,
        block: suspend SessionScope.() -> T,
    ): T = mutex.withLock {
        block(SessionScope(isCluster, stringConnection, binaryConnection))
    }

    fun selectBothOnConnect(
        database: Int,
        stringConnection: StatefulConnection<String, String>,
        binaryConnection: StatefulConnection<ByteArray, ByteArray>,
    ) {
        (stringConnection.syncClusterCommands() as RedisCommands<String, String>).select(database)
        (binaryConnection.syncClusterCommands() as RedisCommands<ByteArray, ByteArray>).select(database)
        connectionDatabase = database
        selectedDatabase = database
    }

    inner class SessionScope internal constructor(
        private val isCluster: Boolean,
        private val stringConnection: StatefulConnection<String, String>?,
        private val binaryConnection: StatefulConnection<ByteArray, ByteArray>?,
    ) {
        fun selectSessionDatabase(index: Int) {
            if (isCluster) {
                selectedDatabase = 0
                return
            }
            selectBoth(index)
            selectedDatabase = index
        }

        fun <T> onString(operation: (io.lettuce.core.cluster.api.sync.RedisClusterCommands<String, String>) -> T): T {
            val connection = stringConnection ?: error("String connection is not active")
            if (!isCluster) {
                selectBoth(selectedDatabase)
            }
            return operation(connection.syncClusterCommands())
        }

        fun <T> onBinary(
            operation: (io.lettuce.core.cluster.api.sync.RedisClusterCommands<ByteArray, ByteArray>) -> T,
        ): T {
            val connection = binaryConnection ?: error("Binary connection is not active")
            if (!isCluster) {
                selectBoth(selectedDatabase)
            }
            return operation(connection.syncClusterCommands())
        }

        fun <T> onDatabase(
            database: Int,
            updateSelected: Boolean,
            operation: (io.lettuce.core.cluster.api.sync.RedisClusterCommands<String, String>) -> T,
        ): T {
            val connection = stringConnection ?: error("String connection is not active")
            if (!isCluster) {
                selectBoth(database)
            }
            if (updateSelected) {
                selectedDatabase = database
            }
            return operation(connection.syncClusterCommands())
        }

        suspend fun <T> onDatabaseSuspend(
            database: Int,
            updateSelected: Boolean,
            operation: suspend (io.lettuce.core.cluster.api.sync.RedisClusterCommands<String, String>) -> T,
        ): T {
            val connection = stringConnection ?: error("String connection is not active")
            if (!isCluster) {
                selectBoth(database)
            }
            if (updateSelected) {
                selectedDatabase = database
            }
            return operation(connection.syncClusterCommands())
        }

        fun <T> onTemporaryDatabase(
            database: Int,
            operation: (io.lettuce.core.cluster.api.sync.RedisClusterCommands<String, String>) -> T,
        ): T {
            val connection = stringConnection ?: error("String connection is not active")
            if (isCluster) {
                return operation(connection.syncClusterCommands())
            }
            val sessionDatabase = selectedDatabase
            selectBoth(database)
            return try {
                operation(connection.syncClusterCommands())
            } finally {
                if (connectionDatabase != sessionDatabase) {
                    selectBoth(sessionDatabase)
                }
            }
        }

        private fun selectBoth(database: Int) {
            if (connectionDatabase == database) return
            val stringConn = stringConnection ?: error("String connection is not active")
            val binaryConn = binaryConnection ?: error("Binary connection is not active")
            (stringConn.syncClusterCommands() as RedisCommands<String, String>).select(database)
            (binaryConn.syncClusterCommands() as RedisCommands<ByteArray, ByteArray>).select(database)
            connectionDatabase = database
        }
    }
}
