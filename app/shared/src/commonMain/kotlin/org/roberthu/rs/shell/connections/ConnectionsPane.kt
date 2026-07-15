package org.roberthu.rs.shell.connections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.presentation.ConnectionEditorSection
import org.roberthu.rs.presentation.ConnectionFormState
import org.roberthu.rs.presentation.ConnectionsUiState

@Composable
fun ConnectionsPane(
    state: ConnectionsUiState,
    onSelect: (ConnectionProfile) -> Unit,
    onAdd: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Connections", style = MaterialTheme.typography.titleSmall)
            TextButton(
                onClick = onAdd,
                modifier = Modifier.testTag("connections_add"),
            ) { Text("Add") }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.profiles.isEmpty()) {
            Text(
                "Add a Redis connection to start browsing keys.",
                modifier = Modifier.testTag("connections_empty"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.profiles, key = { it.id }) { profile ->
                val selected = profile.id == state.selectedProfileId
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    modifier = Modifier
                        .fillMaxWidth()
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
                                TextButton(onClick = { onConnect(profile) }) { Text("Connect") }
                                TextButton(onClick = { onTest(profile) }) { Text("Test") }
                                TextButton(
                                    onClick = { onEdit(profile) },
                                    modifier = Modifier.testTag("connection_edit_${profile.id}"),
                                ) { Text("Edit") }
                                TextButton(onClick = { onDelete(profile) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
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
        )
    }

    state.pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete connection?") },
            text = { Text("Delete “${profile.name}”? This cannot be undone.") },
            confirmButton = {
                Button(onClick = onConfirmDelete) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Cancel") }
            },
        )
    }
}

internal fun connectionSummary(profile: ConnectionProfile): String = when (profile.mode) {
    DeploymentMode.Standalone -> "${profile.host}:${profile.port} · db ${profile.database}"
    DeploymentMode.Sentinel ->
        "Sentinel · ${profile.masterName.ifBlank { "—" }} · ${profile.sentinelNodes.size} nodes"
    DeploymentMode.Cluster -> "Cluster · ${profile.seedNodes.size} seeds"
}
