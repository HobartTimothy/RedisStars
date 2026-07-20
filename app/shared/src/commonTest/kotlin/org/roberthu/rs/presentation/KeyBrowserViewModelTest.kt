package org.roberthu.rs.presentation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.ConnectionBrowserOptions
import org.roberthu.rs.domain.CreateRedisKeyRequest
import org.roberthu.rs.domain.DatabaseFilterMode
import org.roberthu.rs.domain.KeyListViewMode
import org.roberthu.rs.domain.KeyMetadata
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.port.KeyBrowserPort
import org.roberthu.rs.port.KeyCommandPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KeyBrowserViewModelTest {
    @Test
    fun cancel_stopsActiveScanAndClearsLoading() = runTest {
        val port = RecordingKeyBrowserPort(blockScan = true)
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)

        viewModel.refresh()
        assertTrue(viewModel.state.value.loading)

        viewModel.cancel()
        advanceUntilIdle()

        assertTrue(port.cancelled)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun refresh_afterFailure_recoversAndShowsKeys() = runTest {
        val port = RecordingKeyBrowserPort(
            results = ArrayDeque(
                listOf(
                    Result.failure(RedisError.Network("offline")),
                    Result.success(
                        ScanPage(
                            keys = listOf(RedisKeySummary("users:1", RedisKeyType.Hash)),
                            nextCursorToken = null,
                        ),
                    ),
                ),
            ),
        )
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)

        viewModel.refresh()
        advanceUntilIdle()
        assertEquals("offline", viewModel.state.value.error)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.error)
        assertEquals(listOf("users:1"), viewModel.state.value.keys.map { it.key })
    }

    @Test
    fun loadMore_appendsAndDeduplicatesKeys() = runTest {
        val port = RecordingKeyBrowserPort(
            results = ArrayDeque(
                listOf(
                    Result.success(
                        ScanPage(
                            keys = listOf(RedisKeySummary("a", RedisKeyType.String)),
                            nextCursorToken = "1",
                        ),
                    ),
                    Result.success(
                        ScanPage(
                            keys = listOf(
                                RedisKeySummary("a", RedisKeyType.String),
                                RedisKeySummary("b", RedisKeyType.Set),
                            ),
                            nextCursorToken = null,
                        ),
                    ),
                ),
            ),
        )
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), viewModel.state.value.keys.map { it.key })
        assertEquals(listOf(null, "1"), port.queries.map { it.cursorToken })
    }

    @Test
    fun selectDatabase_clearsSelectedKey() = runTest {
        val port = RecordingKeyBrowserPort(
            results = ArrayDeque(
                listOf(
                    Result.success(ScanPage(emptyList(), null)),
                    Result.success(ScanPage(emptyList(), null)),
                ),
            ),
        )
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)
        viewModel.onConnected(clusterMode = false, initialDatabase = 0)
        advanceUntilIdle()

        viewModel.select(RedisKeySummary("foo", RedisKeyType.String))
        viewModel.selectDatabase(1)
        advanceUntilIdle()

        assertNull(viewModel.state.value.selectedKey)
        assertEquals(1, viewModel.state.value.selectedDatabase)
    }

    @Test
    fun submitNewKey_successClosesDialogAndRefreshes() = runTest {
        val port = RecordingKeyBrowserPort(
            results = ArrayDeque(
                listOf(
                    Result.success(ScanPage(emptyList(), null)),
                    Result.success(
                        ScanPage(
                            keys = listOf(RedisKeySummary("new:key", RedisKeyType.String)),
                            nextCursorToken = null,
                        ),
                    ),
                ),
            ),
        )
        val commands = FakeKeyCommandPort()
        val viewModel = KeyBrowserViewModel(port, commands, this)
        viewModel.onConnected(clusterMode = false, initialDatabase = 0)
        advanceUntilIdle()

        viewModel.openAddKeyDialog()
        advanceUntilIdle()
        viewModel.updateAddKeyKey("new:key")
        viewModel.updateAddKeyStringValue("hello")
        viewModel.submitNewKey()
        advanceUntilIdle()

        assertNull(viewModel.state.value.addKeyDialog)
        assertEquals("new:key", viewModel.state.value.lastCreatedKey?.key)
        assertEquals(1, commands.createCalls.size)
    }

    @Test
    fun staleScanResult_isIgnored() = runTest {
        val blocker = CompletableDeferred<Unit>()
        val port = SlowKeyBrowserPort(blocker)
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)

        viewModel.setPattern("slow:*")
        viewModel.refresh()
        viewModel.setPattern("fast:*")
        viewModel.refresh()
        blocker.complete(Unit)
        advanceUntilIdle()

        assertEquals("fast:*", port.queries.last().pattern)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun onConnected_appliesBrowserOptions() = runTest {
        val port = RecordingKeyBrowserPort()
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)
        val options = ConnectionBrowserOptions(
            keyPattern = "session:*",
            keySeparator = "/",
            keyListView = KeyListViewMode.Flat,
            keyLoadBatchSize = 500,
            databaseFilterMode = DatabaseFilterMode.ShowSpecified,
            databaseFilterValues = listOf(0, 1),
        )

        viewModel.onConnected(clusterMode = false, initialDatabase = 1, browserOptions = options)
        advanceUntilIdle()

        assertEquals("session:*", viewModel.state.value.pattern)
        assertEquals(KeyListViewMode.Flat, viewModel.state.value.keyListView)
        assertEquals("/", viewModel.state.value.keySeparator)
        assertEquals(listOf(0, 1), viewModel.state.value.databases.map { it.index })
        assertEquals(1, viewModel.state.value.selectedDatabase)
        assertEquals(500, port.queries.last().countHint)
    }

    @Test
    fun selectDatabase_ignoresFilteredDatabase() = runTest {
        val port = RecordingKeyBrowserPort(
            results = ArrayDeque(
                listOf(
                    Result.success(ScanPage(emptyList(), null)),
                    Result.success(ScanPage(emptyList(), null)),
                ),
            ),
        )
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)
        viewModel.onConnected(
            clusterMode = false,
            initialDatabase = 0,
            browserOptions = ConnectionBrowserOptions(
                databaseFilterMode = DatabaseFilterMode.HideSpecified,
                databaseFilterValues = listOf(1),
            ),
        )
        advanceUntilIdle()

        viewModel.selectDatabase(1)
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.selectedDatabase)
        assertEquals(1, port.selectDatabaseCalls.size)
    }

    @Test
    fun filterDatabases_showSpecified_keepsOnlyListed() = runTest {
        val port = RecordingKeyBrowserPort()
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)

        viewModel.onConnected(
            clusterMode = false,
            browserOptions = ConnectionBrowserOptions(
                databaseFilterMode = DatabaseFilterMode.ShowSpecified,
                databaseFilterValues = listOf(1),
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf(1), viewModel.state.value.databases.map { it.index })
    }

    @Test
    fun filterDatabases_hideSpecified_removesListed() = runTest {
        val port = RecordingKeyBrowserPort()
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)

        viewModel.onConnected(
            clusterMode = false,
            browserOptions = ConnectionBrowserOptions(
                databaseFilterMode = DatabaseFilterMode.HideSpecified,
                databaseFilterValues = listOf(1),
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf(0), viewModel.state.value.databases.map { it.index })
    }

    @Test
    fun setTypeFilter_updatesScanQueryAndRefreshes() = runTest {
        val port = RecordingKeyBrowserPort(
            results = ArrayDeque(
                listOf(
                    Result.success(ScanPage(emptyList(), null)),
                    Result.success(
                        ScanPage(
                            keys = listOf(RedisKeySummary("users:1", RedisKeyType.Hash)),
                            nextCursorToken = null,
                        ),
                    ),
                ),
            ),
        )
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)

        viewModel.refresh()
        advanceUntilIdle()
        assertNull(port.queries.last().type)

        viewModel.setTypeFilter(RedisKeyType.Hash)
        advanceUntilIdle()

        assertEquals(RedisKeyType.Hash, viewModel.state.value.typeFilter)
        assertEquals(RedisKeyType.Hash, port.queries.last().type)
        assertEquals(listOf("users:1"), viewModel.state.value.keys.map { it.key })
    }

    @Test
    fun loadMore_preservesTypeFilter() = runTest {
        val port = RecordingKeyBrowserPort(
            results = ArrayDeque(
                listOf(
                    Result.success(
                        ScanPage(
                            keys = listOf(RedisKeySummary("a", RedisKeyType.Hash)),
                            nextCursorToken = "1",
                        ),
                    ),
                    Result.success(
                        ScanPage(
                            keys = listOf(RedisKeySummary("b", RedisKeyType.Hash)),
                            nextCursorToken = null,
                        ),
                    ),
                ),
            ),
        )
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)

        viewModel.setTypeFilter(RedisKeyType.Hash)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(RedisKeyType.Hash, RedisKeyType.Hash), port.queries.map { it.type })
    }

    @Test
    fun onDisconnected_resetsTypeFilter() = runTest {
        val port = RecordingKeyBrowserPort()
        val viewModel = KeyBrowserViewModel(port, FakeKeyCommandPort(), this)

        viewModel.setTypeFilter(RedisKeyType.Set)
        advanceUntilIdle()
        viewModel.onDisconnected()

        assertNull(viewModel.state.value.typeFilter)
    }
}

