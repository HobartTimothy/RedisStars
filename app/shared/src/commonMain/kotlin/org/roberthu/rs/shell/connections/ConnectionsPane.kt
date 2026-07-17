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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import org.roberthu.rs.shell.icons.AppIcons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
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
import org.roberthu.rs.domain.MoveSidebarItemRequest
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
    onMoveSidebarItem: (MoveSidebarItemRequest) -> Unit = {},
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
            ConnectionTree(
                profiles = state.profiles,
                groups = state.groups,
                selectedProfileId = state.selectedProfileId,
                selectedGroupId = state.selectedGroupId,
                dragEnabled = state.dragReorderEnabled,
                onSelect = onSelect,
                onToggleGroupExpanded = onToggleGroupExpanded,
                onSelectGroup = onSelectGroup,
                onConnect = onConnect,
                onTest = onTest,
                onEdit = onEdit,
                onDelete = onDelete,
                onMoveSidebarItem = onMoveSidebarItem,
                onOpenGroupContextMenu = { offset, groupId ->
                    openContextMenu(offset, ConnectionsContextTarget.Group(groupId))
                },
                modifier = Modifier.weight(1f),
            )
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
                        leadingIcon = { Icon(AppIcons.AddLink, contentDescription = null) },
                        onClick = {
                            contextMenuExpanded = false
                            contextTarget = null
                            onBeginCreate(null)
                        },
                        modifier = Modifier.testTag("connections_add_connection"),
                    )
                    DropdownMenuItem(
                        text = { Text(t(StringKeys.Connections.ContextAddGroup)) },
                        leadingIcon = { Icon(AppIcons.CreateNewFolder, contentDescription = null) },
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
                        leadingIcon = { Icon(AppIcons.AddLink, contentDescription = null) },
                        onClick = {
                            contextMenuExpanded = false
                            contextTarget = null
                            onBeginCreate(target.id)
                        },
                        modifier = Modifier.testTag("connections_add_connection_in_group"),
                    )
                    DropdownMenuItem(
                        text = { Text(t(StringKeys.Connections.ContextCreateRootConnection)) },
                        leadingIcon = { Icon(AppIcons.AddLink, contentDescription = null) },
                        onClick = {
                            contextMenuExpanded = false
                            contextTarget = null
                            onBeginCreate(null)
                        },
                        modifier = Modifier.testTag("connections_add_root_connection"),
                    )
                    DropdownMenuItem(
                        text = { Text(t(StringKeys.Connections.ContextAddGroup)) },
                        leadingIcon = { Icon(AppIcons.CreateNewFolder, contentDescription = null) },
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
internal fun GroupHeaderRow(
    group: ConnectionGroup,
    count: Int,
    selected: Boolean,
    dragEnabled: Boolean = false,
    dragging: Boolean = false,
    highlightInside: Boolean = false,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onContextMenu: (Offset) -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        color = when {
            highlightInside -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .graphicsLayer { alpha = if (dragging) 0.55f else 1f }
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
            SidebarDragHandle(
                enabled = dragEnabled,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
            )
            Icon(
                if (group.expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(2.dp),
            )
            Icon(
                AppIcons.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                group.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (highlightInside) {
                Text(
                    t(StringKeys.Connections.MoveToGroup),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                count.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ConnectionRow(
    profile: ConnectionProfile,
    selected: Boolean,
    indented: Boolean,
    dragEnabled: Boolean = false,
    dragging: Boolean = false,
    onSelect: (ConnectionProfile) -> Unit,
    onConnect: (ConnectionProfile) -> Unit,
    onTest: (ConnectionProfile) -> Unit,
    onEdit: (ConnectionProfile) -> Unit,
    onDelete: (ConnectionProfile) -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
) {
    val tagColor = profile.browser.tagColor.toComposeColor()
    var actionsMenuExpanded by remember(profile.id) { mutableStateOf(false) }
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indented) 16.dp else 0.dp)
            .graphicsLayer { alpha = if (dragging) 0.55f else 1f }
            .pointerInput(profile.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            val change = event.changes.firstOrNull() ?: continue
                            actionsMenuExpanded = true
                            change.consume()
                        }
                    }
                }
            }
            .clickable { onSelect(profile) }
            .testTag("connection_${profile.id}"),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SidebarDragHandle(
                    enabled = dragEnabled,
                    onDragStart = onDragStart,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel,
                )
                ConnectionTagIndicator(color = tagColor)
                Text(
                    profile.name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(
                        onClick = { actionsMenuExpanded = true },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("connection_menu_${profile.id}"),
                    ) {
                        Icon(
                            AppIcons.MoreVert,
                            contentDescription = t(StringKeys.Connections.MoreActions),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = actionsMenuExpanded,
                        onDismissRequest = { actionsMenuExpanded = false },
                        modifier = Modifier.testTag("connection_actions_menu_${profile.id}"),
                    ) {
                        DropdownMenuItem(
                            text = { Text(t(StringKeys.Connections.Connect)) },
                            onClick = {
                                actionsMenuExpanded = false
                                onConnect(profile)
                            },
                            modifier = Modifier.testTag("connection_connect_${profile.id}"),
                        )
                        DropdownMenuItem(
                            text = { Text(t(StringKeys.Connections.Test)) },
                            onClick = {
                                actionsMenuExpanded = false
                                onTest(profile)
                            },
                            modifier = Modifier.testTag("connection_test_${profile.id}"),
                        )
                        DropdownMenuItem(
                            text = { Text(t(StringKeys.Connections.Edit)) },
                            onClick = {
                                actionsMenuExpanded = false
                                onEdit(profile)
                            },
                            modifier = Modifier.testTag("connection_edit_${profile.id}"),
                        )
                        DropdownMenuItem(
                            text = { Text(t(StringKeys.Connections.Delete)) },
                            onClick = {
                                actionsMenuExpanded = false
                                onDelete(profile)
                            },
                            modifier = Modifier.testTag("connection_delete_${profile.id}"),
                        )
                    }
                }
            }
            Text(
                connectionSummary(profile),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
