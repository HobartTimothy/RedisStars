package org.roberthu.rs.domain

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectionProfileValidationTest {
    @Test
    fun redisValueCodecRoundTripsUtf8() {
        val encoded = RedisValueCodec.encode("Redis ★")

        assertContentEquals("Redis ★".encodeToByteArray(), encoded)
        assertEquals("Redis ★", RedisValueCodec.decode(encoded))
    }

    @Test
    fun clusterForcesDatabaseZero() {
        val profile = sampleStandalone().copy(
            mode = DeploymentMode.Cluster,
            seedNodes = listOf(HostPort("127.0.0.1", 7000)),
            database = 2,
        )

        assertTrue(profile.validate().any { it.contains("database", ignoreCase = true) })
    }

    @Test
    fun sentinelRequiresMasterNameAndNodes() {
        val profile = sampleStandalone().copy(
            mode = DeploymentMode.Sentinel,
            sentinelNodes = emptyList(),
            masterName = "",
        )

        assertTrue(profile.validate().isNotEmpty())
    }

    @Test
    fun nameMustNotBeBlank() {
        assertTrue(sampleStandalone().copy(name = " ").validate().any {
            it.contains("name", ignoreCase = true)
        })
    }

    @Test
    fun standaloneRequiresHostAndValidPort() {
        val errors = sampleStandalone().copy(host = "", port = 0).validate()

        assertTrue(errors.any { it.contains("host", ignoreCase = true) })
        assertTrue(errors.any { it.contains("port", ignoreCase = true) })
    }

    @Test
    fun clusterRequiresSeedNode() {
        val profile = sampleStandalone().copy(
            mode = DeploymentMode.Cluster,
            seedNodes = emptyList(),
        )

        assertTrue(profile.validate().any { it.contains("seed", ignoreCase = true) })
    }

    @Test
    fun sentinelAndClusterNodesRequireHostsAndValidPorts() {
        val sentinelErrors = sampleStandalone().copy(
            mode = DeploymentMode.Sentinel,
            sentinelNodes = listOf(HostPort("", 0)),
            masterName = "primary",
        ).validate()
        val clusterErrors = sampleStandalone().copy(
            mode = DeploymentMode.Cluster,
            seedNodes = listOf(HostPort("", 70_000)),
        ).validate()

        assertTrue(sentinelErrors.any { it.contains("sentinel", ignoreCase = true) })
        assertTrue(clusterErrors.any { it.contains("seed", ignoreCase = true) })
    }

    @Test
    fun timeoutsMustBePositive() {
        val profile = sampleStandalone().copy(
            timeouts = TimeoutOptions(connectMs = 0, commandMs = -1, reconnectMs = 0),
        )

        val errors = profile.validate()
        assertTrue(errors.any { it.contains("connect", ignoreCase = true) })
        assertTrue(errors.any { it.contains("command", ignoreCase = true) })
        assertTrue(errors.any { it.contains("reconnect", ignoreCase = true) })
    }

    private fun sampleStandalone() = ConnectionProfile(
        id = "local",
        name = "Local Redis",
        mode = DeploymentMode.Standalone,
        host = "127.0.0.1",
        port = 6379,
    )
}