private class RecordingKeyBrowserPort(
    private val results: ArrayDeque<Result<ScanPage>> = ArrayDeque(
        listOf(Result.success(ScanPage(emptyList(), null))),
    ),
    private val blockScan: Boolean = false,
) : KeyBrowserPort {
    val queries = mutableListOf<ScanQuery>()
    val selectDatabaseCalls = mutableListOf<Int>()
    var cancelled = false
    private val blocker = CompletableDeferred<Unit>()

    override suspend fun scan(query: ScanQuery): Result<ScanPage> {
        queries += query
        if (blockScan) blocker.await()
        return if (results.isNotEmpty()) results.removeFirst() else Result.success(ScanPage(emptyList(), null))
    }

    override fun cancelScan() {
        cancelled = true
        blocker.complete(Unit)
    }

    override suspend fun listDatabases() =
        Result.success(
            listOf(
                RedisDatabaseSummary(0, 5),
                RedisDatabaseSummary(1, 12),
            ),
        )

    override suspend fun selectDatabase(index: Int): Result<Unit> {
        selectDatabaseCalls += index
        return Result.success(Unit)
    }
}

private class SlowKeyBrowserPort(
    private val blocker: CompletableDeferred<Unit>,
) : KeyBrowserPort {
    val queries = mutableListOf<ScanQuery>()

    override suspend fun scan(query: ScanQuery): Result<ScanPage> {
        queries += query
        blocker.await()
        return Result.success(ScanPage(listOf(RedisKeySummary("stale", RedisKeyType.String)), null))
    }

    override fun cancelScan() = Unit

    override suspend fun listDatabases() =
        Result.success(listOf(RedisDatabaseSummary(0, 0)))

    override suspend fun selectDatabase(index: Int) = Result.success(Unit)
}

private class FakeKeyCommandPort : KeyCommandPort {
    val createCalls = mutableListOf<CreateRedisKeyRequest>()

    override suspend fun metadata(key: String) =
        Result.success(KeyMetadata(key, RedisKeyType.String, null, null, null))

    override suspend fun rename(from: String, to: String) = Result.success(Unit)

    override suspend fun delete(keys: List<String>) = Result.success(keys.size.toLong())

    override suspend fun setTtl(key: String, ttlSeconds: Long?) = Result.success(Unit)

    override suspend fun exists(key: String, database: Int) = Result.success(false)

    override suspend fun createKey(request: CreateRedisKeyRequest): Result<Unit> {
        createCalls += request
        return Result.success(Unit)
    }

    override suspend fun isRedisJsonAvailable() = Result.success(true)
}
