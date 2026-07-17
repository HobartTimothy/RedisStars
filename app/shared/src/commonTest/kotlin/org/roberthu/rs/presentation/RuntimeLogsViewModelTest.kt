package org.roberthu.rs.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.ApplicationLogEntry
import org.roberthu.rs.domain.ApplicationLogLevel
import org.roberthu.rs.port.ApplicationLogPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RuntimeLogsViewModelTest {
    @Test
    fun levelFilter_andSearch_filterVisibleEntries() = runTest {
        val port = InMemoryApplicationLogPort(
            initialEntries = listOf(
                entry("1", ApplicationLogLevel.INFO, "boot", "started"),
                entry("2", ApplicationLogLevel.ERROR, "redis", "connection failed"),
            ),
        )
        val viewModel = RuntimeLogsViewModel(port, this)
        advanceUntilIdle()

        viewModel.setLevelFilter(ApplicationLogLevel.ERROR)
        assertEquals(1, viewModel.state.value.filteredEntries.size)
        assertEquals("connection failed", viewModel.state.value.filteredEntries.single().message)

        viewModel.setLevelFilter(null)
        viewModel.setSearchQuery("boot")
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.filteredEntries.size)
        assertEquals("started", viewModel.state.value.filteredEntries.single().message)
        viewModel.dispose()
    }

    @Test
    fun pause_buffersIncomingEntries_untilResumed() = runTest {
        val port = InMemoryApplicationLogPort()
        val viewModel = RuntimeLogsViewModel(port, this)
        advanceUntilIdle()

        viewModel.setPaused(true)
        port.emit(entry("live-1", ApplicationLogLevel.INFO, "app", "while paused"))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.entries.isEmpty())

        viewModel.setPaused(false)
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.entries.size)
        assertEquals("while paused", viewModel.state.value.entries.single().message)
        viewModel.dispose()
    }

    @Test
    fun clearDisplay_onlyClearsVisibleEntries() = runTest {
        val port = InMemoryApplicationLogPort(
            initialEntries = listOf(entry("1", ApplicationLogLevel.INFO, "app", "old")),
        )
        val viewModel = RuntimeLogsViewModel(port, this)
        advanceUntilIdle()

        viewModel.clearDisplay()
        assertTrue(viewModel.state.value.entries.isEmpty())

        port.emit(entry("2", ApplicationLogLevel.WARN, "app", "new"))
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.entries.size)
        viewModel.dispose()
    }

    @Test
    fun loadFailure_setsErrorWithoutCrashing() = runTest {
        val port = FailingApplicationLogPort()
        val viewModel = RuntimeLogsViewModel(port, this)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.loading)
        assertEquals("load failed", viewModel.state.value.error)
        viewModel.dispose()
    }

    @Test
    fun exportUsesFilteredEntries() = runTest {
        val port = RecordingExportApplicationLogPort()
        val viewModel = RuntimeLogsViewModel(port, this)
        advanceUntilIdle()
        viewModel.setLevelFilter(ApplicationLogLevel.WARN)

        viewModel.exportFiltered("/tmp/export.log")
        advanceUntilIdle()

        assertEquals(0, port.exportedCount)
        port.emit(entry("1", ApplicationLogLevel.WARN, "app", "warn message"))
        port.emit(entry("2", ApplicationLogLevel.INFO, "app", "info message"))
        advanceUntilIdle()

        viewModel.exportFiltered("/tmp/export.log")
        advanceUntilIdle()

        assertEquals(1, port.lastExported?.size)
        assertEquals(ApplicationLogLevel.WARN, port.lastExported?.single()?.level)
        viewModel.dispose()
    }

    @Test
    fun appendStress_keepsOnlyMostRecentMaxEntries() = runTest {
        val port = HighCapacityApplicationLogPort()
        val maxEntries = 10_000
        val viewModel = RuntimeLogsViewModel(port, this, maxEntries = maxEntries)
        advanceUntilIdle()

        repeat(maxEntries + 500) { index ->
            port.emit(entry("entry-$index", ApplicationLogLevel.INFO, "app", "message-$index"))
        }
        advanceUntilIdle()

        assertEquals(maxEntries, viewModel.state.value.entries.size)
        assertEquals("message-${maxEntries + 499}", viewModel.state.value.entries.last().message)
        assertEquals("message-500", viewModel.state.value.entries.first().message)
        viewModel.dispose()
    }

    @Test
    fun pausedQueue_isCappedAtMaxEntries() = runTest {
        val port = HighCapacityApplicationLogPort()
        val maxEntries = 100
        val viewModel = RuntimeLogsViewModel(port, this, maxEntries = maxEntries)
        advanceUntilIdle()

        viewModel.setPaused(true)
        repeat(maxEntries + 50) { index ->
            port.emit(entry("paused-$index", ApplicationLogLevel.INFO, "app", "paused-message-$index"))
        }
        advanceUntilIdle()
        assertTrue(viewModel.state.value.entries.isEmpty())

        viewModel.setPaused(false)
        advanceUntilIdle()

        assertEquals(maxEntries, viewModel.state.value.entries.size)
        assertEquals("paused-message-${maxEntries + 49}", viewModel.state.value.entries.last().message)
        assertEquals("paused-message-50", viewModel.state.value.entries.first().message)
        viewModel.dispose()
    }

    @Test
    fun searchQuery_isDebouncedBeforeFiltering() = runTest {
        val port = InMemoryApplicationLogPort(
            initialEntries = listOf(
                entry("1", ApplicationLogLevel.INFO, "boot", "started"),
                entry("2", ApplicationLogLevel.ERROR, "redis", "connection failed"),
            ),
        )
        val viewModel = RuntimeLogsViewModel(port, this)
        advanceUntilIdle()

        viewModel.setSearchQuery("boot")
        assertEquals(2, viewModel.state.value.filteredEntries.size)

        advanceTimeBy(200)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.filteredEntries.size)
        assertEquals("started", viewModel.state.value.filteredEntries.single().message)
        viewModel.dispose()
    }

    @Test
    fun filterRuntimeLogEntries_matchesViewModelFiltering() = runTest {
        val entries = listOf(
            entry("1", ApplicationLogLevel.INFO, "boot", "started"),
            entry("2", ApplicationLogLevel.ERROR, "redis", "connection failed"),
        )
        val expected = filterRuntimeLogEntries(entries, ApplicationLogLevel.ERROR, "conn")

        val port = InMemoryApplicationLogPort(initialEntries = entries)
        val viewModel = RuntimeLogsViewModel(port, this)
        advanceUntilIdle()

        viewModel.setLevelFilter(ApplicationLogLevel.ERROR)
        viewModel.setSearchQuery("conn")
        advanceUntilIdle()

        assertEquals(expected, viewModel.state.value.filteredEntries)
        viewModel.dispose()
    }

    private fun entry(
        id: String,
        level: ApplicationLogLevel,
        target: String,
        message: String,
    ) = ApplicationLogEntry(
        id = id,
        timestamp = "2026-07-16 16:00:00.000",
        level = level,
        target = target,
        message = message,
    )
}

