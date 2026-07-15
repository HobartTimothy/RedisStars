package org.roberthu.rs.shell.keys

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.presentation.KeyBrowserUiState

@Composable
fun KeyBrowserScreen(
    state: KeyBrowserUiState,
    enabled: Boolean,
    onPatternChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onCancel: () -> Unit,
    onSelect: (RedisKeySummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Keys", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = state.pattern,
            onValueChange = onPatternChange,
            label = { Text("Pattern") },
            placeholder = { Text("users:*") },
            enabled = enabled && !state.loading,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("key_pattern"),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onRefresh, enabled = enabled && !state.loading) { Text("Refresh") }
            if (state.loading) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                CircularProgressIndicator(modifier = Modifier.testTag("key_scan_loading"))
            }
        }
        if (!enabled) {
            Text(
                "Connect to Redis to browse keys.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.error?.let {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().testTag("key_scan_error"),
            ) {
                Text(
                    "$it Select Refresh to try again.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        state.partialFailures.takeIf { it.isNotEmpty() }?.let { failures ->
            Text(
                "${failures.size} cluster node(s) could not be scanned. Results may be incomplete.",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (enabled && !state.loading && state.error == null && state.keys.isEmpty()) {
            Text(
                "No keys match this pattern.",
                modifier = Modifier.testTag("key_list_empty"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(modifier = Modifier.weight(1f).testTag("key_list")) {
            items(state.keys, key = { it.key }) { key ->
                val selected = state.selectedKey?.key == key.key
                Surface(
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(key) }
                        .testTag("key_${key.key}"),
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(key.key, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            key.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (state.nextCursorToken != null) {
                item {
                    TextButton(
                        onClick = onLoadMore,
                        enabled = state.canLoadMore,
                        modifier = Modifier.fillMaxWidth().testTag("load_more"),
                    ) {
                        Text("Load more")
                    }
                }
            }
        }
    }
}
