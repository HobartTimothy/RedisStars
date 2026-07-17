package org.roberthu.rs.shell.connections

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.roberthu.rs.domain.BinarySafeString
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.CreateRedisKeyRequest
import org.roberthu.rs.domain.HashScanPage
import org.roberthu.rs.domain.KeyMetadata
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.domain.SetScanPage
import org.roberthu.rs.domain.ZSetScanPage
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.port.KeyBrowserPort
import org.roberthu.rs.port.KeyCommandPort
import org.roberthu.rs.port.RedisConnectionPort
import org.roberthu.rs.port.RedisDataPort
import org.roberthu.rs.presentation.ConnectionsViewModel
import org.roberthu.rs.presentation.InMemoryConnectionProfileStore
import org.roberthu.rs.presentation.KeyBrowserViewModel
import org.roberthu.rs.presentation.KeyDetailViewModel
import org.roberthu.rs.theme.RedisTheme
import kotlin.test.Test

/**
 * Regression coverage for "hide the entire Keys browsing pane while Redis is not connected".
 *
 * The workspace must compose [org.roberthu.rs.shell.keys.KeyBrowserScreen] only when the
 * connection is [ConnectionState.Connected]; all other states must remove it from the tree
 * (no reserved width, no placeholder) so [org.roberthu.rs.shell.detail.KeyDetailScreen] reflows.
 */
@OptIn(ExperimentalTestApi::class)
class ConnectionsWorkspaceTest {

    @Test
    fun disconnected_hidesEntireKeyBrowserPane() = runComposeUiTest {
        val harness = WorkspaceHarness(initial = ConnectionState.Disconnected)
        setContent { harness.Content() }
        waitForIdle()

        onNodeWithTag("key_browser_pane").assertDoesNotExist()
        onNodeWithTag("key_pattern").assertDoesNotExist()
        onNodeWithTag("key_list").assertDoesNotExist()
        onNodeWithTag("keys_database_dropdown").assertDoesNotExist()

        // Left connections pane and right detail region stay present.
        onNodeWithTag("connections_empty").assertIsDisplayed()
        onNodeWithTag("key_detail_empty").assertIsDisplayed()
    }

    @Test
    fun connected_showsKeyBrowserPane() = runComposeUiTest {
        val harness = WorkspaceHarness(initial = ConnectionState.Connected("c1", "Local"))
        setContent { harness.Content() }
        waitUntilTagExists("key_browser_pane")

        onNodeWithTag("key_browser_pane").assertIsDisplayed()
        onNodeWithTag("key_pattern").assertIsDisplayed()
        onNodeWithTag("key_list").assertIsDisplayed()
        onNodeWithTag("keys_database_dropdown").assertIsDisplayed()
    }

    @Test
    fun connecting_doesNotShowKeyBrowserPane() = runComposeUiTest {
        val harness = WorkspaceHarness(initial = ConnectionState.Connecting)
        setContent { harness.Content() }
        waitForIdle()
        onNodeWithTag("key_browser_pane").assertDoesNotExist()
    }

    @Test
    fun reconnecting_doesNotShowKeyBrowserPane() = runComposeUiTest {
        val harness = WorkspaceHarness(initial = ConnectionState.Reconnecting(attempt = 1))
        setContent { harness.Content() }
        waitForIdle()
        onNodeWithTag("key_browser_pane").assertDoesNotExist()
    }

    @Test
    fun failed_doesNotShowKeyBrowserPane() = runComposeUiTest {
        val harness = WorkspaceHarness(initial = ConnectionState.Failed(RedisError.Network("offline")))
        setContent { harness.Content() }
        waitForIdle()
        onNodeWithTag("key_browser_pane").assertDoesNotExist()
    }

    @Test
    fun stateTransitions_showAndHidePaneWithoutStaleKeys() = runComposeUiTest {
        val harness = WorkspaceHarness(initial = ConnectionState.Disconnected)
        setContent { harness.Content() }

        // Disconnected: pane absent.
        waitForIdle()
        onNodeWithTag("key_browser_pane").assertDoesNotExist()

        // Connected: pane appears and a fresh scan surfaces the key.
        harness.emit(ConnectionState.Connected("c1", "Local"))
        waitUntilTagExists("key_browser_pane")
        waitUntilTagExists("key_alpha")
        onNodeWithTag("key_browser_pane").assertIsDisplayed()

        // Disconnected again: pane and any previously loaded key disappear entirely.
        harness.emit(ConnectionState.Disconnected)
        waitUntilTagAbsent("key_browser_pane")
        onNodeWithTag("key_alpha").assertDoesNotExist()

        // Reconnected: pane reloads normally without stale state.
        harness.emit(ConnectionState.Connected("c1", "Local"))
        waitUntilTagExists("key_browser_pane")
        waitUntilTagExists("key_alpha")
        onNodeWithTag("key_browser_pane").assertIsDisplayed()
    }

    private fun ComposeUiTest.waitUntilTagExists(tag: String) =
        waitUntil(timeoutMillis = 5_000) { tagExists(tag) }

    private fun ComposeUiTest.waitUntilTagAbsent(tag: String) =
        waitUntil(timeoutMillis = 5_000) { !tagExists(tag) }

