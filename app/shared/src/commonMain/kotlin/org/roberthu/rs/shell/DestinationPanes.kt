package org.roberthu.rs.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.platform.FileSavePicker
import org.roberthu.rs.platform.runtimeLogExportFileName
import org.roberthu.rs.presentation.RuntimeLogsViewModel
import org.roberthu.rs.shell.runtimelogs.RuntimeLogsScreen

@Composable
fun MonitorPane(modifier: Modifier = Modifier) {
    PlaceholderDestination(
        title = t(StringKeys.Monitor.Title),
        subtitle = t(StringKeys.Monitor.Placeholder),
        modifier = modifier,
    )
}

@Composable
fun RuntimeLogsPane(
    viewModel: RuntimeLogsViewModel?,
    runtimeLogSavePicker: FileSavePicker,
    modifier: Modifier = Modifier,
) {
    if (viewModel == null) {
        PlaceholderDestination(
            title = t(StringKeys.RuntimeLogs.Title),
            subtitle = t(StringKeys.RuntimeLogs.UnavailablePreview),
            modifier = modifier,
        )
        return
    }
    val state by viewModel.state.collectAsState()
    val exportDialogTitle = t(StringKeys.RuntimeLogs.ExportDialogTitle)
    RuntimeLogsScreen(
        state = state,
        onLevelFilterChange = viewModel::setLevelFilter,
        onSearchQueryChange = viewModel::setSearchQuery,
        onAutoScrollChange = viewModel::setAutoScroll,
        onPausedChange = viewModel::setPaused,
        onRefresh = viewModel::refresh,
        onClearDisplay = viewModel::clearDisplay,
        onExport = {
            val path = runtimeLogSavePicker.pickSavePath(
                title = exportDialogTitle,
                defaultFileName = runtimeLogExportFileName(),
            )
            if (path != null) {
                viewModel.exportFiltered(path)
            }
        },
        onToggleExpanded = viewModel::toggleExpanded,
        onUserPinnedScrollChange = viewModel::setUserPinnedScroll,
        onDismissExportMessage = viewModel::dismissExportMessage,
        modifier = modifier,
    )
}

@Composable
private fun PlaceholderDestination(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "$title\n$subtitle",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}
