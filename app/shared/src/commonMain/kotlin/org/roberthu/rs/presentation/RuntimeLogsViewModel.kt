package org.roberthu.rs.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

    private val pendingWhilePaused = ArrayDeque<ApplicationLogEntry>()
    private var watchJob: Job? = null

    init {
        refresh()
        startWatching()
    }

    fun setLevelFilter(level: ApplicationLogLevel?) {
        mutableState.update { it.copy(levelFilter = level) }
    }

    fun setSearchQuery(query: String) {
        mutableState.update { it.copy(searchQuery = query) }
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
                val merged = mergeEntries(current.entries, pendingWhilePaused.toList())
                pendingWhilePaused.clear()
                current.copy(entries = merged, paused = false)
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
        mutableState.update {
            it.copy(
                entries = emptyList(),
                expandedEntryId = null,
                userPinnedScroll = false,
            )
        }
    }

    fun refresh() {
        scope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            logPort.loadRecent(maxEntries)
                .onSuccess { loaded ->
                    mutableState.update {
                        it.copy(
                            entries = mergeEntries(emptyList(), loaded),
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
        }
    }

    fun dismissExportMessage() {
        mutableState.update { it.copy(exportMessage = null) }
    }

    fun dispose() {
        watchJob?.cancel()
        watchJob = null
    }

    private fun startWatching() {
        watchJob?.cancel()
        watchJob = scope.launch {
            logPort.watch().collect { entry ->
                handleIncoming(entry)
            }
        }
    }

    private fun handleIncoming(entry: ApplicationLogEntry) {
        val sanitized = entry.copy(
            message = SensitiveRedactor.redact(entry.message),
            details = entry.details?.let(SensitiveRedactor::redact),
        )
        mutableState.update { current ->
            if (current.paused) {
                pendingWhilePaused.addLast(sanitized)
                current
            } else {
                current.copy(entries = mergeEntries(current.entries, listOf(sanitized)))
            }
        }
    }

    private fun mergeEntries(
        existing: List<ApplicationLogEntry>,
        incoming: List<ApplicationLogEntry>,
    ): List<ApplicationLogEntry> {
        if (incoming.isEmpty()) return existing
        val merged = ArrayList<ApplicationLogEntry>(existing.size + incoming.size)
        merged.addAll(existing)
        merged.addAll(incoming)
        return if (merged.size <= maxEntries) merged else merged.takeLast(maxEntries)
    }
}
