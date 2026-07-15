package org.roberthu.rs.shell.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.presentation.ConnectionsViewModel
import org.roberthu.rs.presentation.KeyBrowserViewModel
import org.roberthu.rs.presentation.KeyDetailViewModel
import org.roberthu.rs.shell.detail.KeyDetailScreen
import org.roberthu.rs.shell.keys.KeyBrowserScreen

@Composable
fun ConnectionsWorkspace(
    connections: ConnectionsViewModel?,
    browser: KeyBrowserViewModel?,
    detail: KeyDetailViewModel?,
    modifier: Modifier = Modifier,
) {
    if (connections == null || browser == null || detail == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Redis access is unavailable in this preview.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val connectionsState by connections.state.collectAsState()
    val browserState by browser.state.collectAsState()
    val detailState by detail.state.collectAsState()
    val connected = connectionsState.connectionState is ConnectionState.Connected

    LaunchedEffect(detailState.deletedKey) {
        if (detailState.deletedKey != null) browser.refresh()
    }

    Row(modifier = modifier.fillMaxSize()) {
        ConnectionsPane(
            state = connectionsState,
            onSelect = connections::select,
            onAdd = connections::beginCreate,
            onEdit = connections::edit,
            onSelectEditorSection = connections::selectEditorSection,
            onUpdateEditorForm = connections::updateEditorForm,
            onRequestCloseEditor = connections::requestCloseEditor,
            onConfirmDiscardEditor = connections::confirmDiscardEditor,
            onDismissDiscardConfirmation = connections::dismissDiscardConfirmation,
            onSaveEditor = connections::saveEditor,
            onTestEditor = connections::testEditor,
            onParseClipboardUrl = connections::parseClipboardUrl,
            onTest = connections::test,
            onConnect = connections::connect,
            onDelete = connections::requestDelete,
            onConfirmDelete = connections::confirmDelete,
            onDismissDelete = connections::dismissDelete,
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer),
        )
        KeyBrowserScreen(
            state = browserState,
            enabled = connected,
            onPatternChange = browser::setPattern,
            onRefresh = browser::refresh,
            onLoadMore = browser::loadMore,
            onCancel = browser::cancel,
            onSelect = { key ->
                browser.select(key)
                detail.load(key)
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow),
        )
        KeyDetailScreen(
            state = detailState,
            onRefresh = detail::refresh,
            onSaveString = detail::saveString,
            onRename = detail::rename,
            onSetTtl = detail::setTtl,
            onRequestDelete = detail::requestDelete,
            onConfirmDelete = detail::confirmDelete,
            onDismissDelete = detail::dismissDelete,
            modifier = Modifier.weight(1f),
        )
    }
}
