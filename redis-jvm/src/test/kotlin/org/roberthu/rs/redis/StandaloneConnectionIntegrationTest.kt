package org.roberthu.rs.redis

import io.lettuce.core.RedisClient
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.port.ConnectionState
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StandaloneConnectionIntegrationTest {
    @Test
    fun testsConnectsAndDisconnectsFromStandaloneRedis() {
        runBlocking {
            assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
            val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
                withExposedPorts(6379)
            }
            redis.start()

            try {
                val profile = ConnectionProfile(
                    id = "integration",
                    name = "Integration Redis",
                    mode = DeploymentMode.Standalone,
                    host = redis.host,
                    port = redis.getMappedPort(6379),
                )
                LettuceRedisConnection().use { adapter ->
                    assertTrue(adapter.test(profile).isSuccess)
                    assertTrue(adapter.connect(profile).isSuccess)
                    assertIs<ConnectionState.Connected>(adapter.connectionState().value)

                    RedisClient.create("redis://${redis.host}:${redis.getMappedPort(6379)}").use { seedClient ->
                        seedClient.connect().use { seed ->
                            seed.sync().set("user:one", "Ada")
                            seed.sync().hset("user:two", "name", "Grace")
                            seed.sync().rpush("queue", "first")
                        }
                    }

                    val users = adapter.scanAll(
                        ScanQuery(pattern = "user:*", countHint = 1),
                    )
                    assertEquals(
                        setOf(
                            RedisKeySummary("user:one", RedisKeyType.String),
                            RedisKeySummary("user:two", RedisKeyType.Hash),
                        ),
                        users.toSet(),
                    )

                    val hashes = adapter.scanAll(
                        ScanQuery(type = RedisKeyType.Hash, countHint = 1),
                    )
                    assertEquals(listOf(RedisKeySummary("user:two", RedisKeyType.Hash)), hashes)

                    val metadata = adapter.metadata("user:one").getOrThrow()
                    assertEquals(RedisKeyType.String, metadata.type)
                    assertNull(metadata.ttlSeconds)
                    assertNotNull(metadata.memoryBytes)
                    assertNotNull(metadata.encoding)

                    assertTrue(adapter.rename("user:one", "user:renamed").isSuccess)
                    assertTrue(adapter.setTtl("user:renamed", 60).isSuccess)
                    val ttl = assertNotNull(
                        adapter.metadata("user:renamed").getOrThrow().ttlSeconds,
                    )
                    assertTrue(ttl in 1L..60L)
                    assertTrue(adapter.setTtl("user:renamed", null).isSuccess)
                    assertNull(adapter.metadata("user:renamed").getOrThrow().ttlSeconds)
                    assertEquals(2L, adapter.delete(listOf("user:renamed", "user:two")).getOrThrow())

                    adapter.disconnect()
                    assertIs<ConnectionState.Disconnected>(adapter.connectionState().value)
                }
            } finally {
                redis.stop()
            }
        }
    }

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