private class FailingApplicationLogPort : ApplicationLogPort {
    override suspend fun loadRecent(maxEntries: Int) = Result.failure<List<ApplicationLogEntry>>(
        IllegalStateException("load failed"),
    )

    override fun watch(): Flow<ApplicationLogEntry> = emptyFlow()

    override suspend fun export(entries: List<ApplicationLogEntry>, targetPath: String) = Result.success(Unit)
}

private class RecordingExportApplicationLogPort : InMemoryApplicationLogPort() {
    var exportedCount = 0
    var lastExported: List<ApplicationLogEntry>? = null

    override suspend fun export(entries: List<ApplicationLogEntry>, targetPath: String): Result<Unit> {
        exportedCount += 1
        lastExported = entries
        return Result.success(Unit)
    }
}

private class HighCapacityApplicationLogPort : ApplicationLogPort {
    private val entries = mutableListOf<ApplicationLogEntry>()
    private val events = Channel<ApplicationLogEntry>(Channel.UNLIMITED)

    fun emit(entry: ApplicationLogEntry) {
        entries.add(entry)
        events.trySend(entry)
    }

    override suspend fun loadRecent(maxEntries: Int): Result<List<ApplicationLogEntry>> =
        Result.success(entries.takeLast(maxEntries))

    override fun watch(): Flow<ApplicationLogEntry> = events.receiveAsFlow()

    override suspend fun export(entries: List<ApplicationLogEntry>, targetPath: String): Result<Unit> =
        Result.success(Unit)
}
