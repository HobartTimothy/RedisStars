package org.roberthu.rs.shell.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.presentation.HostPortFormState

@Composable
fun HostPortListEditor(
    nodes: List<HostPortFormState>,
    enabled: Boolean,
    fieldErrors: Map<String, String>,
    listKey: String,
    defaultPort: String,
    onChange: (List<HostPortFormState>) -> Unit,
    newNodeId: () -> String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        fieldErrors[listKey]?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("connection_editor_${listKey}_error"),
            )
        }
        nodes.forEachIndexed { index, node ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = node.host,
                    onValueChange = { value ->
                        onChange(
                            nodes.mapIndexed { i, item ->
                                if (i == index) item.copy(host = value) else item
                            },
                        )
                    },
                    label = { Text(t(StringKeys.ConnectionEditor.HostLabel)) },
                    enabled = enabled,
                    singleLine = true,
                    isError = fieldErrors.containsKey("$listKey.$index.host"),
                    supportingText = fieldErrors["$listKey.$index.host"]?.let { { Text(it) } },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("connection_editor_${listKey}_${index}_host"),
                )
                OutlinedTextField(
                    value = node.port,
                    onValueChange = { value ->
                        onChange(
                            nodes.mapIndexed { i, item ->
                                if (i == index) item.copy(port = value) else item
                            },
                        )
                    },
                    label = { Text(t(StringKeys.ConnectionEditor.PortLabel)) },
                    enabled = enabled,
                    singleLine = true,
                    isError = fieldErrors.containsKey("$listKey.$index.port"),
                    supportingText = fieldErrors["$listKey.$index.port"]?.let { { Text(it) } },
                    modifier = Modifier
                        .weight(0.45f)
                        .testTag("connection_editor_${listKey}_${index}_port"),
                )
                IconButton(
                    onClick = { onChange(nodes.filterIndexed { i, _ -> i != index }) },
                    enabled = enabled && nodes.size > 1,
                    modifier = Modifier.testTag("connection_editor_${listKey}_${index}_delete"),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = t(StringKeys.ConnectionEditor.RemoveNode))
                }
            }
        }
        OutlinedButton(
            onClick = {
                onChange(nodes + HostPortFormState(newNodeId(), "", defaultPort))
            },
            enabled = enabled,
            modifier = Modifier.testTag("connection_editor_${listKey}_add"),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(t(StringKeys.ConnectionEditor.AddNode))
        }
    }
}
