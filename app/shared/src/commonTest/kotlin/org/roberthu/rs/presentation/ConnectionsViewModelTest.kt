package org.roberthu.rs.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.HostPort
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.port.RedisConnectionPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionsViewModelTest {
    @Test
    fun beginCreate_opensEditorWithDefaults() = vmTest { viewModel, _, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.state.value.editor)
        assertEquals(ConnectionEditorMode.Create, editor.mode)
        assertEquals("New connection", editor.form.name)
        assertEquals(DeploymentMode.Standalone, editor.form.deploymentMode)
        assertEquals("localhost", editor.form.host)
        assertEquals("6379", editor.form.port)
        assertEquals("0", editor.form.database)
        assertEquals("5000", editor.form.connectTimeoutMs)
        assertEquals("5000", editor.form.commandTimeoutMs)
        assertEquals("30000", editor.form.reconnectTimeoutMs)
        assertFalse(editor.form.tlsEnabled)
        assertTrue(editor.form.verifyPeer)
        assertFalse(editor.isDirty)
    }

    @Test
    fun edit_fillsFormFromProfile() = vmTest(
        store = FakeConnectionProfileStore(mutableListOf(standaloneProfile("c1", "Existing"))),
    ) { viewModel, store, _ ->
        val profile = store.profiles.single()
        viewModel.edit(profile)
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.state.value.editor)
        assertEquals(ConnectionEditorMode.Edit, editor.mode)
        assertEquals("c1", editor.profileId)
        assertEquals("Existing", editor.form.name)
        assertEquals(profile.host, editor.form.host)
        assertFalse(editor.isDirty)
    }

    @Test
    fun updateEditorForm_marksDirty() = vmTest { viewModel, _, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()

        viewModel.updateEditorForm { it.copy(name = "Changed") }
        advanceUntilIdle()

        assertTrue(assertNotNull(viewModel.state.value.editor).isDirty)
    }

    @Test
    fun requestCloseEditor_closesImmediatelyWhenClean() = vmTest { viewModel, _, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()

        viewModel.requestCloseEditor()
        advanceUntilIdle()

        assertNull(viewModel.state.value.editor)
    }

    @Test
    fun requestCloseEditor_showsDiscardConfirmationWhenDirty() = vmTest { viewModel, _, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()
        viewModel.updateEditorForm { it.copy(name = "Dirty") }

        viewModel.requestCloseEditor()
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.state.value.editor)
        assertTrue(editor.confirmDiscardVisible)
    }

    @Test
    fun testEditor_successSetsFlag() = vmTest { viewModel, _, port ->
        viewModel.beginCreate()
        advanceUntilIdle()

        viewModel.testEditor()
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.state.value.editor)
        assertTrue(editor.testSucceeded)
        assertFalse(editor.testing)
        assertEquals(1, port.testCalls.size)
    }

    @Test
    fun testEditor_failureKeepsInputAndShowsError() = vmTest(
        port = FakeRedisConnectionPort(testResult = Result.failure(RedisError.Network("offline"))),
    ) { viewModel, _, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()
        viewModel.updateEditorForm { it.copy(name = "Keep me") }

        viewModel.testEditor()
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.state.value.editor)
        assertEquals("Keep me", editor.form.name)
        assertFalse(editor.testSucceeded)
        assertEquals("offline", editor.globalError)
    }

    @Test
    fun testEditor_doesNotSaveProfile() = vmTest { viewModel, store, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()

        viewModel.testEditor()
        advanceUntilIdle()

        assertTrue(store.profiles.isEmpty())
        assertEquals(0, store.upsertCount)
    }

    @Test
    fun saveEditor_successClosesDialog() = vmTest { viewModel, store, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()
        viewModel.updateEditorForm { it.copy(name = "Saved") }

        viewModel.saveEditor()
        advanceUntilIdle()

        assertNull(viewModel.state.value.editor)
        assertEquals(1, store.profiles.size)
        assertEquals("Saved", store.profiles.first().name)
        assertEquals(store.profiles.first().id, viewModel.state.value.selectedProfileId)
    }

    @Test
    fun saveEditor_failureKeepsDialogAndInput() = vmTest(
        store = FakeConnectionProfileStore(failUpsert = true),
    ) { viewModel, store, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()
        viewModel.updateEditorForm { it.copy(name = "Keep open") }

        viewModel.saveEditor()
        advanceUntilIdle()

        val editor = assertNotNull(viewModel.state.value.editor)
        assertEquals("Keep open", editor.form.name)
        assertNotNull(editor.globalError)
        assertTrue(store.profiles.isEmpty())
    }

    @Test
    fun saveEditor_sentinelPersistsNodes() = vmTest { viewModel, store, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()
        viewModel.updateEditorForm {
            it.copy(
                name = "Sentinel",
                deploymentMode = DeploymentMode.Sentinel,
                masterName = "mymaster",
                sentinelNodes = listOf(HostPortFormState("n1", "s1", "26379")),
            )
        }

        viewModel.saveEditor()
        advanceUntilIdle()

        val saved = store.profiles.single()
        assertEquals(DeploymentMode.Sentinel, saved.mode)
        assertEquals("mymaster", saved.masterName)
        assertEquals(listOf(HostPort("s1", 26379)), saved.sentinelNodes)
    }

    @Test
    fun saveEditor_clusterPersistsSeedsAndDatabaseZero() = vmTest { viewModel, store, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()
        viewModel.updateEditorForm {
            it.copy(
                name = "Cluster",
                deploymentMode = DeploymentMode.Cluster,
                database = "5",
                seedNodes = listOf(HostPortFormState("n1", "c1", "7000")),
            )
        }

        viewModel.saveEditor()
        advanceUntilIdle()

        val saved = store.profiles.single()
        assertEquals(DeploymentMode.Cluster, saved.mode)
        assertEquals(0, saved.database)
        assertEquals(listOf(HostPort("c1", 7000)), saved.seedNodes)
    }

    @Test
    fun saveEditor_editKeepsExistingId() = vmTest(
        store = FakeConnectionProfileStore(mutableListOf(standaloneProfile("keep-id", "Old"))),
    ) { viewModel, store, _ ->
        val profile = store.profiles.single()
        viewModel.edit(profile)
        advanceUntilIdle()
        viewModel.updateEditorForm { it.copy(name = "New name") }

        viewModel.saveEditor()
        advanceUntilIdle()

        assertEquals(listOf("keep-id"), store.profiles.map { it.id })
        assertEquals("New name", store.profiles.single().name)
    }

    @Test
    fun confirmDiscardEditor_closesDialog() = vmTest { viewModel, _, _ ->
        viewModel.beginCreate()
        advanceUntilIdle()
        viewModel.updateEditorForm { it.copy(name = "Dirty") }
        viewModel.requestCloseEditor()

        viewModel.confirmDiscardEditor()
        advanceUntilIdle()

        assertNull(viewModel.state.value.editor)
    }
}

