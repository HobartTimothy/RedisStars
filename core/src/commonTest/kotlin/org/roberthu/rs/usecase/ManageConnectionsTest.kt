package org.roberthu.rs.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.HostPort
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.port.RedisConnectionPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ManageConnectionsTest {
    @Test
    fun createsCopiesAndDeletesProfiles() = runTest {
        val store = InMemoryProfileStore()
        val useCase = ManageConnections(store, RecordingConnectionPort())
        val original = standaloneProfile()

        assertTrue(useCase.create(original).isSuccess)
        val copied = useCase.copy(
            sourceId = original.id,
            newId = "local-copy",
            newName = "Local Redis Copy",
        ).getOrThrow()
        useCase.delete(original.id)

        assertEquals("local-copy", copied.id)
        assertEquals("Local Redis Copy", copied.name)
        assertEquals(listOf(copied), useCase.list())
    }

    @Test
    fun rejectsInvalidClusterBeforeTestingConnection() = runTest {
        val port = RecordingConnectionPort()
        val useCase = ManageConnections(InMemoryProfileStore(), port)
        val invalid = clusterProfile(database = 2)

        val result = useCase.test(invalid)

        assertTrue(result.isFailure)
        assertEquals(0, port.testCalls.size)
    }

    @Test
    fun rejectsInvalidClusterBeforeConnecting() = runTest {
        val port = RecordingConnectionPort()
        val useCase = ManageConnections(InMemoryProfileStore(), port)
        val invalid = clusterProfile(database = 2)

        val result = useCase.connect(invalid)

        assertTrue(result.isFailure)
        assertEquals(0, port.connectCalls.size)
    }

    @Test
    fun delegatesTestConnectDisconnectAndConnectionState() = runTest {
        val port = RecordingConnectionPort()
        val useCase = ManageConnections(InMemoryProfileStore(), port)
        val profile = standaloneProfile()

        assertTrue(useCase.test(profile).isSuccess)
        assertTrue(useCase.connect(profile).isSuccess)
        useCase.disconnect()

        assertEquals(listOf(profile), port.testCalls)
        assertEquals(listOf(profile), port.connectCalls)
        assertEquals(1, port.disconnectCalls)
        assertIs<ConnectionState.Disconnected>(useCase.connectionState().value)
    }

    private class InMemoryProfileStore : ConnectionProfileStore {
        private val profiles = linkedMapOf<String, ConnectionProfile>()

        override suspend fun list(): List<ConnectionProfile> = profiles.values.toList()

        override suspend fun upsert(profile: ConnectionProfile) {
            profiles[profile.id] = profile
        }

        override suspend fun delete(id: String) {
            profiles.remove(id)
        }
    }

    private class RecordingConnectionPort : RedisConnectionPort {
        val testCalls = mutableListOf<ConnectionProfile>()
        val connectCalls = mutableListOf<ConnectionProfile>()
        var disconnectCalls = 0
        private val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

        override suspend fun test(profile: ConnectionProfile): Result<Unit> {
            testCalls += profile
            return Result.success(Unit)
        }

        override suspend fun connect(profile: ConnectionProfile): Result<Unit> {
            connectCalls += profile
            return Result.success(Unit)
        }

        override suspend fun disconnect() {
            disconnectCalls += 1
        }

        override fun connectionState(): StateFlow<ConnectionState> = state
    }

    private fun standaloneProfile() = ConnectionProfile(
        id = "local",
        name = "Local Redis",
        mode = DeploymentMode.Standalone,
        host = "127.0.0.1",
    )

    private fun clusterProfile(database: Int) = ConnectionProfile(
        id = "cluster",
        name = "Redis Cluster",
        mode = DeploymentMode.Cluster,
        seedNodes = listOf(HostPort("127.0.0.1", 7000)),
        database = database,
    )
}
