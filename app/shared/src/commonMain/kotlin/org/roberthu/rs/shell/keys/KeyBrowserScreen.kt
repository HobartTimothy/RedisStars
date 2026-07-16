package org.roberthu.rs.shell.keys

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.presentation.KeyBrowserUiState
import org.roberthu.rs.theme.color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyBrowserScreen(
    state: KeyBrowserUiState,
    enabled: Boolean,
    onPatternChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onCancel: () -> Unit,
    onSelect: (RedisKeySummary) -> Unit,
    onOpenAddKey: () -> Unit,
    onSelectDatabase: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(min = 340.dp)
            .width(340.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "键 (${state.keys.size})",
                style = MaterialTheme.typography.titleSmall,
            )
            Row {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { Text("刷新") },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = enabled && !state.loading,
                        modifier = Modifier.testTag("keys_refresh"),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { Text("添加键") },
                    state = rememberTooltipState(),
                ) {
                    IconButton(
                        onClick = onOpenAddKey,
                        enabled = enabled,
                        modifier = Modifier.testTag("keys_add"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加键")
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.pattern,
            onValueChange = onPatternChange,
            label = { Text("匹配模式") },
            placeholder = { Text("users:*") },
            enabled = enabled && !state.loading,
            singleLine = true,
            trailingIcon = {
                if (state.pattern.isNotEmpty()) {
                    IconButton(onClick = { onPatternChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("key_pattern")
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        onRefresh()
                        true
                    } else {
                        false
                    }
                },
        )

        if (state.loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("key_scan_loading"),
                )
                TextButton(onClick = onCancel) { Text("取消") }
            }
        }

        if (!enabled) {
            Text(
                "连接 Redis 后可浏览键。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.error?.let {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().testTag("key_scan_error"),
            ) {
                Text(
                    "$it 选择刷新重试。",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        state.partialFailures.takeIf { it.isNotEmpty() }?.let { failures ->
            Text(
                "${failures.size} 个集群节点扫描失败，结果可能不完整。",
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (enabled && !state.loading && state.error == null && state.keys.isEmpty()) {
            Text(
                "没有匹配此模式的键。",
                modifier = Modifier.testTag("key_list_empty"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(modifier = Modifier.weight(1f).testTag("key_list")) {
            items(state.keys, key = { it.key }) { key ->
                val selected = state.selectedKey?.key == key.key
                KeyListRow(
                    key = key,
                    selected = selected,
                    onSelect = { onSelect(key) },
                )
            }
            if (state.nextCursorToken != null) {
                item {
                    TextButton(
                        onClick = onLoadMore,
                        enabled = state.canLoadMore,
                        modifier = Modifier.fillMaxWidth().testTag("load_more"),
                    ) {
                        Text("加载更多")
                    }
                }
            }
        }

        DatabaseSelector(
            databases = state.databases,
            selectedDatabase = state.selectedDatabase,
            clusterMode = state.clusterMode,
            loading = state.databasesLoading,
            enabled = enabled,
            onSelectDatabase = onSelectDatabase,
        )
    }
}

@Composable
private fun KeyListRow(
    key: RedisKeySummary,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("key_${key.key}"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(key.type.color()),
            )
            Column {
                Text(key.key, style = MaterialTheme.typography.bodyMedium)
                Text(
                    key.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatabaseSelector(
    databases: List<RedisDatabaseSummary>,
    selectedDatabase: Int,
    clusterMode: Boolean,
    loading: Boolean,
    enabled: Boolean,
    onSelectDatabase: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = databases.firstOrNull { it.index == selectedDatabase }
    val label = selected?.let { "db${it.index} (${it.keyCount})" } ?: "db$selectedDatabase"

    if (clusterMode) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { Text("Cluster 模式仅支持 db0") },
            state = rememberTooltipState(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("keys_database_dropdown"),
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("keys_database_dropdown"),
    ) {
        OutlinedTextField(
            value = if (loading) "加载中…" else label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled && !loading,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            databases.forEach { db ->
                DropdownMenuItem(
                    text = { Text("db${db.index} (${db.keyCount})") },
                    onClick = {
                        expanded = false
                        onSelectDatabase(db.index)
                    },
                    modifier = Modifier.testTag("keys_database_${db.index}"),
                )
            }
        }
    }
}
