package org.roberthu.rs.shell.runtimelogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.roberthu.rs.domain.ApplicationLogEntry
import org.roberthu.rs.domain.ApplicationLogLevel
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.presentation.RuntimeLogsUiState
import org.roberthu.rs.theme.logLevelColor

@Composable
fun RuntimeLogsScreen(
    state: RuntimeLogsUiState,
    onLevelFilterChange: (ApplicationLogLevel?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAutoScrollChange: (Boolean) -> Unit,
    onPausedChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onClearDisplay: () -> Unit,
    onExport: () -> Unit,
    onToggleExpanded: (String) -> Unit,
    onUserPinnedScrollChange: (Boolean) -> Unit,
    onDismissExportMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val filtered = state.filteredEntries
    val shouldAutoScroll by remember {
        derivedStateOf {
            state.autoScroll && !state.userPinnedScroll && filtered.isNotEmpty()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible < layoutInfo.totalItemsCount - 2
        }.distinctUntilChanged().collect { scrolledUp ->
            if (scrolledUp && state.autoScroll) {
                onUserPinnedScrollChange(true)
            }
        }
    }

    LaunchedEffect(filtered.size, shouldAutoScroll) {
        if (shouldAutoScroll && filtered.isNotEmpty()) {
            listState.animateScrollToItem(filtered.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("runtime_logs_screen"),
    ) {
        RuntimeLogsToolbar(
            state = state,
            onLevelFilterChange = onLevelFilterChange,
            onSearchQueryChange = onSearchQueryChange,
            onAutoScrollChange = onAutoScrollChange,
            onPausedChange = onPausedChange,
            onRefresh = onRefresh,
            onClearDisplay = onClearDisplay,
            onExport = onExport,
        )
        HorizontalDivider()
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.loading && state.entries.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).testTag("runtime_logs_loading"),
                    )
                }

                state.isEmpty -> {
                    RuntimeLogsEmptyState(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().testTag("runtime_logs_list"),
                    ) {
                        items(
                            items = filtered,
                            key = { it.id },
                        ) { entry ->
                            RuntimeLogRow(
                                entry = entry,
                                expanded = state.expandedEntryId == entry.id,
                                onToggleExpanded = { onToggleExpanded(entry.id) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }

            state.error?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .testTag("runtime_logs_error"),
                    action = {
                        TextButton(onClick = onRefresh) {
                            Text(t(StringKeys.Common.Refresh))
                        }
                    },
                ) {
                    Text(message)
                }
            }

            state.exportMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .testTag("runtime_logs_export_message"),
                    action = {
                        TextButton(onClick = onDismissExportMessage) {
                            Text(t(StringKeys.Common.Dismiss))
                        }
                    },
                ) {
                    Text(message)
                }
            }
        }
    }
}

@Composable
private fun RuntimeLogsToolbar(
    state: RuntimeLogsUiState,
    onLevelFilterChange: (ApplicationLogLevel?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAutoScrollChange: (Boolean) -> Unit,
    onPausedChange: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onClearDisplay: () -> Unit,
    onExport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = t(StringKeys.RuntimeLogs.Title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.testTag("runtime_logs_title"),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.levelFilter == null,
                onClick = { onLevelFilterChange(null) },
                label = { Text(t(StringKeys.RuntimeLogs.FilterAll)) },
                modifier = Modifier.testTag("runtime_logs_filter_all"),
            )
            ApplicationLogLevel.entries.forEach { level ->
                FilterChip(
                    selected = state.levelFilter == level,
                    onClick = { onLevelFilterChange(level) },
                    label = { Text(level.name) },
                    modifier = Modifier.testTag("runtime_logs_filter_${level.name}"),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("runtime_logs_search"),
                singleLine = true,
                placeholder = { Text(t(StringKeys.RuntimeLogs.SearchPlaceholder)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = t(StringKeys.RuntimeLogs.AutoScroll),
                    style = MaterialTheme.typography.labelMedium,
                )
                Switch(
                    checked = state.autoScroll,
                    onCheckedChange = onAutoScrollChange,
                    modifier = Modifier.testTag("runtime_logs_auto_scroll"),
                )
            }
            IconButton(
                onClick = { onPausedChange(!state.paused) },
                modifier = Modifier.testTag("runtime_logs_pause_toggle"),
            ) {
                Icon(
                    imageVector = if (state.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (state.paused) {
                        t(StringKeys.RuntimeLogs.Resume)
                    } else {
                        t(StringKeys.RuntimeLogs.Pause)
                    },
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("runtime_logs_refresh"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = t(StringKeys.Common.Refresh),
                )
            }
            IconButton(
                onClick = onClearDisplay,
                modifier = Modifier.testTag("runtime_logs_clear_display"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = t(StringKeys.RuntimeLogs.ClearDisplay),
                )
            }
            IconButton(
                onClick = onExport,
                modifier = Modifier.testTag("runtime_logs_export"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = t(StringKeys.RuntimeLogs.Export),
                )
            }
        }
    }
}

@Composable
private fun RuntimeLogsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = t(StringKeys.RuntimeLogs.EmptyTitle),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("runtime_logs_empty_title"),
        )
        Text(
            text = t(StringKeys.RuntimeLogs.EmptyDescription),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("runtime_logs_empty_description"),
        )
    }
}

@Composable
private fun RuntimeLogRow(
    entry: ApplicationLogEntry,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val levelColor = logLevelColor(entry.level)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .background(levelColor.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("runtime_log_row_${entry.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = entry.timestamp.ifBlank { "—" },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(176.dp),
            )
            Text(
                text = entry.level.name,
                style = MaterialTheme.typography.labelSmall,
                color = levelColor,
                modifier = Modifier.width(56.dp),
            )
            Text(
                text = entry.target.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(160.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = if (expanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (expanded) {
            val details = entry.details
            if (!details.isNullOrBlank()) {
                Text(
                    text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 8.dp, start = 176.dp)
                    .fillMaxWidth(),
                )
            }
        }
    }
}
