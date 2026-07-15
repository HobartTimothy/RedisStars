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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.presentation.ConnectionsUiState

@Composable
fun ConnectionsPane(
    state: ConnectionsUiState,
    onSelect: (ConnectionProfile) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ConnectionProfile) -> Unit,
    onUpdateDraft: ((ConnectionProfile) -> ConnectionProfile) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
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
            TextButton(onClick = onAdd) { Text("Add") }
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.profiles.isEmpty() && state.draft == null) {
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
                            "${profile.host}:${profile.port} · db ${profile.database}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (selected) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                TextButton(onClick = { onConnect(profile) }) { Text("Connect") }
                                TextButton(onClick = { onTest(profile) }) { Text("Test") }
                                TextButton(onClick = { onEdit(profile) }) { Text("Edit") }
                                TextButton(onClick = { onDelete(profile) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
        state.draft?.let { draft ->
            HorizontalDivider()
            ConnectionEditor(
                draft = draft,
                testSucceeded = state.testSucceeded,
                busy = state.busy,
                onUpdate = onUpdateDraft,
                onSave = onSave,
                onCancel = onCancelEdit,
                onTest = { onTest(draft) },
            )
        }
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

@Composable
private fun ConnectionEditor(
    draft: ConnectionProfile,
    testSucceeded: Boolean,
    busy: Boolean,
    onUpdate: ((ConnectionProfile) -> ConnectionProfile) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onTest: () -> Unit,
) {
    Column(
        modifier = Modifier.testTag("connection_editor"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Standalone connection", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = draft.name,
            onValueChange = { value -> onUpdate { it.copy(name = value) } },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.host,
            onValueChange = { value -> onUpdate { it.copy(host = value) } },
            label = { Text("Host") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = draft.port.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { port -> onUpdate { it.copy(port = port) } }
                },
                label = { Text("Port") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = draft.database.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { db -> onUpdate { it.copy(database = db) } }
                },
                label = { Text("DB") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = draft.username.orEmpty(),
            onValueChange = { value -> onUpdate { it.copy(username = value.ifBlank { null }) } },
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.password.orEmpty(),
            onValueChange = { value -> onUpdate { it.copy(password = value.ifBlank { null }) } },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TLS", modifier = Modifier.weight(1f))
            Switch(
                checked = draft.tls.enabled,
                onCheckedChange = { enabled ->
                    onUpdate { it.copy(tls = it.tls.copy(enabled = enabled)) }
                },
            )
        }
        if (testSucceeded) {
            Text("Connection test succeeded", color = MaterialTheme.colorScheme.primary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onSave, enabled = !busy) { Text("Save") }
            OutlinedButton(onClick = onTest, enabled = !busy) { Text("Test") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}
