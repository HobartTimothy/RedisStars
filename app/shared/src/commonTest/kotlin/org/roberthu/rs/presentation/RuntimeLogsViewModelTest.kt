package org.roberthu.rs.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
