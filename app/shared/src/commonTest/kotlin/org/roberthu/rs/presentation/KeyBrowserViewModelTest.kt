package org.roberthu.rs.presentation

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.port.KeyBrowserPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class KeyBrowserViewModelTest {
    @Test
    fun cancel_stopsActiveScanAndClearsLoading() = runTest {
        val port = RecordingKeyBrowserPort(blockScan = true)
        val viewModel = KeyBrowserViewModel(port, this)

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
        val viewModel = KeyBrowserViewModel(port, this)

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
        val viewModel = KeyBrowserViewModel(port, this)

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), viewModel.state.value.keys.map { it.key })
        assertEquals(listOf(null, "1"), port.queries.map { it.cursorToken })
    }
}

private class RecordingKeyBrowserPort(
    private val results: ArrayDeque<Result<ScanPage>> = ArrayDeque(),
    private val blockScan: Boolean = false,
) : KeyBrowserPort {
    val queries = mutableListOf<ScanQuery>()
    var cancelled = false
    private val blocker = CompletableDeferred<Unit>()

    override suspend fun scan(query: ScanQuery): Result<ScanPage> {
        queries += query
        if (blockScan) blocker.await()
        return results.removeFirst()
    }

    override fun cancelScan() {
        cancelled = true
        blocker.complete(Unit)
    }
}
