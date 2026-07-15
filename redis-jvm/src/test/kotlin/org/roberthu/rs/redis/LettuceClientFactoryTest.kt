package org.roberthu.rs.redis

import org.junit.Test
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.HostPort
import org.roberthu.rs.domain.RedisError
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class LettuceClientFactoryTest {
    private val factory = LettuceClientFactory()

    @Test
    fun createsStandaloneSentinelAndClusterClientHandles() {
        factory.create(profile(DeploymentMode.Standalone)).use {
            assertIs<LettuceClientHandle.Standard>(it)
            assertEquals(DeploymentMode.Standalone, it.mode)
        }
        factory.create(
            profile(DeploymentMode.Sentinel).copy(
                sentinelNodes = listOf(HostPort("sentinel-a", 26379), HostPort("sentinel-b", 26380)),
                masterName = "primary",
            ),
        ).use {
            assertIs<LettuceClientHandle.Standard>(it)
            assertEquals(DeploymentMode.Sentinel, it.mode)
        }
        factory.create(
            profile(DeploymentMode.Cluster).copy(
                seedNodes = listOf(HostPort("cluster-a", 7000), HostPort("cluster-b", 7001)),
            ),
        ).use {
            assertIs<LettuceClientHandle.Cluster>(it)
            assertEquals(DeploymentMode.Cluster, it.mode)
        }
    }

    @Test
    fun rejectsInvalidProfilesBeforeAllocatingClient() {
        val error = assertFailsWith<RedisError.Validation> {
            factory.create(
                profile(DeploymentMode.Cluster).copy(
                    seedNodes = emptyList(),
                    database = 2,
                ),
            )
        }

        assertEquals("VALIDATION", error.code)
    }

    private fun profile(mode: DeploymentMode) = ConnectionProfile(
        id = "test",
        name = "Test",
        mode = mode,
        host = "127.0.0.1",
    )
}