private fun standaloneProfile(id: String, name: String): ConnectionProfile = ConnectionProfile(
    id = id,
    name = name,
    mode = DeploymentMode.Standalone,
    host = "localhost",
    port = 6379,
    database = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun vmTest(
    store: FakeConnectionProfileStore = FakeConnectionProfileStore(),
    port: FakeRedisConnectionPort = FakeRedisConnectionPort(),
    block: suspend TestScope.(
        ConnectionsViewModel,
        FakeConnectionProfileStore,
        FakeRedisConnectionPort,
    ) -> Unit,
) = runTest {
    val job = Job(coroutineContext[Job])
    val viewModel = ConnectionsViewModel(store, port, CoroutineScope(coroutineContext + job))
    try {
        advanceUntilIdle()
        block(viewModel, store, port)
    } finally {
        job.cancel()
    }
}

private class FakeConnectionProfileStore(
    val profiles: MutableList<ConnectionProfile> = mutableListOf(),
    private val failUpsert: Boolean = false,
) : ConnectionProfileStore {
    var upsertCount: Int = 0
        private set

    override suspend fun list(): List<ConnectionProfile> = profiles.toList()

    override suspend fun upsert(profile: ConnectionProfile) {
        upsertCount++
        if (failUpsert) throw RedisError.Unknown("store write failed")
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            profiles[index] = profile
        } else {
            profiles += profile
        }
    }

    override suspend fun delete(id: String) {
        profiles.removeAll { it.id == id }
    }
}

private class FakeRedisConnectionPort(
    private val testResult: Result<Unit> = Result.success(Unit),
) : RedisConnectionPort {
    val testCalls = mutableListOf<ConnectionProfile>()
    private val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    override suspend fun test(profile: ConnectionProfile): Result<Unit> {
        testCalls += profile
        return testResult
    }

    override suspend fun connect(profile: ConnectionProfile): Result<Unit> =
        Result.success(Unit).also {
            state.value = ConnectionState.Connected(profile.id, profile.name)
        }

    override suspend fun disconnect() {
        state.value = ConnectionState.Disconnected
    }

    override fun connectionState(): StateFlow<ConnectionState> = state
}
