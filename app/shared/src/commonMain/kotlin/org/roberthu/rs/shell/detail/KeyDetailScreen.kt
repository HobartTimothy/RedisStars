package org.roberthu.rs.shell.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.BinarySafeString
import org.roberthu.rs.presentation.KeyContent
import org.roberthu.rs.presentation.KeyDetailUiState

@Composable
fun KeyDetailScreen(
    state: KeyDetailUiState,
    onRefresh: () -> Unit,
    onSaveString: (String) -> Unit,
    onRename: (String) -> Unit,
    onSetTtl: (Long?) -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val key = state.key
    if (key == null) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                state.deletedKey?.let { "Deleted “$it”." } ?: "Select a key to inspect its value.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("key_detail_empty"),
            )
        }
        return
    }

    var rename by remember(key.key) { mutableStateOf(key.key) }
    var ttlText by remember(state.metadata?.ttlSeconds) {
        mutableStateOf(state.metadata?.ttlSeconds?.toString().orEmpty())
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(key.key, style = MaterialTheme.typography.titleMedium)
                Text(
                    state.metadata?.type?.name ?: key.type.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                TextButton(onClick = onRefresh, enabled = !state.loading) { Text("Refresh") }
                TextButton(onClick = onRequestDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        state.error?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                )
            }
        }
        if (state.loading) {
            CircularProgressIndicator()
        } else {
            state.metadata?.let { metadata ->
                Text(
                    "TTL: ${metadata.ttlSeconds?.let { "$it s" } ?: "persistent"}" +
                        "  ·  Memory: ${metadata.memoryBytes?.let { "$it B" } ?: "unknown"}" +
                        "  ·  Encoding: ${metadata.encoding ?: "unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rename,
                    onValueChange = { rename = it },
                    label = { Text("Key name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { onRename(rename) },
                    enabled = rename.isNotBlank() && rename != key.key && !state.saving,
                ) { Text("Rename") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ttlText,
                    onValueChange = { ttlText = it },
                    label = { Text("TTL seconds") },
                    placeholder = { Text("Persistent") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                    onClick = { ttlText.toLongOrNull()?.let(onSetTtl) },
                    enabled = ttlText.toLongOrNull()?.let { it >= 0 } == true && !state.saving,
                ) { Text("Set TTL") }
                TextButton(onClick = { onSetTtl(null) }, enabled = !state.saving) {
                    Text("Persist")
                }
            }
            KeyContentView(
                content = state.content,
                saving = state.saving,
                onSaveString = onSaveString,
            )
        }
    }

    if (state.confirmDelete) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete key?") },
            text = { Text("Permanently delete “${key.key}”? This cannot be undone.") },
            confirmButton = {
                Button(onClick = onConfirmDelete) { Text("Delete key") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun KeyContentView(
    content: KeyContent?,
    saving: Boolean,
    onSaveString: (String) -> Unit,
) {
    when (content) {
        null -> Unit
        is KeyContent.StringValue -> {
            BinaryWarnings(content.value)
            var value by remember(content.value) { mutableStateOf(display(content.value)) }
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("String value") },
                readOnly = content.value.utf8 == null,
                minLines = 6,
                modifier = Modifier.fillMaxWidth().testTag("string_value"),
            )
            Button(
                onClick = { onSaveString(value) },
                enabled = content.value.utf8 != null && !content.value.truncated && !saving,
            ) { Text("Save value") }
        }
        is KeyContent.HashValue -> {
            Text("Hash entries (${content.entries.size} shown)")
            content.entries.forEach { entry ->
                BinaryWarnings(entry.field)
                BinaryWarnings(entry.value)
                Text("${display(entry.field)}  →  ${display(entry.value)}")
            }
        }
        is KeyContent.ListValue -> {
            Text("List entries (${content.entries.size} shown)")
            content.entries.forEachIndexed { index, value ->
                BinaryWarnings(value)
                Text("$index  ${display(value)}")
            }
        }
        is KeyContent.SetValue -> {
            Text("Set members (${content.members.size} shown)")
            content.members.forEach {
                BinaryWarnings(it)
                Text(display(it))
            }
        }
        is KeyContent.ZSetValue -> {
            Text("Sorted-set entries (${content.entries.size} shown)")
            content.entries.forEach {
                BinaryWarnings(it.member)
                Text("${it.score}  ${display(it.member)}")
            }
        }
        is KeyContent.Unsupported -> Text(
            content.reason,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BinaryWarnings(value: BinarySafeString) {
    if (value.utf8 == null) {
        WarningBanner("Binary value — showing Base64. Editing is disabled.")
    }
    if (value.truncated) {
        WarningBanner("Value truncated for safety. Editing is disabled.")
    }
}

@Composable
private fun WarningBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun display(value: BinarySafeString): String =
    value.utf8 ?: "base64:${value.base64}"
