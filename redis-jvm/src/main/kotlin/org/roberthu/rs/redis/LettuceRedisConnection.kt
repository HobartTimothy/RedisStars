package org.roberthu.rs.redis

import io.lettuce.core.KeyScanArgs
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.ScanArgs
import io.lettuce.core.ScanCursor
import io.lettuce.core.XAddArgs
import io.lettuce.core.api.StatefulConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.output.NestedMultiOutput
import io.lettuce.core.api.sync.RedisServerCommands
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.output.StatusOutput
import io.lettuce.core.protocol.CommandArgs
import io.lettuce.core.protocol.ProtocolKeyword
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.roberthu.rs.domain.BinarySafeString
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.CreateRedisKeyRequest
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.HashEntry
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisKeyPayload
import org.roberthu.rs.domain.HashScanPage
import org.roberthu.rs.domain.KeyMetadata
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.domain.SetScanPage
import org.roberthu.rs.domain.ZSetEntry
import org.roberthu.rs.domain.ZSetScanPage
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.port.KeyBrowserPort
import org.roberthu.rs.port.KeyCommandPort
import org.roberthu.rs.port.RedisConnectionPort
import org.roberthu.rs.port.RedisDataPort
import org.roberthu.rs.util.SensitiveRedactor
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.util.Base64
import java.util.concurrent.CompletionStage
import kotlin.math.min

