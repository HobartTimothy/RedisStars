package org.roberthu.rs.shell.connections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
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
import androidx.compose.ui.geometry.Offset
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
import org.roberthu.rs.presentation.ConnectionEditorSection
import org.roberthu.rs.presentation.ConnectionFormState
import org.roberthu.rs.presentation.ConnectionsUiState

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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissError()
    }

    fun openContextMenu(offset: Offset) {
        contextMenuOffset = offset
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
                            openContextMenu(change.position)
                        }
                    }
                }
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.key == Key.F10 &&
                    event.isShiftPressed
                ) {
                    openContextMenu(Offset(40f, 40f))
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
            Text("连接", style = MaterialTheme.typography.titleSmall)
            if (state.profiles.isEmpty() && state.groups.isEmpty()) {
                Text(
                    "添加 Redis 连接以开始浏览键。",
                    modifier = Modifier.testTag("connections_empty"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                state.groups.forEach { group ->
                    val groupProfiles = state.profiles.filter { it.groupId == group.id }
                    item(key = "group-header-${group.id}") {
                        GroupHeaderRow(
                            group = group,
                            count = groupProfiles.size,
                            selected = group.id == state.selectedGroupId,
                            onToggle = { onToggleGroupExpanded(group.id) },
                            onSelect = { onSelectGroup(group.id) },
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
                // Profiles without a group are listed flat — no "未分组" section header.
                val ungrouped = state.profiles.filter { it.groupId == null }
                items(ungrouped, key = { "root-${it.id}" }) { profile ->
                    ConnectionRow(
                        profile = profile,
                        selected = profile.id == state.selectedProfileId,
                        indented = false,
                        onSelect = onSelect,
                        onConnect = onConnect,
                        onTest = onTest,
                        onEdit = onEdit,
                        onDelete = onDelete,
                    )
                }
            }
        }

        DropdownMenu(
            expanded = contextMenuExpanded,
            onDismissRequest = { contextMenuExpanded = false },
            offset = DpOffset(contextMenuOffset.x.dp, contextMenuOffset.y.dp),
            modifier = Modifier.testTag("connections_context_menu"),
        ) {
            DropdownMenuItem(
                text = { Text("添加组") },
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                onClick = {
                    contextMenuExpanded = false
                    onOpenAddGroupDialog()
                },
                modifier = Modifier.testTag("connections_add_group"),
            )
            DropdownMenuItem(
                text = { Text("添加连接") },
                leadingIcon = { Icon(Icons.Default.AddLink, contentDescription = null) },
                onClick = {
                    contextMenuExpanded = false
                    onBeginCreate(state.selectedGroupId)
                },
                modifier = Modifier.testTag("connections_add_connection"),
            )
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
            title = { Text("新建连接组") },
            text = {
                OutlinedTextField(
                    value = dialog.name,
                    onValueChange = onUpdateGroupName,
                    label = { Text("组名称") },
                    isError = dialog.error != null,
                    supportingText = dialog.error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("connections_group_dialog"),
                )
            },
            confirmButton = {
                Button(onClick = onConfirmCreateGroup) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = onDismissGroupDialog) { Text("取消") }
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
            onPickSshPrivateKeyPath = onPickSshPrivateKeyPath,
        )
    }

    state.pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("删除连接？") },
            text = { Text("删除「${profile.name}」？此操作无法撤销。") },
            confirmButton = {
                Button(onClick = onConfirmDelete) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("取消") }
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
            .padding(vertical = 2.dp),
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
            Text(profile.name, style = MaterialTheme.typography.labelLarge)
            Text(
                connectionSummary(profile),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selected) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { onConnect(profile) }) { Text("连接") }
                    TextButton(onClick = { onTest(profile) }) { Text("测试") }
                    TextButton(
                        onClick = { onEdit(profile) },
                        modifier = Modifier.testTag("connection_edit_${profile.id}"),
                    ) { Text("编辑") }
                    TextButton(onClick = { onDelete(profile) }) { Text("删除") }
                }
            }
        }
    }
}

internal fun connectionSummary(profile: ConnectionProfile): String = when (profile.mode) {
    DeploymentMode.Standalone -> "${profile.host}:${profile.port}"
    DeploymentMode.Sentinel ->
        "Sentinel · ${profile.masterName.ifBlank { "—" }} · ${profile.sentinelNodes.size} nodes"
    DeploymentMode.Cluster -> "Cluster · ${profile.seedNodes.size} seeds"
}