    private fun ComposeUiTest.tagExists(tag: String): Boolean =
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
}

/**
 * Builds the three real view models against in-memory fakes so the test drives only
 * [ConnectionState]. An empty profile store keeps the connections pane in its stable
 * empty-state (`connections_empty`) regardless of connection state.
 */
private class WorkspaceHarness(initial: ConnectionState) {
    private val store = InMemoryConnectionProfileStore()
    private val connectionPort = FakeConnectionPort(initial)
    private val browserPort = FakeKeyBrowserPort()
    private val commandPort = FakeKeyCommandPort()
    private val dataPort = FakeRedisDataPort()

    fun emit(state: ConnectionState) = connectionPort.emit(state)

    @androidx.compose.runtime.Composable
    fun Content() {
        val scope = rememberCoroutineScope()
        val connections = remember { ConnectionsViewModel(store, connectionPort, scope) }
        val browser = remember { KeyBrowserViewModel(browserPort, commandPort, scope) }
        val detail = remember { KeyDetailViewModel(commandPort, dataPort, scope) }
        RedisTheme(darkTheme = true) {
            ConnectionsWorkspace(
                connections = connections,
                browser = browser,
                detail = detail,
            )
        }
    }
}

private class FakeConnectionPort(initial: ConnectionState) : RedisConnectionPort {
    private val stateFlow = MutableStateFlow(initial)

    fun emit(state: ConnectionState) {
        stateFlow.value = state
    }

    override suspend fun test(profile: ConnectionProfile): Result<Unit> = Result.success(Unit)

    override suspend fun connect(profile: ConnectionProfile): Result<Unit> = Result.success(Unit)

    override suspend fun disconnect() {
        stateFlow.value = ConnectionState.Disconnected
    }

    override fun connectionState(): StateFlow<ConnectionState> = stateFlow
}

private class FakeKeyBrowserPort : KeyBrowserPort {
    override suspend fun scan(query: ScanQuery): Result<ScanPage> =
        Result.success(
            ScanPage(
                keys = listOf(RedisKeySummary("alpha", RedisKeyType.String)),
                nextCursorToken = null,
            ),
        )

    override fun cancelScan() = Unit

    override suspend fun listDatabases(): Result<List<RedisDatabaseSummary>> =
        Result.success(listOf(RedisDatabaseSummary(0, 1)))

    override suspend fun selectDatabase(index: Int): Result<Unit> = Result.success(Unit)
}

private class FakeKeyCommandPort : KeyCommandPort {
    override suspend fun metadata(key: String): Result<KeyMetadata> =
        Result.success(KeyMetadata(key, RedisKeyType.String, null, null, null))

    override suspend fun rename(from: String, to: String): Result<Unit> = Result.success(Unit)

    override suspend fun delete(keys: List<String>): Result<Long> = Result.success(keys.size.toLong())

    override suspend fun setTtl(key: String, ttlSeconds: Long?): Result<Unit> = Result.success(Unit)

    override suspend fun exists(key: String, database: Int): Result<Boolean> = Result.success(false)

    override suspend fun createKey(request: CreateRedisKeyRequest): Result<Unit> = Result.success(Unit)

    override suspend fun isRedisJsonAvailable(): Result<Boolean> = Result.success(true)
}

private class FakeRedisDataPort : RedisDataPort {
    override suspend fun getString(key: String, maxBytes: Int): Result<BinarySafeString> =
        Result.success(BinarySafeString(utf8 = "", base64 = "", truncated = false))

    override suspend fun setString(key: String, value: String): Result<Unit> = Result.success(Unit)

    override suspend fun hscan(key: String, cursor: String?, count: Int): Result<HashScanPage> =
        Result.success(HashScanPage(emptyList(), null))

    override suspend fun hset(key: String, field: String, value: String): Result<Unit> = Result.success(Unit)

    override suspend fun hdel(key: String, fields: List<String>): Result<Long> = Result.success(0L)

    override suspend fun lrange(key: String, start: Long, stop: Long): Result<List<BinarySafeString>> =
        Result.success(emptyList())

    override suspend fun lpush(key: String, values: List<String>): Result<Long> = Result.success(0L)

    override suspend fun rpush(key: String, values: List<String>): Result<Long> = Result.success(0L)

    override suspend fun lset(key: String, index: Long, value: String): Result<Unit> = Result.success(Unit)

    override suspend fun lrem(key: String, count: Long, value: String): Result<Long> = Result.success(0L)

    override suspend fun sscan(key: String, cursor: String?, count: Int): Result<SetScanPage> =
        Result.success(SetScanPage(emptyList(), null))

    override suspend fun sadd(key: String, members: List<String>): Result<Long> = Result.success(0L)

    override suspend fun srem(key: String, members: List<String>): Result<Long> = Result.success(0L)

    override suspend fun zscan(key: String, cursor: String?, count: Int): Result<ZSetScanPage> =
        Result.success(ZSetScanPage(emptyList(), null))

    override suspend fun zadd(key: String, score: Double, member: String): Result<Long> = Result.success(0L)

    override suspend fun zrem(key: String, members: List<String>): Result<Long> = Result.success(0L)
}
