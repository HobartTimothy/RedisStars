package org.roberthu.rs.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RedisDataIntegrationTest {
    @Test
    fun readsAndEditsFiveTypesAndTtl() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
            start()
        }
        try {
            val profile = ConnectionProfile(
                id = "data-integration",
                name = "Data Integration Redis",
                mode = DeploymentMode.Standalone,
                host = redis.host,
                port = redis.getMappedPort(6379),
            )
            LettuceRedisConnection().use { adapter ->
                adapter.connect(profile).getOrThrow()

                adapter.setString("string", "hello").getOrThrow()
                assertEquals("hello", adapter.getString("string", 100).getOrThrow().utf8)
                val truncated = adapter.getString("string", 3).getOrThrow()
                assertEquals("hel", truncated.utf8)
                assertEquals("aGVs", truncated.base64)
                assertTrue(truncated.truncated)

                RedisClient.create("redis://${redis.host}:${redis.getMappedPort(6379)}").use { client ->
                    client.connect(ByteArrayCodec.INSTANCE).use { connection ->
                        connection.sync().set("binary".encodeToByteArray(), byteArrayOf(0xC3.toByte(), 0x28))
                    }
                }
                val binary = adapter.getString("binary", 100).getOrThrow()
                assertNull(binary.utf8)
                assertEquals("wyg=", binary.base64)

                adapter.hset("hash", "first", "one").getOrThrow()
                adapter.hset("hash", "second", "two").getOrThrow()
                assertEquals(
                    mapOf<String?, String?>("first" to "one", "second" to "two"),
                    adapter.hscanAll("hash").associate { it.first to it.second },
                )
                assertEquals(1, adapter.hdel("hash", listOf("first")).getOrThrow())

                adapter.rpush("list", listOf("two", "three")).getOrThrow()
                adapter.lpush("list", listOf("one")).getOrThrow()
                adapter.lset("list", 1, "TWO").getOrThrow()
                assertEquals(listOf("one", "TWO", "three"), adapter.lrange("list", 0, 10).getOrThrow().map { it.utf8 })
                assertEquals(1, adapter.lrem("list", 0, "TWO").getOrThrow())

                adapter.sadd("set", listOf("one", "two")).getOrThrow()
                assertEquals(setOf("one", "two"), adapter.sscanAll("set").toSet())
                assertEquals(1, adapter.srem("set", listOf("one")).getOrThrow())

                adapter.zadd("zset", 1.5, "one").getOrThrow()
                adapter.zadd("zset", 2.5, "two").getOrThrow()
                assertEquals(
                    mapOf<String?, Double>("one" to 1.5, "two" to 2.5),
                    adapter.zscanAll("zset").toMap(),
                )
                assertEquals(1, adapter.zrem("zset", listOf("one")).getOrThrow())

                adapter.setTtl("string", 60).getOrThrow()
                assertTrue(adapter.metadata("string").getOrThrow().ttlSeconds in 1L..60L)
                adapter.setTtl("string", null).getOrThrow()
                assertNull(adapter.metadata("string").getOrThrow().ttlSeconds)
            }
        } finally {
            redis.stop()
        }
    }

    private suspend fun LettuceRedisConnection.hscanAll(key: String): List<Pair<String?, String?>> {
        val entries = mutableListOf<Pair<String?, String?>>()
        var cursor: String? = null
        do {
            val page = hscan(key, cursor, 1).getOrThrow()
            entries += page.entries.map { it.field.utf8 to it.value.utf8 }
            cursor = page.nextCursorToken
        } while (cursor != null)
        return entries
    }

    private suspend fun LettuceRedisConnection.sscanAll(key: String): List<String?> {
        val members = mutableListOf<String?>()
        var cursor: String? = null
        do {
            val page = sscan(key, cursor, 1).getOrThrow()
            members += page.members.map { it.utf8 }
            cursor = page.nextCursorToken
        } while (cursor != null)
        return members
    }

    private suspend fun LettuceRedisConnection.zscanAll(key: String): List<Pair<String?, Double>> {
        val entries = mutableListOf<Pair<String?, Double>>()
        var cursor: String? = null
        do {
            val page = zscan(key, cursor, 1).getOrThrow()
            entries += page.entries.map { it.member.utf8 to it.score }
            cursor = page.nextCursorToken
        } while (cursor != null)
        return entries
    }
}
