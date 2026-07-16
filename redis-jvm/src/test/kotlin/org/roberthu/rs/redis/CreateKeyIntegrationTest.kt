package org.roberthu.rs.redis

import io.lettuce.core.RedisClient
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.CreateRedisKeyRequest
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeyPayload
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.usecase.CreateRedisKey
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CreateKeyIntegrationTest {
    @Test
    fun createsKeysOfAllSupportedTypes() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = startRedis()
        try {
            val profile = standaloneProfile(redis)
            LettuceRedisConnection().use { adapter ->
                adapter.connect(profile).getOrThrow()
                val createKey = CreateRedisKey(adapter)

                createKey.create(
                    CreateRedisKeyRequest(
                        database = 0,
                        key = "new:string",
                        type = RedisKeyType.String,
                        ttlSeconds = null,
                        payload = RedisKeyPayload.StringPayload("hello"),
                    ),
                ).getOrThrow()
                assertEquals("hello", adapter.getString("new:string", 100).getOrThrow().utf8)

                createKey.create(
                    CreateRedisKeyRequest(
                        database = 0,
                        key = "new:string:ttl",
                        type = RedisKeyType.String,
                        ttlSeconds = 60,
                        payload = RedisKeyPayload.StringPayload("expires"),
                    ),
                ).getOrThrow()
                val ttl = assertNotNull(adapter.metadata("new:string:ttl").getOrThrow().ttlSeconds)
                assertTrue(ttl in 1L..60L)

                createKey.create(
                    CreateRedisKeyRequest(
                        database = 0,
                        key = "new:hash",
                        type = RedisKeyType.Hash,
                        ttlSeconds = null,
                        payload = RedisKeyPayload.HashPayload(listOf("a" to "1", "b" to "2")),
                    ),
                ).getOrThrow()
                assertEquals(RedisKeyType.Hash, adapter.metadata("new:hash").getOrThrow().type)

                createKey.create(
                    CreateRedisKeyRequest(
                        database = 0,
                        key = "new:list",
                        type = RedisKeyType.List,
                        ttlSeconds = null,
                        payload = RedisKeyPayload.ListPayload(listOf("one", "two", "three")),
                    ),
                ).getOrThrow()
                assertEquals(
                    listOf("one", "two", "three"),
                    adapter.lrange("new:list", 0, -1).getOrThrow().map { it.utf8 },
                )

                createKey.create(
                    CreateRedisKeyRequest(
                        database = 0,
                        key = "new:set",
                        type = RedisKeyType.Set,
                        ttlSeconds = null,
                        payload = RedisKeyPayload.SetPayload(listOf("alpha", "beta")),
                    ),
                ).getOrThrow()
                assertEquals(RedisKeyType.Set, adapter.metadata("new:set").getOrThrow().type)

                createKey.create(
                    CreateRedisKeyRequest(
                        database = 0,
                        key = "new:zset",
                        type = RedisKeyType.ZSet,
                        ttlSeconds = null,
                        payload = RedisKeyPayload.ZSetPayload(listOf(1.0 to "low", 2.0 to "high")),
                    ),
                ).getOrThrow()
                assertEquals(RedisKeyType.ZSet, adapter.metadata("new:zset").getOrThrow().type)

                createKey.create(
                    CreateRedisKeyRequest(
                        database = 0,
                        key = "new:stream",
                        type = RedisKeyType.Stream,
                        ttlSeconds = null,
                        payload = RedisKeyPayload.StreamPayload(
                            fields = listOf("field" to "value"),
                        ),
                    ),
                ).getOrThrow()
                assertEquals(RedisKeyType.Stream, adapter.metadata("new:stream").getOrThrow().type)
            }
        } finally {
            redis.stop()
        }
    }

    @Test
    fun rejectsDuplicateKey() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = startRedis()
        try {
            val profile = standaloneProfile(redis)
            LettuceRedisConnection().use { adapter ->
                adapter.connect(profile).getOrThrow()
                val createKey = CreateRedisKey(adapter)
                val request = CreateRedisKeyRequest(
                    database = 0,
                    key = "dup:key",
                    type = RedisKeyType.String,
                    ttlSeconds = null,
                    payload = RedisKeyPayload.StringPayload("first"),
                )
                createKey.create(request).getOrThrow()

                val duplicate = createKey.create(request.copy(payload = RedisKeyPayload.StringPayload("second")))
                assertTrue(duplicate.isFailure)
                assertTrue(duplicate.exceptionOrNull() is RedisError.Validation)
                assertTrue(duplicate.exceptionOrNull()!!.message!!.contains("该键已存在"))
            }
        } finally {
            redis.stop()
        }
    }

    @Test
    fun listsDatabasesSelectsDbAndScans() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = startRedis()
        try {
            val profile = standaloneProfile(redis)
            val redisUri = "redis://${redis.host}:${redis.getMappedPort(6379)}"
            RedisClient.create(redisUri).use { client ->
                client.connect().use { connection ->
                    connection.sync().select(1)
                    connection.sync().set("db1:key", "value")
                }
            }

            LettuceRedisConnection().use { adapter ->
                adapter.connect(profile).getOrThrow()

                val databases = adapter.listDatabases().getOrThrow()
                assertTrue(databases.size >= 16)
                assertTrue(databases.any { it.index == 1 && it.keyCount >= 1L })

                adapter.selectDatabase(1).getOrThrow()
                val keys = adapter.scanAll(ScanQuery(database = 1, pattern = "db1:*", countHint = 10))
                assertEquals(listOf(RedisKeySummary("db1:key", RedisKeyType.String)), keys)
            }
        } finally {
            redis.stop()
        }
    }

    @Test
    fun reportsRedisJsonUnavailableOnStockRedis() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = startRedis()
        try {
            val profile = standaloneProfile(redis)
            LettuceRedisConnection().use { adapter ->
                adapter.connect(profile).getOrThrow()
                assertFalse(adapter.isRedisJsonAvailable().getOrThrow())

                val createKey = CreateRedisKey(adapter)
                val result = createKey.create(
                    CreateRedisKeyRequest(
                        database = 0,
                        key = "new:json",
                        type = RedisKeyType.Json,
                        ttlSeconds = null,
                        payload = RedisKeyPayload.JsonPayload("""{"name":"test"}"""),
                    ),
                )
                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is RedisError.NotSupported)
            }
        } finally {
            redis.stop()
        }
    }

    private fun startRedis(): GenericContainer<Nothing> =
        GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
            start()
        }

    private fun standaloneProfile(redis: GenericContainer<Nothing>) = ConnectionProfile(
        id = "create-key-integration",
        name = "Create Key Integration Redis",
        mode = DeploymentMode.Standalone,
        host = redis.host,
        port = redis.getMappedPort(6379),
    )

    private suspend fun LettuceRedisConnection.scanAll(initial: ScanQuery): List<RedisKeySummary> {
        val keys = mutableListOf<RedisKeySummary>()
        var query = initial
        do {
            val page = scan(query).getOrThrow()
            keys += page.keys
            query = query.copy(cursorToken = page.nextCursorToken)
        } while (page.nextCursorToken != null)
        return keys
    }
}
