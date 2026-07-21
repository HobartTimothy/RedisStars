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
import org.roberthu.rs.domain.ConnectionBrowserOptions
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.normalized
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.presentation.ConnectionsViewModel
import org.roberthu.rs.presentation.KeyBrowserViewModel
import org.roberthu.rs.presentation.KeyDetailViewModel
import org.roberthu.rs.shell.detail.KeyDetailScreen
import org.roberthu.rs.shell.keys.AddKeyDialog
import org.roberthu.rs.shell.keys.KeyBrowserScreen
import org.roberthu.rs.ui.theme.RedisTheme

@Composable
fun ConnectionsWorkspace(
    connections: ConnectionsViewModel?,
    browser: KeyBrowserViewModel?,
    detail: KeyDetailViewModel?,
    modifier: Modifier = Modifier,
    onImportText: () -> String? = { null },
    onPickSshPrivateKeyPath: () -> String? = { null },
) {
    if (connections == null || browser == null || detail == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                t(StringKeys.Connections.UnavailablePreview),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val connectionsState by connections.state.collectAsState()
    val browserState by browser.state.collectAsState()
    val detailState by detail.state.collectAsState()
    val connected = connectionsState.connectionState is ConnectionState.Connected

    val connectedProfile = when (val connection = connectionsState.connectionState) {
        is ConnectionState.Connected ->
            connectionsState.profiles.firstOrNull { it.id == connection.profileId }
        else -> null
    }

    LaunchedEffect(connectionsState.connectionState, connectedProfile?.browser, connectedProfile?.mode, connectedProfile?.database) {
        when (val state = connectionsState.connectionState) {
            is ConnectionState.Connected -> {
                val profile = connectionsState.profiles.firstOrNull { it.id == state.profileId }
                val clusterMode = profile?.mode == DeploymentMode.Cluster
                val initialDatabase = if (clusterMode) 0 else profile?.database ?: 0
                val browserOptions = profile?.browser?.normalized() ?: ConnectionBrowserOptions()
                browser.onConnected(
                    clusterMode = clusterMode,
                    initialDatabase = initialDatabase,
                    browserOptions = browserOptions,
                )
            }
            ConnectionState.Disconnected -> browser.onDisconnected()
            else -> Unit
        }
    }

    LaunchedEffect(detailState.deletedKey) {
        if (detailState.deletedKey != null) browser.refresh()
    }

    LaunchedEffect(browserState.selectedKey) {
        if (browserState.selectedKey == null) {
            detail.clear()
        }
    }

    LaunchedEffect(browserState.lastCreatedKey) {
        browserState.lastCreatedKey?.let { key ->
            detail.load(key)
        }
    }

    val colors = RedisTheme.colors
    Row(modifier = modifier.fillMaxSize().background(colors.appBackground)) {
        ConnectionsPane(
            state = connectionsState,
            onSelect = connections::select,
            onBeginCreate = connections::beginCreate,
            onOpenAddGroupDialog = connections::openAddGroupDialog,
            onUpdateGroupName = connections::updateGroupName,
            onConfirmCreateGroup = connections::confirmCreateGroup,
            onDismissGroupDialog = connections::dismissGroupDialog,
            onToggleGroupExpanded = connections::toggleGroupExpanded,
            onSelectGroup = connections::selectGroup,
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
            onDismissError = connections::dismissError,
            onPickSshPrivateKeyPath = onPickSshPrivateKeyPath,
            onMoveSidebarItem = connections::moveSidebarItem,
            modifier = Modifier.background(colors.paneSurface),
        )
        if (connected) {
            KeyBrowserScreen(
                state = browserState,
                enabled = true,
                onPatternChange = browser::setPattern,
                onRefresh = browser::refresh,
                onLoadMore = browser::loadMore,
                onCancel = browser::cancel,
                onSelect = { key ->
                    browser.select(key)
                    detail.load(key)
                },
                onOpenAddKey = browser::openAddKeyDialog,
                onSelectDatabase = browser::selectDatabase,
                onTypeFilterChange = browser::setTypeFilter,
                onKeyListViewChange = browser::setKeyListView,
            )
        }
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

    browserState.addKeyDialog?.let { dialog ->
        AddKeyDialog(
            state = dialog,
            databases = browserState.databases,
            clusterMode = browserState.clusterMode,
            onDismiss = browser::closeAddKeyDialog,
            onKeyChange = browser::updateAddKeyKey,
            onDatabaseChange = browser::updateAddKeyDatabase,
            onTypeChange = browser::changeAddKeyType,
            onTtlTextChange = browser::updateAddKeyTtlText,
            onPermanentChange = browser::updateAddKeyPermanent,
            onStringValueChange = browser::updateAddKeyStringValue,
            onHashFieldsChange = browser::updateAddKeyHashFields,
            onListValuesChange = browser::updateAddKeyListValues,
            onSetMembersChange = browser::updateAddKeySetMembers,
            onZsetEntriesChange = browser::updateAddKeyZsetEntries,
            onStreamFieldsChange = browser::updateAddKeyStreamFields,
            onJsonContentChange = browser::updateAddKeyJsonContent,
            onImportClick = {
                onImportText()?.let(browser::applyImportedText)
            },
            onSubmit = browser::submitNewKey,
        )
    }
}
