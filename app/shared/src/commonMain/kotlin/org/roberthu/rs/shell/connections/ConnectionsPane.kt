package org.roberthu.rs.shell.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.i18n.AppI18n
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.presentation.ConnectionEditorSection
import org.roberthu.rs.presentation.ConnectionFormState
import org.roberthu.rs.presentation.ConnectionsUiState
import org.roberthu.rs.theme.toComposeColor

private sealed interface ConnectionsContextTarget {
    data object Root : ConnectionsContextTarget
    data class Group(val id: String) : ConnectionsContextTarget
}

private sealed interface ConnectionsTreeEntry {
    val sortName: String

    data class RootProfile(val profile: ConnectionProfile) : ConnectionsTreeEntry {
        override val sortName: String = profile.name
    }

    data class GroupEntry(val group: ConnectionGroup) : ConnectionsTreeEntry {
        override val sortName: String = group.name
    }
}

@Composable
fun ConnectionsPane(
    state: ConnectionsUiState,
    onSelect: (ConnectionProfile) -> Unit,
    onBeginCreate: (String?) -> Unit,
    onOpenAddGroupDialog: () -> Unit,
    onUpdateGroupName: (String) -> Unit,
    onConfirmCreateGroup: () -> Unit,
    onDismissGroupDialog: () -> Unit,
    onToggleGroupExpanded: (String) -> Unit,
    onSelectGroup: (String?) -> Unit = {},
    onEdit: (ConnectionProfile) -> Unit,
    onSelectEditorSection: (ConnectionEditorSection) -> Unit,
    onUpdateEditorForm: ((ConnectionFormState) -> ConnectionFormState) -> Unit,
    onRequestCloseEditor: () -> Unit,
    onConfirmDiscardEditor: () -> Unit,
    onDismissDiscardConfirmation: () -> Unit,
    onSaveEditor: () -> Unit,
    onTestEditor: () -> Unit,
    onParseClipboardUrl: (String) -> Unit,
    onTest: (ConnectionProfile) -> Unit,
    onConnect: (ConnectionProfile) -> Unit,
    onDelete: (ConnectionProfile) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onDismissError: () -> Unit = {},
    onPickSshPrivateKeyPath: () -> String? = { null },
    modifier: Modifier = Modifier,
) {
    var contextMenuExpanded by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var contextTarget by remember { mutableStateOf<ConnectionsContextTarget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissError()
    }

    fun openContextMenu(offset: Offset, target: ConnectionsContextTarget) {
        contextMenuOffset = offset
        contextTarget = target
        contextMenuExpanded = true
    }

    val treeEntries = remember(state.profiles, state.groups) {
        val ungrouped = state.profiles.filter { it.groupId == null }
        buildList {
            ungrouped.forEach { add(ConnectionsTreeEntry.RootProfile(it)) }
            state.groups.forEach { add(ConnectionsTreeEntry.GroupEntry(it)) }
        }.sortedBy { it.sortName }
    }

    Box(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val change = event.changes.firstOrNull() ?: continue
                            openContextMenu(change.position, ConnectionsContextTarget.Root)
                        }
                    }
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.key == Key.F10 &&
                    event.isShiftPressed
                ) {
                    openContextMenu(Offset(40f, 40f), ConnectionsContextTarget.Root)
                    true
                } else {
                    false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(t(StringKeys.Connections.Title), style = MaterialTheme.typography.titleSmall)
            if (state.profiles.isEmpty() && state.groups.isEmpty()) {
                Text(
                    t(StringKeys.Connections.Empty),
                    modifier = Modifier.testTag("connections_empty"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                treeEntries.forEach { entry ->
                    when (entry) {
                        is ConnectionsTreeEntry.RootProfile -> {
                            item(key = "root-${entry.profile.id}") {
                                ConnectionRow(
                                    profile = entry.profile,
                                    selected = entry.profile.id == state.selectedProfileId,
                                    indented = false,
                                    onSelect = onSelect,
                                    onConnect = onConnect,
                                    onTest = onTest,
                                    onEdit = onEdit,
                                    onDelete = onDelete,
                                )
                            }
                        }
                        is ConnectionsTreeEntry.GroupEntry -> {
                            val group = entry.group
                            val groupProfiles = state.profiles.filter { it.groupId == group.id }
                            item(key = "group-header-${group.id}") {
                                GroupHeaderRow(
                                    group = group,
                                    count = groupProfiles.size,
                                    selected = group.id == state.selectedGroupId,
                                    onToggle = { onToggleGroupExpanded(group.id) },
                                    onSelect = { onSelectGroup(group.id) },
                                    onContextMenu = { offset ->
                                        openContextMenu(offset, ConnectionsContextTarget.Group(group.id))
                                    },
                                    modifier = Modifier.testTag("connections_group_${group.id}"),
                                )
                            }
                            if (group.expanded) {
                                items(groupProfiles, key = { "group-${group.id}-${it.id}" }) { profile ->
                                    ConnectionRow(
                                        profile = profile,
                                        selected = profile.id == state.selectedProfileId,
                                        indented = true,
                                        onSelect = onSelect,
                                        onConnect = onConnect,
                                        onTest = onTest,
                                        onEdit = onEdit,
                                        onDelete = onDelete,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = contextMenuExpanded,
            onDismissRequest = {
                contextMenuExpanded = false
                contextTarget = null
            },
            offset = DpOffset(contextMenuOffset.x.dp, contextMenuOffset.y.dp),
            modifier = Modifier.testTag("connections_context_menu"),
        ) {
            when (val target = contextTarget) {
                ConnectionsContextTarget.Root -> {
                    DropdownMenuItem(
                        text = { Text(t(StringKeys.Connections.ContextCreateConnection)) },
                        leadingIcon = { Icon(Icons.Default.AddLink, contentDescription = null) },
                        onClick = {
                            contextMenuExpanded = false
                            contextTarget = null
                            onBeginCreate(null)
                        },
                        modifier = Modifier.testTag("connections_add_connection"),
                    )
                    DropdownMenuItem(
                        text = { Text(t(StringKeys.Connections.ContextAddGroup)) },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            contextMenuExpanded = false
                            contextTarget = null
                            onOpenAddGroupDialog()
                        },
                        modifier = Modifier.testTag("connections_add_group"),
                    )
                }
                is ConnectionsContextTarget.Group -> {
                    DropdownMenuItem(
                        text = { Text(t(StringKeys.Connections.ContextCreateConnectionInGroup)) },
                        leadingIcon = { Icon(Icons.Default.AddLink, contentDescription = null) },
                        onClick = {
                            contextMenuExpanded = false
                            contextTarget = null
                            onBeginCreate(target.id)
                        },
                        modifier = Modifier.testTag("connections_add_connection_in_group"),
                    )
                    DropdownMenuItem(
                        text = { Text(t(StringKeys.Connections.ContextCreateRootConnection)) },
                        leadingIcon = { Icon(Icons.Default.AddLink, contentDescription = null) },
                        onClick = {
                            contextMenuExpanded = false
                            contextTarget = null
                            onBeginCreate(null)
                        },
                        modifier = Modifier.testTag("connections_add_root_connection"),
                    )
                    DropdownMenuItem(
                        text = { Text(t(StringKeys.Connections.ContextAddGroup)) },
                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            contextMenuExpanded = false
                            contextTarget = null
                            onOpenAddGroupDialog()
                        },
                        modifier = Modifier.testTag("connections_add_group"),
                    )
                }
                null -> Unit
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag("connections_error_toast"),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }

    state.groupDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = onDismissGroupDialog,
            title = { Text(t(StringKeys.Connections.GroupDialogTitle)) },
            text = {
                OutlinedTextField(
                    value = dialog.name,
                    onValueChange = onUpdateGroupName,
                    label = { Text(t(StringKeys.Connections.GroupNameLabel)) },
                    isError = dialog.error != null,
                    supportingText = dialog.error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("connections_group_dialog"),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmCreateGroup) { Text(t(StringKeys.Connections.GroupConfirm)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissGroupDialog) { Text(t(StringKeys.Connections.GroupCancel)) }
            },
        )
    }

    state.editor?.let { editor ->
        ConnectionEditorDialog(
            state = editor,
            onSelectSection = onSelectEditorSection,
            onUpdateForm = onUpdateEditorForm,
            onRequestClose = onRequestCloseEditor,
            onConfirmDiscard = onConfirmDiscardEditor,
            onDismissDiscard = onDismissDiscardConfirmation,
            onTest = onTestEditor,
            onSave = onSaveEditor,
            onParseUrl = onParseClipboardUrl,
            groups = state.groups,
            onPickSshPrivateKeyPath = onPickSshPrivateKeyPath,
        )
    }

    state.pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(t(StringKeys.Connections.DeleteTitle)) },
            text = { Text(t(StringKeys.Connections.DeleteMessage, profile.name)) },
            confirmButton = {
                Button(onClick = onConfirmDelete) { Text(t(StringKeys.Connections.Delete)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text(t(StringKeys.Connections.GroupCancel)) }
            },
        )
    }
}

@Composable
private fun GroupHeaderRow(
    group: ConnectionGroup,
    count: Int,
    selected: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onContextMenu: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .pointerInput(group.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val change = event.changes.firstOrNull() ?: continue
                            onContextMenu(change.position)
                            change.consume()
                        }
                    }
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onSelect()
                    onToggle()
                }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (group.expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(2.dp),
            )
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                group.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectionRow(
    profile: ConnectionProfile,
    selected: Boolean,
    indented: Boolean,
    onSelect: (ConnectionProfile) -> Unit,
    onConnect: (ConnectionProfile) -> Unit,
    onTest: (ConnectionProfile) -> Unit,
    onEdit: (ConnectionProfile) -> Unit,
    onDelete: (ConnectionProfile) -> Unit,
) {
    val tagColor = profile.browser.tagColor.toComposeColor()
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indented) 16.dp else 0.dp)
            .clickable { onSelect(profile) }
            .testTag("connection_${profile.id}"),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConnectionTagIndicator(color = tagColor)
                Text(profile.name, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                connectionSummary(profile),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selected) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { onConnect(profile) }) { Text(t(StringKeys.Connections.Connect)) }
                    TextButton(onClick = { onTest(profile) }) { Text(t(StringKeys.Connections.Test)) }
                    TextButton(
                        onClick = { onEdit(profile) },
                        modifier = Modifier.testTag("connection_edit_${profile.id}"),
                    ) { Text(t(StringKeys.Connections.Edit)) }
                    TextButton(onClick = { onDelete(profile) }) { Text(t(StringKeys.Connections.Delete)) }
                }
            }
        }
    }
}

@Composable
private fun ConnectionTagIndicator(color: Color?) {
    if (color == null) {
        Icon(
            Icons.Default.Close,
            contentDescription = null,
            modifier = Modifier.size(10.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        )
    } else {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

internal fun connectionSummary(profile: ConnectionProfile): String = when (profile.mode) {
    DeploymentMode.Standalone -> "${profile.host}:${profile.port}"
    DeploymentMode.Sentinel ->
        AppI18n.t(
            StringKeys.Connections.SummarySentinel,
            profile.masterName.ifBlank { "—" },
            profile.sentinelNodes.size,
        )
    DeploymentMode.Cluster ->
        AppI18n.t(StringKeys.Connections.SummaryCluster, profile.seedNodes.size)
}
