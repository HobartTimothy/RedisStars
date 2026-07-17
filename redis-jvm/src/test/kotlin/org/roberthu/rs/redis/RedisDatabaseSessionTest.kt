package org.roberthu.rs.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedisDatabaseSessionTest {
    @Test
    fun selectsBothConnectionsTogether() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
        }
        redis.start()
        try {
            val uri = "redis://${redis.host}:${redis.getMappedPort(6379)}"
            RedisClient.create(uri).use { client ->
                client.connect(StringCodec.UTF8).use { stringConnection ->
                    client.connect(ByteArrayCodec.INSTANCE).use { binaryConnection ->
                        val session = RedisDatabaseSession()
                        session.selectBothOnConnect(0, stringConnection, binaryConnection)
                        session.withLock(
                            isCluster = false,
                            stringConnection = stringConnection,
                            binaryConnection = binaryConnection,
                        ) {
                            selectSessionDatabase(1)
                            onString { commands -> commands.set("db1:string", "value") }
                            onBinary { commands ->
                                commands.set("db1:binary".encodeToByteArray(), "bytes".encodeToByteArray())
                            }
                        }
                        stringConnection.sync().select(1)
                        assertEquals("value", stringConnection.sync().get("db1:string"))
                        binaryConnection.sync().select(1)
                        assertTrue(binaryConnection.sync().exists("db1:binary".encodeToByteArray()) > 0L)
                    }
                }
            }
        } finally {
            redis.stop()
        }
    }

    @Test
    fun temporaryDatabaseRestoresSessionSelection() = runBlocking {
        assumeTrue("Docker is not available", DockerClientFactory.instance().isDockerAvailable)
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
        }
        redis.start()
        try {
            val uri = "redis://${redis.host}:${redis.getMappedPort(6379)}"
            RedisClient.create(uri).use { client ->
                client.connect(StringCodec.UTF8).use { stringConnection ->
                    client.connect(ByteArrayCodec.INSTANCE).use { binaryConnection ->
                        stringConnection.sync().select(0)
                        stringConnection.sync().set("db0:key", "zero")
                        stringConnection.sync().select(1)
                        stringConnection.sync().set("db1:key", "one")

                        val session = RedisDatabaseSession()
                        session.selectBothOnConnect(0, stringConnection, binaryConnection)
                        session.withLock(
                            isCluster = false,
                            stringConnection = stringConnection,
                            binaryConnection = binaryConnection,
                        ) {
                            val onDb1 = onTemporaryDatabase(1) { commands -> commands.get("db1:key") }
                            assertEquals("one", onDb1)
                            onString { commands -> commands.get("db0:key") }
                        }.let { assertEquals("zero", it) }
                    }
                }
            }
        } finally {
            redis.stop()
        }
    }
}
