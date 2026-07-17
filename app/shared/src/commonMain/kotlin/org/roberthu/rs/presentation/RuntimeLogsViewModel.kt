package org.roberthu.rs.presentation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.roberthu.rs.domain.ApplicationLogEntry
import org.roberthu.rs.domain.ApplicationLogLevel
import org.roberthu.rs.i18n.AppI18n
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.port.ApplicationLogPort
import org.roberthu.rs.util.SensitiveRedactor

class RuntimeLogsViewModel(
    private val logPort: ApplicationLogPort,
    private val scope: CoroutineScope,
    private val maxEntries: Int = 10_000,
) {
    private val mutableState = MutableStateFlow(RuntimeLogsUiState())
    val state: StateFlow<RuntimeLogsUiState> = mutableState.asStateFlow()

    private val entryBuffer = ArrayDeque<ApplicationLogEntry>()
    private val pendingWhilePaused = ArrayDeque<ApplicationLogEntry>()
    private var effectiveSearchQuery = ""
    private var watchJob: Job? = null
    private var batchFlushJob: Job? = null
    private var searchDebounceJob: Job? = null

    init {
        refresh()
        startWatching()
    }

    fun setLevelFilter(level: ApplicationLogLevel?) {
        mutableState.update { current ->
            current.copy(
                levelFilter = level,
                filteredEntries = filterRuntimeLogEntries(entryBuffer, level, effectiveSearchQuery),
            )
        }
    }

    fun setSearchQuery(query: String) {
        mutableState.update { it.copy(searchQuery = query) }
        scheduleSearchFilterUpdate()
    }

    fun setAutoScroll(enabled: Boolean) {
        mutableState.update {
            it.copy(
                autoScroll = enabled,
                userPinnedScroll = if (enabled) false else it.userPinnedScroll,
            )
        }
    }

    fun setPaused(paused: Boolean) {
        mutableState.update { current ->
            if (!paused && current.paused) {
                pendingWhilePaused.forEach { appendToBuffer(entryBuffer, it) }
                pendingWhilePaused.clear()
                current.copy(
                    entries = entryBuffer.toList(),
                    filteredEntries = filterRuntimeLogEntries(
                        entryBuffer,
                        current.levelFilter,
                        effectiveSearchQuery,
                    ),
                    paused = false,
                )
            } else {
                current.copy(paused = paused)
            }
        }
    }

    fun setUserPinnedScroll(pinned: Boolean) {
        mutableState.update { it.copy(userPinnedScroll = pinned) }
    }

    fun toggleExpanded(entryId: String) {
        mutableState.update { current ->
            current.copy(
                expandedEntryId = if (current.expandedEntryId == entryId) null else entryId,
            )
        }
    }

    fun clearDisplay() {
        pendingWhilePaused.clear()
        entryBuffer.clear()
        mutableState.update {
            it.copy(
                entries = emptyList(),
                filteredEntries = emptyList(),
                expandedEntryId = null,
                userPinnedScroll = false,
            )
        }
    }

    fun refresh() {
        scope.launch {
            try {
                mutableState.update { it.copy(loading = true, error = null) }
                logPort.loadRecent(maxEntries)
                    .onSuccess { loaded ->
                        replaceBuffer(entryBuffer, loaded)
                        mutableState.update { current ->
                            current.copy(
                                entries = entryBuffer.toList(),
                                filteredEntries = filterRuntimeLogEntries(
                                    entryBuffer,
                                    current.levelFilter,
                                    effectiveSearchQuery,
                                ),
                                loading = false,
                                error = null,
                            )
                        }
                    }
                    .onFailure { error ->
                        mutableState.update {
                            it.copy(
                                loading = false,
                                error = error.message ?: AppI18n.t(StringKeys.RuntimeLogs.ErrorLoadFailed),
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun exportFiltered(targetPath: String) {
        val filtered = state.value.filteredEntries
        if (filtered.isEmpty()) {
            mutableState.update {
                it.copy(exportMessage = AppI18n.t(StringKeys.RuntimeLogs.ErrorExportEmpty))
            }
            return
        }
        scope.launch {
            try {
                logPort.export(filtered, targetPath)
                    .onSuccess {
                        mutableState.update {
                            it.copy(exportMessage = AppI18n.t(StringKeys.RuntimeLogs.ExportSuccess))
                        }
                    }
                    .onFailure { error ->
                        mutableState.update {
                            it.copy(
                                exportMessage = error.message
                                    ?: AppI18n.t(StringKeys.RuntimeLogs.ErrorExportFailed),
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun dismissExportMessage() {
        mutableState.update { it.copy(exportMessage = null) }
    }

    fun dispose() {
        watchJob?.cancel()
        watchJob = null
        batchFlushJob?.cancel()
        batchFlushJob = null
        searchDebounceJob?.cancel()
        searchDebounceJob = null
    }

    private fun startWatching() {
        watchJob?.cancel()
        watchJob = scope.launch {
            try {
                logPort.watch().collect { entry ->
                    handleIncoming(entry)
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    private fun handleIncoming(entry: ApplicationLogEntry) {
        val sanitized = entry.copy(
            message = SensitiveRedactor.redact(entry.message),
            details = entry.details?.let(SensitiveRedactor::redact),
        )
        if (mutableState.value.paused) {
            appendToBuffer(pendingWhilePaused, sanitized)
            return
        }
        appendToBuffer(entryBuffer, sanitized)
        scheduleBatchFlush()
    }

    private fun scheduleBatchFlush() {
        if (batchFlushJob?.isActive == true) return
        batchFlushJob = scope.launch {
            try {
                delay(BATCH_FLUSH_MS)
                flushEntriesToState()
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    private fun flushEntriesToState() {
        mutableState.update { current ->
            current.copy(
                entries = entryBuffer.toList(),
                filteredEntries = filterRuntimeLogEntries(
                    entryBuffer,
                    current.levelFilter,
                    effectiveSearchQuery,
                ),
            )
        }
    }

    private fun scheduleSearchFilterUpdate() {
        searchDebounceJob?.cancel()
        searchDebounceJob = scope.launch {
            try {
                delay(SEARCH_DEBOUNCE_MS)
                effectiveSearchQuery = mutableState.value.searchQuery
                mutableState.update { current ->
                    current.copy(
                        filteredEntries = filterRuntimeLogEntries(
                            entryBuffer,
                            current.levelFilter,
                            effectiveSearchQuery,
                        ),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    private fun appendToBuffer(buffer: ArrayDeque<ApplicationLogEntry>, entry: ApplicationLogEntry) {
        if (buffer.size >= maxEntries) {
            buffer.removeFirst()
        }
        buffer.addLast(entry)
    }

    private fun replaceBuffer(buffer: ArrayDeque<ApplicationLogEntry>, incoming: List<ApplicationLogEntry>) {
        buffer.clear()
        incoming.takeLast(maxEntries).forEach { appendToBuffer(buffer, it) }
    }

    companion object {
        private const val BATCH_FLUSH_MS = 75L
        private const val SEARCH_DEBOUNCE_MS = 200L
    }
}