class LettuceRedisConnection(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val reconnectBaseMs: Long = 250,
    private val clientFactory: LettuceClientFactory = LettuceClientFactory(),
) : RedisConnectionPort, KeyBrowserPort, KeyCommandPort, RedisDataPort, AutoCloseable {
    private val logger = LoggerFactory.getLogger(LettuceRedisConnection::class.java)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val resourceLock = Any()

    @Volatile
    private var client: LettuceClientHandle? = null

    @Volatile
    private var connection: StatefulConnection<String, String>? = null

    @Volatile
    private var binaryConnection: StatefulConnection<ByteArray, ByteArray>? = null

    @Volatile
    private var reconnectJob: Job? = null

    @Volatile
    private var closed = false

    private val databaseSession = RedisDatabaseSession()

    override fun connectionState(): StateFlow<ConnectionState> = state

    override suspend fun test(profile: ConnectionProfile): Result<Unit> {
        validateProfile(profile)?.let { return Result.failure(it) }

        return withContext(Dispatchers.IO) {
            var testClient: LettuceClientHandle? = null
            var testConnection: StatefulConnection<String, String>? = null
            try {
                testClient = clientFactory.create(profile)
                testConnection = testClient.connect(StringCodec.UTF8)
                testConnection.syncClusterCommands().ping()
                Result.success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                Result.failure(LettuceExceptionMapper.map(failure))
            } finally {
                testConnection.closeQuietly()
                testClient.shutdownQuietly()
            }
        }
    }

    override suspend fun connect(profile: ConnectionProfile): Result<Unit> {
        validateProfile(profile)?.let { return Result.failure(it) }
        if (closed) {
            return Result.failure(RedisError.ConnectionClosed("Redis adapter is closed"))
        }

        reconnectJob?.cancelAndJoin()
        reconnectJob = null
        closeActiveResources()
        state.value = ConnectionState.Connecting
        logger.info("Connecting to Redis profile {}", SensitiveRedactor.redact(profile.name))

        return try {
            openAndStore(profile)
            state.value = ConnectionState.Connected(profile.id, profile.name)
            logger.info("Connected to Redis profile {}", SensitiveRedactor.redact(profile.name))
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            state.value = ConnectionState.Disconnected
            throw cancellation
        } catch (failure: Throwable) {
            val error = LettuceExceptionMapper.map(failure)
            state.value = ConnectionState.Failed(error)
            logger.warn("Redis connection failed for {}: {}", profile.name, error.message)
            // Auth/validation failures will not succeed on retry — do not spin reconnect.
            if (error !is RedisError.AuthFailed && error !is RedisError.Validation) {
                scheduleReconnect(profile)
            }
            Result.failure(error)
        }
    }

    override suspend fun disconnect() {
        reconnectJob?.cancelAndJoin()
        reconnectJob = null
        closeActiveResources()
        state.value = ConnectionState.Disconnected
        logger.info("Disconnected from Redis")
    }

    override suspend fun scan(query: ScanQuery): Result<ScanPage> {
        if (query.database < 0) {
            return Result.failure(RedisError.Validation("database must not be negative"))
        }
        if (query.countHint <= 0) {
            return Result.failure(RedisError.Validation("countHint must be positive"))
        }
        if (query.type == RedisKeyType.Other || query.type == RedisKeyType.Unknown) {
            return Result.failure(
                RedisError.NotSupported("SCAN TYPE requires a concrete Redis key type"),
            )
        }

        val activeConnection = connection
            ?: return Result.failure(RedisError.ConnectionClosed("Redis connection is not active"))
        if (activeConnection is StatefulRedisClusterConnection<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val clusterConnection =
                activeConnection as StatefulRedisClusterConnection<String, String>
            return withContext(Dispatchers.IO) {
                try {
                    Result.success(ClusterKeyScanner(clusterConnection).scan(query))
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    Result.failure(LettuceExceptionMapper.map(failure))
                }
            }
        }

        return executeOnDatabaseSuspend(query.database, updateSelected = true) { commands ->
            scanPage(
                LettuceScanCommands.fromNodeConnection(activeConnection, commands),
                query,
            )
        }
    }

    override fun cancelScan() = Unit

    override suspend fun listDatabases(): Result<List<RedisDatabaseSummary>> =
        withContext(Dispatchers.IO) {
            try {
                val activeConnection = connection
                    ?: return@withContext Result.failure(
                        RedisError.ConnectionClosed("Redis connection is not active"),
                    )
                if (activeConnection is StatefulRedisClusterConnection<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val clusterConnection =
                        activeConnection as StatefulRedisClusterConnection<String, String>
                    val commands = clusterConnection.sync()
                    val keyCount = commands.dbsize()
                    return@withContext Result.success(
                        listOf(RedisDatabaseSummary(index = 0, keyCount = keyCount, selectable = true)),
                    )
                }

                val commands = activeConnection.syncClusterCommands() as RedisCommands<String, String>
                val databaseCount = readDatabaseCount(commands)
                val keyCounts = parseKeyspaceInfo(commands.info("keyspace"))
                Result.success(
                    (0 until databaseCount).map { index ->
                        RedisDatabaseSummary(
                            index = index,
                            keyCount = keyCounts[index] ?: 0L,
                            selectable = true,
                        )
                    },
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                Result.success(fallbackDatabaseList())
            }
        }

    override suspend fun selectDatabase(index: Int): Result<Unit> {
        if (index < 0) {
            return Result.failure(RedisError.Validation("database must not be negative"))
        }
        if (isClusterConnection()) {
            if (index != 0) {
                return Result.failure(
                    RedisError.NotSupported("Cluster mode only supports database 0"),
                )
            }
            databaseSession.initialize(0)
            return Result.success(Unit)
        }
        return executeOnDatabase(index, updateSelected = true) { }
    }

    override suspend fun exists(key: String, database: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                if (database < 0) {
                    return@withContext Result.failure(
                        RedisError.Validation("database must not be negative"),
                    )
                }
                val activeConnection = connection
                    ?: return@withContext Result.failure(
                        RedisError.ConnectionClosed("Redis connection is not active"),
                    )
                if (activeConnection is StatefulRedisClusterConnection<*, *>) {
                    if (database != 0) {
                        return@withContext Result.failure(
                            RedisError.Validation("Cluster mode only supports database 0"),
                        )
                    }
                    @Suppress("UNCHECKED_CAST")
                    val clusterConnection =
                        activeConnection as StatefulRedisClusterConnection<String, String>
                    val commands = clusterConnection.sync()
                    return@withContext Result.success(commands.exists(key) > 0L)
                }

                Result.success(
                    withActiveSession {
                        onTemporaryDatabase(database) { commands ->
                            commands.exists(key) > 0
                        }
                    },
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                Result.failure(LettuceExceptionMapper.map(failure))
            }
        }

    override suspend fun createKey(request: CreateRedisKeyRequest): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val activeConnection = connection
                    ?: return@withContext Result.failure(
                        RedisError.ConnectionClosed("Redis connection is not active"),
                    )
                if (activeConnection is StatefulRedisClusterConnection<*, *>) {
                    if (request.database != 0) {
                        return@withContext Result.failure(
                            RedisError.Validation("Cluster mode only supports database 0"),
                        )
                    }
                }

                withActiveSession {
                    onDatabase(request.database, updateSelected = true) { commands ->
                        if (commands.exists(request.key) > 0) {
                            throw RedisError.Validation("该键已存在")
                        }
                        createKeyPayload(commands, request)
                    }
                }
                Result.success(Unit)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                Result.failure(LettuceExceptionMapper.map(failure))
            }
        }

    override suspend fun isRedisJsonAvailable(): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val commands = connection?.syncClusterCommands()
                    ?: return@withContext Result.success(false)
                Result.success(detectRedisJsonAvailable(commands))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                Result.success(false)
            }
        }

    override suspend fun metadata(key: String): Result<KeyMetadata> = execute { commands ->
        val typeName = commands.type(key)
        if (typeName == "none") {
            throw RedisError.Unknown("Redis key '$key' does not exist")
        }
        val ttl = commands.ttl(key)
        if (ttl == -2L) {
            throw RedisError.Unknown("Redis key '$key' does not exist")
        }
        KeyMetadata(
            key = key,
            type = typeName.toRedisKeyType(),
            ttlSeconds = ttl.takeUnless { it == -1L },
            memoryBytes = optionalMetadata { commands.memoryUsage(key) },
            encoding = optionalMetadata { commands.objectEncoding(key) },
        )
    }

    override suspend fun rename(from: String, to: String): Result<Unit> = execute { commands ->
        commands.rename(from, to)
        return@execute Unit
    }

    override suspend fun delete(keys: List<String>): Result<Long> {
        if (keys.isEmpty()) return Result.success(0)
        return execute { commands ->
            var deleted = 0L
            keys.chunked(UNLINK_BATCH_SIZE).forEach { batch ->
                val arguments = batch.toTypedArray()
                deleted += try {
                    commands.unlink(*arguments)
                } catch (failure: RedisCommandExecutionException) {
                    if (!failure.isUnsupportedUnlink()) throw failure
                    commands.del(*arguments)
                }
            }
            deleted
        }
    }

    override suspend fun setTtl(key: String, ttlSeconds: Long?): Result<Unit> {
        if (ttlSeconds != null && ttlSeconds < 0) {
            return Result.failure(RedisError.Validation("ttlSeconds must not be negative"))
        }
        return execute { commands ->
            val updated = if (ttlSeconds == null) {
                commands.persist(key)
            } else {
                commands.expire(key, ttlSeconds)
            }
            if (!updated) {
                throw RedisError.Unknown("Redis key '$key' does not exist")
            }
            return@execute Unit
        }
    }

    override suspend fun getString(key: String, maxBytes: Int): Result<BinarySafeString> {
        if (maxBytes < 0) {
            return Result.failure(RedisError.Validation("maxBytes must not be negative"))
        }
        return executeBinary { commands ->
            val value = commands.get(key.toRedisBytes())
                ?: throw RedisError.Unknown("Redis key '$key' does not exist")
            val truncated = value.size > maxBytes
            value.copyOf(min(value.size, maxBytes)).toBinarySafeString(truncated)
        }
    }

    override suspend fun setString(key: String, value: String): Result<Unit> =
        executeBinary { commands ->
            commands.set(key.toRedisBytes(), value.toRedisBytes()).discardResult()
        }

    override suspend fun hscan(
        key: String,
        cursor: String?,
        count: Int,
    ): Result<HashScanPage> {
        positiveCountError(count)?.let { return Result.failure(it) }
        return executeBinary { commands ->
            val page = commands.hscan(
                key.toRedisBytes(),
                ScanCursor.of(cursor ?: "0"),
                ScanArgs().limit(count.toLong()),
            )
            HashScanPage(
                entries = page.map.entries.map { (field, value) ->
                    HashEntry(
                        field = field.toBinarySafeString(),
                        value = value.toBinarySafeString(),
                    )
                },
                nextCursorToken = page.cursor.takeUnless { page.isFinished },
            )
        }
    }

    override suspend fun hset(key: String, field: String, value: String): Result<Unit> =
        executeBinary { commands ->
            commands.hset(key.toRedisBytes(), field.toRedisBytes(), value.toRedisBytes()).discardResult()
        }

    override suspend fun hdel(key: String, fields: List<String>): Result<Long> {
        if (fields.isEmpty()) return Result.success(0)
        return executeBinary { commands ->
            commands.hdel(key.toRedisBytes(), *fields.toRedisByteArrays())
        }
    }

    override suspend fun lrange(
        key: String,
        start: Long,
        stop: Long,
    ): Result<List<BinarySafeString>> = executeBinary { commands ->
        commands.lrange(key.toRedisBytes(), start, stop).map { it.toBinarySafeString() }
    }

    override suspend fun lpush(key: String, values: List<String>): Result<Long> {
        if (values.isEmpty()) return Result.success(0)
        return executeBinary { commands ->
            commands.lpush(key.toRedisBytes(), *values.toRedisByteArrays())
        }
    }

    override suspend fun rpush(key: String, values: List<String>): Result<Long> {
        if (values.isEmpty()) return Result.success(0)
        return executeBinary { commands ->
            commands.rpush(key.toRedisBytes(), *values.toRedisByteArrays())
        }
    }

    override suspend fun lset(
        key: String,
        index: Long,
        value: String,
    ): Result<Unit> = executeBinary { commands ->
        commands.lset(key.toRedisBytes(), index, value.toRedisBytes()).discardResult()
    }

    override suspend fun lrem(
        key: String,
        count: Long,
        value: String,
    ): Result<Long> = executeBinary { commands ->
        commands.lrem(key.toRedisBytes(), count, value.toRedisBytes())
    }

    override suspend fun sscan(
        key: String,
        cursor: String?,
        count: Int,
    ): Result<SetScanPage> {
        positiveCountError(count)?.let { return Result.failure(it) }
        return executeBinary { commands ->
            val page = commands.sscan(
                key.toRedisBytes(),
                ScanCursor.of(cursor ?: "0"),
                ScanArgs().limit(count.toLong()),
            )
            SetScanPage(
                members = page.values.map { it.toBinarySafeString() },
                nextCursorToken = page.cursor.takeUnless { page.isFinished },
            )
        }
    }

    override suspend fun sadd(key: String, members: List<String>): Result<Long> {
        if (members.isEmpty()) return Result.success(0)
        return executeBinary { commands ->
            commands.sadd(key.toRedisBytes(), *members.toRedisByteArrays())
        }
    }

    override suspend fun srem(key: String, members: List<String>): Result<Long> {
        if (members.isEmpty()) return Result.success(0)
        return executeBinary { commands ->
            commands.srem(key.toRedisBytes(), *members.toRedisByteArrays())
        }
    }

    override suspend fun zscan(
        key: String,
        cursor: String?,
        count: Int,
    ): Result<ZSetScanPage> {
        positiveCountError(count)?.let { return Result.failure(it) }
        return executeBinary { commands ->
            val page = commands.zscan(
                key.toRedisBytes(),
                ScanCursor.of(cursor ?: "0"),
                ScanArgs().limit(count.toLong()),
            )
            ZSetScanPage(
                entries = page.values.map { value ->
                    ZSetEntry(value.value.toBinarySafeString(), value.score)
                },
                nextCursorToken = page.cursor.takeUnless { page.isFinished },
            )
        }
    }

    override suspend fun zadd(
        key: String,
        score: Double,
        member: String,
    ): Result<Long> {
        if (!score.isFinite()) {
            return Result.failure(RedisError.Validation("score must be finite"))
        }
        return executeBinary { commands ->
            commands.zadd(key.toRedisBytes(), score, member.toRedisBytes())
        }
    }

    override suspend fun zrem(key: String, members: List<String>): Result<Long> {
        if (members.isEmpty()) return Result.success(0)
        return executeBinary { commands ->
            commands.zrem(key.toRedisBytes(), *members.toRedisByteArrays())
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        reconnectJob?.cancel()
        reconnectJob = null
        runBlocking {
            databaseSession.withExclusiveLock {
                closeActiveResourcesBlocking()
            }
        }
        state.value = ConnectionState.Disconnected
        scope.cancel()
    }

    private fun scheduleReconnect(profile: ConnectionProfile) {
        if (closed) return
        reconnectJob = scope.launch {
            var attempt = 1
            while (!closed) {
                state.value = ConnectionState.Reconnecting(attempt)
                delay(backoffMs(attempt, profile.timeouts.reconnectMs))
                try {
                    openAndStore(profile)
                    state.value = ConnectionState.Connected(profile.id, profile.name)
                    reconnectJob = null
                    return@launch
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    state.value = ConnectionState.Failed(LettuceExceptionMapper.map(failure))
                    attempt += 1
                }
            }
        }
    }

    private suspend fun openAndStore(profile: ConnectionProfile) = withContext(Dispatchers.IO) {
        val newClient = clientFactory.create(profile)
        var newConnection: StatefulConnection<String, String>? = null
        var newBinaryConnection: StatefulConnection<ByteArray, ByteArray>? = null
        try {
            newConnection = newClient.connect(StringCodec.UTF8)
            newBinaryConnection = newClient.connect(ByteArrayCodec.INSTANCE)
            newConnection.syncClusterCommands().ping()
            val isCluster = newConnection is StatefulRedisClusterConnection<*, *>
            if (isCluster) {
                databaseSession.initialize(0)
            } else {
                databaseSession.selectBothOnConnect(
                    profile.database,
                    newConnection,
                    newBinaryConnection,
                )
            }
            synchronized(resourceLock) {
                connection = newConnection
                binaryConnection = newBinaryConnection
                client = newClient
            }
        } catch (failure: Throwable) {
            newConnection.closeQuietly()
            newBinaryConnection.closeQuietly()
            newClient.shutdownQuietly()
            throw failure
        }
    }

    private suspend fun closeActiveResources() = withContext(Dispatchers.IO) {
        databaseSession.withExclusiveLock {
            closeActiveResourcesBlocking()
        }
    }

    private fun closeActiveResourcesBlocking() {
        val oldConnection: StatefulConnection<String, String>?
        val oldBinaryConnection: StatefulConnection<ByteArray, ByteArray>?
        val oldClient: LettuceClientHandle?
        synchronized(resourceLock) {
            oldConnection = connection
            oldBinaryConnection = binaryConnection
            oldClient = client
            connection = null
            binaryConnection = null
            client = null
        }
        oldConnection.closeQuietly()
        oldBinaryConnection.closeQuietly()
        oldClient.shutdownQuietly()
    }

    private fun backoffMs(attempt: Int, capMs: Long): Long {
        var delayMs = reconnectBaseMs.coerceAtLeast(1)
        repeat((attempt - 1).coerceIn(0, 62)) {
            if (delayMs >= capMs || delayMs > Long.MAX_VALUE / 2) return min(delayMs, capMs)
            delayMs *= 2
        }
        return min(delayMs, capMs)
    }

    private fun validateProfile(profile: ConnectionProfile): RedisError? =
        profile.validate()
            .takeIf { it.isNotEmpty() }
            ?.let { RedisError.Validation(it.joinToString("; ")) }

    private suspend fun <T> executeOnDatabase(
        database: Int,
        updateSelected: Boolean,
        operation: (RedisClusterCommands<String, String>) -> T,
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            withActiveSession {
                onDatabase(database, updateSelected, operation)
            }.let { Result.success(it) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Result.failure(LettuceExceptionMapper.map(failure))
        }
    }

    private suspend fun <T> executeOnDatabaseSuspend(
        database: Int,
        updateSelected: Boolean,
        operation: suspend (RedisClusterCommands<String, String>) -> T,
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            withActiveSession {
                onDatabaseSuspend(database, updateSelected, operation)
            }.let { Result.success(it) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Result.failure(LettuceExceptionMapper.map(failure))
        }
    }

    private suspend fun <T> execute(
        operation: (RedisClusterCommands<String, String>) -> T,
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            withActiveSession {
                onString(operation)
            }.let { Result.success(it) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Result.failure(LettuceExceptionMapper.map(failure))
        }
    }

    private suspend fun <T> executeBinary(
        operation: (RedisClusterCommands<ByteArray, ByteArray>) -> T,
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            withActiveSession {
                onBinary(operation)
            }.let { Result.success(it) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            Result.failure(LettuceExceptionMapper.map(failure))
        }
    }

    private suspend fun <T> withActiveSession(
        block: suspend RedisDatabaseSession.SessionScope.() -> T,
    ): T {
        val activeStringConnection = connection
            ?: throw RedisError.ConnectionClosed("Redis connection is not active")
        val activeBinaryConnection = binaryConnection
            ?: throw RedisError.ConnectionClosed("Redis connection is not active")
        return databaseSession.withLock(
            isCluster = activeStringConnection is StatefulRedisClusterConnection<*, *>,
            stringConnection = activeStringConnection,
            binaryConnection = activeBinaryConnection,
            block = block,
        )
    }

    private fun isClusterConnection(): Boolean =
        connection is StatefulRedisClusterConnection<*, *>

    private fun createKeyPayload(
        commands: RedisClusterCommands<String, String>,
        request: CreateRedisKeyRequest,
    ) {
        when (request.type) {
            RedisKeyType.String -> {
                val payload = request.payload as RedisKeyPayload.StringPayload
                val ttl = effectiveTtl(request.ttlSeconds)
                if (ttl != null) {
                    commands.setex(request.key, ttl, payload.value)
                } else {
                    commands.set(request.key, payload.value)
                }
            }

            RedisKeyType.Hash -> {
                val payload = request.payload as RedisKeyPayload.HashPayload
                try {
                    if (payload.fields.isNotEmpty()) {
                        commands.hset(request.key, payload.fields.toMap())
                    }
                    applyTtl(commands, request.key, request.ttlSeconds)
                } catch (failure: Throwable) {
                    cleanupKey(commands, request.key)
                    throw failure
                }
            }

            RedisKeyType.List -> {
                val payload = request.payload as RedisKeyPayload.ListPayload
                try {
                    commands.rpush(request.key, *payload.values.toTypedArray())
                    applyTtl(commands, request.key, request.ttlSeconds)
                } catch (failure: Throwable) {
                    cleanupKey(commands, request.key)
                    throw failure
                }
            }

            RedisKeyType.Set -> {
                val payload = request.payload as RedisKeyPayload.SetPayload
                try {
                    commands.sadd(request.key, *payload.members.toTypedArray())
                    applyTtl(commands, request.key, request.ttlSeconds)
                } catch (failure: Throwable) {
                    cleanupKey(commands, request.key)
                    throw failure
                }
            }

            RedisKeyType.ZSet -> {
                val payload = request.payload as RedisKeyPayload.ZSetPayload
                try {
                    if (payload.entries.isNotEmpty()) {
                        val scored = payload.entries.associate { (score, member) -> member to score }
                        commands.zadd(request.key, scored)
                    }
                    applyTtl(commands, request.key, request.ttlSeconds)
                } catch (failure: Throwable) {
                    cleanupKey(commands, request.key)
                    throw failure
                }
            }

            RedisKeyType.Stream -> {
                val payload = request.payload as RedisKeyPayload.StreamPayload
                val body = payload.fields.toMap()
                try {
                    if (payload.id == "*") {
                        commands.xadd(request.key, body)
                    } else {
                        commands.xadd(request.key, XAddArgs().id(payload.id), body)
                    }
                    applyTtl(commands, request.key, request.ttlSeconds)
                } catch (failure: Throwable) {
                    cleanupKey(commands, request.key)
                    throw failure
                }
            }

            RedisKeyType.Json -> {
                val payload = request.payload as RedisKeyPayload.JsonPayload
                try {
                    commands.jsonSet(request.key, payload.json)
                    applyTtl(commands, request.key, request.ttlSeconds)
                } catch (failure: RedisCommandExecutionException) {
                    cleanupKey(commands, request.key)
                    if (failure.isUnknownJsonCommand()) {
                        throw RedisError.NotSupported("RedisJSON module is not available")
                    }
                    throw failure
                } catch (failure: Throwable) {
                    cleanupKey(commands, request.key)
                    throw failure
                }
            }

            RedisKeyType.Other, RedisKeyType.Unknown ->
                throw RedisError.NotSupported("Cannot create key type ${request.type}")
        }
    }

    private fun effectiveTtl(ttlSeconds: Long?): Long? =
        when (ttlSeconds) {
            null, -1L -> null
            else -> ttlSeconds
        }

    private fun applyTtl(
        commands: RedisClusterCommands<String, String>,
        key: String,
        ttlSeconds: Long?,
    ) {
        effectiveTtl(ttlSeconds)?.let { commands.expire(key, it) }
    }

    private fun cleanupKey(commands: RedisClusterCommands<String, String>, key: String) {
        try {
            commands.del(key)
        } catch (_: Throwable) {
            // Best-effort cleanup must not mask the original failure.
        }
    }

    private fun detectRedisJsonAvailable(commands: RedisClusterCommands<String, String>): Boolean {
        try {
            val info = (commands as RedisServerCommands<String, String>).commandInfo("JSON.SET")
            if (!info.isNullOrEmpty()) return true
        } catch (_: Throwable) {
            // Fall through to MODULE LIST.
        }
        return try {
            val output = NestedMultiOutput(StringCodec.UTF8)
            commands.dispatch(
                RedisAdminCommand.MODULE,
                output,
                CommandArgs(StringCodec.UTF8).add("LIST"),
            )
            output.get().toString().let { modulesText ->
                isRedisJsonModuleName(modulesText) ||
                    modulesText.contains("ReJSON", ignoreCase = true) ||
                    modulesText.contains("redisjson", ignoreCase = true)
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun isRedisJsonModuleName(name: String): Boolean =
        name.equals("ReJSON-RL", ignoreCase = true) ||
            name.equals("json", ignoreCase = true) ||
            name.contains("redisjson", ignoreCase = true)

    private fun readDatabaseCount(commands: RedisCommands<String, String>): Int =
        try {
            commands.configGet("databases")["databases"]?.toIntOrNull()?.coerceIn(1, 256)
                ?: DEFAULT_DATABASE_COUNT
        } catch (_: Throwable) {
            DEFAULT_DATABASE_COUNT
        }

    private fun parseKeyspaceInfo(info: String): Map<Int, Long> {
        val pattern = Regex("""db(\d+):keys=(\d+)""")
        return pattern.findAll(info).associate { match ->
            match.groupValues[1].toInt() to match.groupValues[2].toLong()
        }
    }

    private fun fallbackDatabaseList(): List<RedisDatabaseSummary> =
        (0 until DEFAULT_DATABASE_COUNT).map { index ->
            RedisDatabaseSummary(index = index, keyCount = 0L, selectable = true)
        }

    private fun positiveCountError(count: Int): RedisError? =
        RedisError.Validation("count must be positive").takeIf { count <= 0 }

    private fun RedisCommandExecutionException.isUnsupportedUnlink(): Boolean {
        val message = message.orEmpty().lowercase()
        return "unknown command" in message && "unlink" in message
    }

    private inline fun <T> optionalMetadata(read: () -> T): T? =
        try {
            read()
        } catch (_: RedisCommandExecutionException) {
            null
        }

    private fun <K, V> StatefulConnection<K, V>?.closeQuietly() {
        try {
            this?.close()
        } catch (_: Throwable) {
            // Cleanup must not mask the operation result.
        }
    }

    private fun LettuceClientHandle?.shutdownQuietly() {
        try {
            this?.close()
        } catch (_: Throwable) {
            // Cleanup must not mask the operation result.
        }
    }
}

private fun String.toRedisBytes(): ByteArray = encodeToByteArray()

private fun List<String>.toRedisByteArrays(): Array<ByteArray> =
    map(String::toRedisBytes).toTypedArray()

private const val DEFAULT_DATABASE_COUNT = 16
private const val UNLINK_BATCH_SIZE = 500

private enum class RedisJsonCommand : ProtocolKeyword {
    JSON_SET {
        override fun getBytes(): ByteArray = "JSON.SET".encodeToByteArray()
    },
}

private enum class RedisAdminCommand : ProtocolKeyword {
    MODULE {
        override fun getBytes(): ByteArray = "MODULE".encodeToByteArray()
    },
}

private fun RedisClusterCommands<String, String>.jsonSet(key: String, json: String) {
    dispatch(
        RedisJsonCommand.JSON_SET,
        StatusOutput(StringCodec.UTF8),
        CommandArgs(StringCodec.UTF8).add(key).add("$").add(json),
    )
}

private fun RedisCommandExecutionException.isUnknownJsonCommand(): Boolean {
    val message = message.orEmpty().lowercase()
    return "unknown command" in message && "json.set" in message
}

private fun Any?.discardResult() = Unit

@Suppress("UNCHECKED_CAST")
internal fun <K, V> StatefulConnection<K, V>.syncClusterCommands(): RedisClusterCommands<K, V> =
    when (this) {
        is StatefulRedisConnection<*, *> ->
            (this as StatefulRedisConnection<K, V>).sync()
        is StatefulRedisClusterConnection<*, *> ->
            (this as StatefulRedisClusterConnection<K, V>).sync()
        else -> error("Unsupported Lettuce connection type: ${this::class.qualifiedName}")
    }

private fun ByteArray.toBinarySafeString(truncated: Boolean = false): BinarySafeString {
    val utf8 = try {
        Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(this)).toString()
    } catch (_: CharacterCodingException) {
        null
    }
    return BinarySafeString(
        utf8 = utf8,
        base64 = Base64.getEncoder().encodeToString(this),
        truncated = truncated,
    )
}

internal data class ScanCommandRequest(
    val cursorToken: String,
    val pattern: String,
    val count: Long,
    val type: String?,
)

internal data class ScanCommandPage(
    val keys: List<String>,
    val nextCursorToken: String,
    val finished: Boolean,
)

internal interface ScanCommands {
    fun scan(request: ScanCommandRequest): ScanCommandPage
    fun type(key: String): String
}

internal class LettuceScanCommands(
    private val commands: RedisClusterCommands<String, String>,
    private val typeInvoker: suspend (String) -> String = { key -> commands.type(key) },
) : ScanCommands {
    override fun scan(request: ScanCommandRequest): ScanCommandPage {
        val args = KeyScanArgs()
            .match(request.pattern)
            // Lettuce 6.x names this API limit(), but it serializes Redis SCAN's COUNT argument.
            .limit(request.count)
        request.type?.let(args::type)
        val cursor = commands.scan(ScanCursor.of(request.cursorToken), args)
        return ScanCommandPage(cursor.keys, cursor.cursor, cursor.isFinished)
    }

    override fun type(key: String): String = commands.type(key)

    suspend fun resolveTypesAsync(
        keys: List<String>,
        concurrency: Int = DEFAULT_TYPE_CONCURRENCY,
    ): List<Pair<String, Result<String>>> = coroutineScope {
        val semaphore = Semaphore(concurrency.coerceAtLeast(1))
        keys.map { key ->
            async {
                semaphore.withPermit {
                    key to runCatching { typeInvoker(key) }
                }
            }
        }.awaitAll()
    }

    companion object {
        fun fromNodeConnection(
            connection: StatefulConnection<String, String>,
            commands: RedisClusterCommands<String, String>,
        ): LettuceScanCommands = LettuceScanCommands(commands) { key ->
            lettuceAsyncType(connection, key).awaitCompletionStage()
        }
    }
}

internal const val DEFAULT_TYPE_CONCURRENCY = 16

internal suspend fun scanPage(commands: ScanCommands, query: ScanQuery): ScanPage {
    val requestedType = query.type
    val request = ScanCommandRequest(
        cursorToken = query.cursorToken ?: "0",
        pattern = query.pattern,
        count = query.countHint.toLong(),
        type = requestedType?.redisTypeName(),
    )
    val (cursor, typeWasServerFiltered) = if (requestedType == null) {
        commands.scan(request) to false
    } else {
        try {
            commands.scan(request) to true
        } catch (failure: RedisCommandExecutionException) {
            if (!failure.isUnsupportedScanType()) throw failure
            commands.scan(request.copy(type = null)) to false
        }
    }

    val partialFailures = mutableListOf<String>()
    val keys = if (typeWasServerFiltered) {
        cursor.keys.map { key ->
            RedisKeySummary(key, requestedType!!)
        }
    } else {
        val resolvedTypes = when (commands) {
            is LettuceScanCommands -> commands.resolveTypesAsync(cursor.keys)
            else -> resolveKeyTypes(commands, cursor.keys)
        }
        resolvedTypes.mapNotNull { (key, typeResult) ->
            val type = typeResult.fold(
                onSuccess = { it.toRedisKeyType() },
                onFailure = { failure ->
                    if (failure is CancellationException) throw failure
                    partialFailures += "$key: ${failure.message ?: "TYPE failed"}"
                    RedisKeyType.Unknown
                },
            )
            RedisKeySummary(key, type).takeIf { requestedType == null || type == requestedType }
        }
    }
    return ScanPage(
        keys = keys,
        nextCursorToken = cursor.nextCursorToken.takeUnless { cursor.finished },
        partialFailures = partialFailures,
    )
}

internal suspend fun resolveKeyTypes(
    commands: ScanCommands,
    keys: List<String>,
    concurrency: Int = DEFAULT_TYPE_CONCURRENCY,
): List<Pair<String, Result<String>>> = coroutineScope {
    val semaphore = Semaphore(concurrency.coerceAtLeast(1))
    keys.map { key ->
        async {
            semaphore.withPermit {
                key to runCatching { commands.type(key) }
            }
        }
    }.awaitAll()
}

internal suspend fun <T> CompletionStage<T>.awaitCompletionStage(): T =
    suspendCancellableCoroutine { continuation ->
        whenComplete { value, failure ->
            when {
                failure != null -> {
                    if (failure is CancellationException) {
                        continuation.cancel(failure)
                    } else {
                        continuation.resumeWithException(failure)
                    }
                }
                else -> continuation.resume(value)
            }
        }
    }

@Suppress("UNCHECKED_CAST")
internal fun lettuceAsyncType(
    connection: StatefulConnection<String, String>,
    key: String,
): CompletionStage<String> =
    when (connection) {
        is StatefulRedisConnection<*, *> ->
            (connection as StatefulRedisConnection<String, String>).async().type(key)
        is StatefulRedisClusterConnection<*, *> ->
            (connection as StatefulRedisClusterConnection<String, String>).async().type(key)
        else -> error("Unsupported Lettuce connection type: ${connection::class.qualifiedName}")
    }

private fun RedisKeyType.redisTypeName(): String = when (this) {
    RedisKeyType.String -> "string"
    RedisKeyType.Hash -> "hash"
    RedisKeyType.List -> "list"
    RedisKeyType.Set -> "set"
    RedisKeyType.ZSet -> "zset"
    RedisKeyType.Stream -> "stream"
    RedisKeyType.Json -> "ReJSON-RL"
    RedisKeyType.Other, RedisKeyType.Unknown -> error("A concrete Redis type is required")
}

internal fun String.toRedisKeyType(): RedisKeyType = when (lowercase()) {
    "string" -> RedisKeyType.String
    "hash" -> RedisKeyType.Hash
    "list" -> RedisKeyType.List
    "set" -> RedisKeyType.Set
    "zset" -> RedisKeyType.ZSet
    "stream" -> RedisKeyType.Stream
    "rejson-rl", "json" -> RedisKeyType.Json
    "none" -> RedisKeyType.Unknown
    else -> RedisKeyType.Other
}

private fun RedisCommandExecutionException.isUnsupportedScanType(): Boolean {
    val message = message.orEmpty().lowercase()
    return "syntax error" in message ||
        ("unknown command" in message && ("scan" in message || "type" in message)) ||
        ("unsupported" in message && "type" in message)
}
