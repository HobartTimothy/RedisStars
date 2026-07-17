package org.roberthu.rs.presentation

import org.roberthu.rs.domain.ApplicationLogEntry
import org.roberthu.rs.domain.ApplicationLogLevel

data class RuntimeLogsUiState(
    val entries: List<ApplicationLogEntry> = emptyList(),
    val filteredEntries: List<ApplicationLogEntry> = emptyList(),
    val levelFilter: ApplicationLogLevel? = null,
    val searchQuery: String = "",
    val autoScroll: Boolean = true,
    val paused: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val expandedEntryId: String? = null,
    val userPinnedScroll: Boolean = false,
    val exportMessage: String? = null,
) {
    val isEmpty: Boolean get() = !loading && error == null && filteredEntries.isEmpty()
}

internal fun filterRuntimeLogEntries(
    entries: Iterable<ApplicationLogEntry>,
    levelFilter: ApplicationLogLevel?,
    searchQuery: String,
): List<ApplicationLogEntry> {
    val query = searchQuery.trim()
    return entries.filter { entry ->
        val levelMatches = levelFilter == null || entry.level == levelFilter
        val queryMatches = query.isEmpty() ||
            entry.message.contains(query, ignoreCase = true) ||
            entry.target?.contains(query, ignoreCase = true) == true ||
            entry.details?.contains(query, ignoreCase = true) == true ||
            entry.timestamp.contains(query, ignoreCase = true)
        levelMatches && queryMatches
    }
}
