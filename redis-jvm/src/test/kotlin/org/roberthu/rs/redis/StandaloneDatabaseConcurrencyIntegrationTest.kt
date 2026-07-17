package org.roberthu.rs.redis

import io.lettuce.core.RedisClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanQuery
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandaloneDatabaseConcurrencyIntegrationTest {
    @Test
    fun concurrentDatabaseZeroAndOneReadsAndWritesStayIsolated() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = startRedis()
        try {
            seedDatabaseKeys(redis)
            LettuceRedisConnection().use { adapter ->
                adapter.connect(standaloneProfile(redis)).getOrThrow()

                val results = coroutineScope {
                    (0 until 100).map { index ->
                        async(Dispatchers.IO) {
                            val database = index % 2
                            val key = "db$database:counter"
                            adapter.selectDatabase(database).getOrThrow()
                            adapter.setString(key, index.toString()).getOrThrow()
                            adapter.getString(key, maxBytes = 64).getOrThrow().utf8
                        }
                    }.awaitAll()
                }

                assertEquals(100, results.size)
                results.forEachIndexed { index, value ->
                    assertEquals(index.toString(), value)
                }
            }
        } finally {
            redis.stop()
        }
    }

    @Test
    fun interleavedStringAndBinaryCommandsStayConsistent() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = startRedis()
        try {
            LettuceRedisConnection().use { adapter ->
                adapter.connect(standaloneProfile(redis)).getOrThrow()
                adapter.selectDatabase(0).getOrThrow()
                adapter.setString("shared:key", "payload").getOrThrow()

                val outcomes = coroutineScope {
                    (0 until 50).map {
                        async(Dispatchers.IO) {
                            adapter.selectDatabase(0).getOrThrow()
                            val stringValue = adapter.getString("shared:key", maxBytes = 64).getOrThrow().utf8
                            val metadata = adapter.metadata("shared:key").getOrThrow()
                            stringValue to metadata.type
                        }
                    }.awaitAll()
                }

                outcomes.forEach { (value, type) ->
                    assertEquals("payload", value)
                    assertEquals(RedisKeyType.String, type)
                }
            }
        } finally {
            redis.stop()
        }
    }

    @Test
    fun concurrentScanAndMetadataOnSelectedDatabase() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = startRedis()
        try {
            seedDatabaseKeys(redis)
            LettuceRedisConnection().use { adapter ->
                adapter.connect(standaloneProfile(redis)).getOrThrow()
                adapter.selectDatabase(1).getOrThrow()

                val scanResults = coroutineScope {
                    (0 until 20).map {
                        async(Dispatchers.IO) {
                            adapter.scan(
                                ScanQuery(database = 1, pattern = "db1:*", countHint = 10),
                            ).getOrThrow().keys.map { it.key }.toSet()
                        }
                    }.awaitAll()
                }
                val metadataResults = coroutineScope {
                    (0 until 20).map {
                        async(Dispatchers.IO) {
                            adapter.selectDatabase(1).getOrThrow()
                            adapter.metadata("db1:key").getOrThrow().type
                        }
                    }.awaitAll()
                }

                scanResults.forEach { keys ->
                    assertEquals(setOf("db1:key"), keys)
                }
                metadataResults.forEach { type ->
                    assertEquals(RedisKeyType.String, type)
                }
            }
        } finally {
            redis.stop()
        }
    }

    @Test
    fun disconnectDuringInFlightCommandsFailsGracefullyWithoutClosingSharedConnectionEarly() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = startRedis()
        try {
            LettuceRedisConnection().use { adapter ->
                adapter.connect(standaloneProfile(redis)).getOrThrow()
                adapter.selectDatabase(0).getOrThrow()
                adapter.setString("inflight:key", "before").getOrThrow()

                val inFlight = async(Dispatchers.IO) {
                    repeat(20) {
                        adapter.selectDatabase(it % 2).getOrThrow()
                        adapter.getString("db${it % 2}:counter", maxBytes = 64)
                        delay(5)
                    }
                }

                delay(20)
                adapter.disconnect()
                runCatching { inFlight.await() }

                adapter.connect(standaloneProfile(redis)).getOrThrow()
                adapter.selectDatabase(0).getOrThrow()
                val restored = adapter.getString("inflight:key", maxBytes = 64).getOrThrow().utf8
                assertEquals("before", restored)
            }
        } finally {
            redis.stop()
        }
    }

    @Test
    fun existsOnTemporaryDatabaseDoesNotChangeSessionSelection() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = startRedis()
        try {
            seedDatabaseKeys(redis)
            LettuceRedisConnection().use { adapter ->
                adapter.connect(standaloneProfile(redis)).getOrThrow()
                adapter.selectDatabase(0).getOrThrow()

                assertTrue(adapter.exists("db1:key", database = 1).getOrThrow())

                val value = adapter.getString("db0:counter", maxBytes = 64).getOrThrow().utf8
                assertEquals("0", value)
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
        id = "db-concurrency",
        name = "Database Concurrency Redis",
        mode = DeploymentMode.Standalone,
        host = redis.host,
        port = redis.getMappedPort(6379),
    )

    private fun seedDatabaseKeys(redis: GenericContainer<Nothing>) {
        val uri = "redis://${redis.host}:${redis.getMappedPort(6379)}"
        RedisClient.create(uri).use { client ->
            client.connect().use { connection ->
                val sync = connection.sync()
                sync.select(0)
                sync.set("db0:counter", "0")
                sync.select(1)
                sync.set("db1:key", "value")
                sync.set("db1:counter", "1")
            }
        }
    }
}
