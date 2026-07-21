package org.roberthu.rs.shell.keys

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import org.roberthu.rs.shell.icons.AppIcons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.KeyListViewMode
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.presentation.KeyBrowserUiState
import org.roberthu.rs.theme.color
import org.roberthu.rs.ui.components.RedisEmptyState
import org.roberthu.rs.ui.components.RedisErrorState
import org.roberthu.rs.ui.components.RedisIconButtonStyle
import org.roberthu.rs.ui.components.RedisNotConnectedState
import org.roberthu.rs.ui.components.RedisTooltipIconButton
import org.roberthu.rs.ui.components.RedisWarningBanner
import org.roberthu.rs.ui.theme.RedisTheme

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
    onTypeFilterChange: (RedisKeyType?) -> Unit,
    onKeyListViewChange: (KeyListViewMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dim = RedisTheme.dimensions
    val spacing = RedisTheme.spacing
    val colors = RedisTheme.colors
    val shapes = RedisTheme.shapes
    val toolbarHeight = dim.iconButtonSize
    val fieldTextStyle = MaterialTheme.typography.bodySmall
    val borderColor = colors.divider
    val showEmptyState = enabled && !state.loading && state.error == null && state.keys.isEmpty()
    Column(
        modifier = modifier
            .testTag("key_browser_pane")
            .widthIn(min = dim.keyBrowserPaneMinWidth)
            .width(dim.keyBrowserPaneDefaultWidth)
            .fillMaxHeight()
            .padding(horizontal = spacing.sm, vertical = spacing.toolbarGap),
        verticalArrangement = Arrangement.spacedBy(spacing.toolbarGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.toolbarGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KeyFilterField(
                pattern = state.pattern,
                enabled = enabled && !state.loading,
                typeFilter = state.typeFilter,
                fieldHeight = toolbarHeight,
                textStyle = fieldTextStyle,
                borderColor = borderColor,
                onPatternChange = onPatternChange,
                onTypeFilterChange = onTypeFilterChange,
                onSearch = onRefresh,
                modifier = Modifier.weight(1f),
            )
            RedisTooltipIconButton(
                tooltip = t(StringKeys.Keys.Search),
                imageVector = AppIcons.Search,
                enabled = enabled && !state.loading,
                onClick = onRefresh,
                testTag = "keys_search",
                iconSize = dim.iconSize,
                buttonSize = toolbarHeight,
                style = RedisIconButtonStyle.Outlined,
            )
            RedisTooltipIconButton(
                tooltip = t(StringKeys.Keys.Refresh),
                imageVector = Icons.Default.Refresh,
                enabled = enabled && !state.loading,
                onClick = onRefresh,
                testTag = "keys_refresh",
                iconSize = dim.iconSize,
                buttonSize = toolbarHeight,
                style = RedisIconButtonStyle.Outlined,
            )
            RedisTooltipIconButton(
                tooltip = t(StringKeys.Keys.Add),
                imageVector = Icons.Default.Add,
                enabled = enabled,
                onClick = onOpenAddKey,
                testTag = "keys_add",
                iconSize = dim.iconSize,
                buttonSize = toolbarHeight,
                style = RedisIconButtonStyle.Outlined,
            )
        }

        if (state.loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(dim.iconSize)
                        .testTag("key_scan_loading"),
                )
                TextButton(onClick = onCancel) {
                    Text(t(StringKeys.Keys.CancelScan), style = fieldTextStyle)
                }
            }
        }

        if (!enabled) {
            RedisNotConnectedState(
                message = t(StringKeys.Keys.NotConnected),
                testTag = "keys_not_connected",
            )
        }
        state.error?.let {
            RedisErrorState(
                message = it,
                hint = t(StringKeys.Keys.ScanErrorHint),
                testTag = "key_scan_error",
            )
        }
        state.partialFailures.takeIf { it.isNotEmpty() }?.let { failures ->
            RedisWarningBanner(
                message = t(StringKeys.Keys.ClusterPartial, failures.size),
                testTag = "key_scan_partial",
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (showEmptyState) {
                RedisEmptyState(
                    message = t(StringKeys.Keys.EmptyTitle),
                    modifier = Modifier.align(Alignment.Center),
                    testTag = "key_list_empty",
                )
            }
            val keyTree = remember(state.keys, state.keySeparator, state.keyListView) {
                if (state.keyListView == KeyListViewMode.Tree) {
                    buildKeyTree(state.keys, state.keySeparator)
                } else {
                    emptyList()
                }
            }
            if (!showEmptyState) {
                LazyColumn(modifier = Modifier.fillMaxSize().testTag("key_list")) {
                    if (state.keyListView == KeyListViewMode.Tree) {
                        item(key = "key_tree") {
                            KeyTreeItems(
                                nodes = keyTree,
                                selectedKey = state.selectedKey,
                                onSelect = onSelect,
                                depth = 0,
                            )
                        }
                    } else {
                        items(state.keys, key = { it.key }) { key ->
                            val selected = state.selectedKey?.key == key.key
                            KeyListRow(
                                key = key,
                                selected = selected,
                                onSelect = { onSelect(key) },
                            )
                        }
                    }
                    if (state.nextCursorToken != null) {
                        item {
                            TextButton(
                                onClick = onLoadMore,
                                enabled = state.canLoadMore,
                                modifier = Modifier.fillMaxWidth().testTag("load_more"),
                            ) {
                                Text(t(StringKeys.Keys.LoadMore), style = fieldTextStyle)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.toolbarGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DatabaseSelector(
                databases = state.databases,
                selectedDatabase = state.selectedDatabase,
                loadedKeyCount = state.keys.size,
                clusterMode = state.clusterMode,
                loading = state.databasesLoading,
                enabled = enabled,
                onSelectDatabase = onSelectDatabase,
                fieldHeight = toolbarHeight,
                fieldTextStyle = fieldTextStyle,
                borderColor = borderColor,
                modifier = Modifier.weight(1f),
            )
            val viewToggleLabel = if (state.keyListView == KeyListViewMode.Tree) {
                t(StringKeys.Keys.ViewFlat)
            } else {
                t(StringKeys.Keys.ViewTree)
            }
            val viewToggleIcon = if (state.keyListView == KeyListViewMode.Tree) {
                AppIcons.AccountTree
            } else {
                Icons.Default.List
            }
            RedisTooltipIconButton(
                tooltip = viewToggleLabel,
                imageVector = viewToggleIcon,
                enabled = enabled,
                onClick = {
                    onKeyListViewChange(
                        if (state.keyListView == KeyListViewMode.Tree) KeyListViewMode.Flat else KeyListViewMode.Tree,
                    )
                },
                testTag = "keys_view_toggle",
                iconSize = dim.iconSize,
                buttonSize = toolbarHeight,
                style = RedisIconButtonStyle.Outlined,
            )
        }
    }
}

@Composable
private fun KeyFilterField(
    pattern: String,
    enabled: Boolean,
    typeFilter: RedisKeyType?,
    fieldHeight: Dp,
    textStyle: TextStyle,
    borderColor: Color,
    onPatternChange: (String) -> Unit,
    onTypeFilterChange: (RedisKeyType?) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeBadgeSize = fieldHeight - 8.dp
    Surface(
        modifier = modifier
            .height(fieldHeight)
            .testTag("key_pattern"),
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KeyTypeFilterButton(
                selectedType = typeFilter,
                enabled = enabled,
                onTypeFilterChange = onTypeFilterChange,
                buttonSize = typeBadgeSize,
                embedded = true,
            )
            BasicTextField(
                value = pattern,
                onValueChange = onPatternChange,
                enabled = enabled,
                singleLine = true,
                textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            onSearch()
                            true
                        } else {
                            false
                        }
                    },
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (pattern.isEmpty()) {
                            Text(
                                t(StringKeys.Keys.FilterPlaceholder),
                                style = textStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (pattern.isNotEmpty()) {
                IconButton(
                    onClick = { onPatternChange("") },
                    modifier = Modifier.size(fieldHeight),
                    enabled = enabled,
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = t(StringKeys.Keys.ClearPattern),
                        modifier = Modifier.size(RedisTheme.dimensions.iconSizeSm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyTreeItems(
    nodes: List<KeyTreeNode>,
    selectedKey: RedisKeySummary?,
    onSelect: (RedisKeySummary) -> Unit,
    depth: Int,
) {
    nodes.forEach { node ->
        when (node) {
            is KeyTreeNode.Folder -> {
                var expanded by remember(node.segment, depth) { mutableStateOf(true) }
                KeyFolderRow(
                    segment = node.segment,
                    depth = depth,
                    expanded = expanded,
                    onToggle = { expanded = !expanded },
                )
                if (expanded) {
                    KeyTreeItems(
                        nodes = node.children,
                        selectedKey = selectedKey,
                        onSelect = onSelect,
                        depth = depth + 1,
                    )
                }
            }
            is KeyTreeNode.Leaf -> {
                val selected = selectedKey?.key == node.key.key
                KeyListRow(
                    key = node.key,
                    selected = selected,
                    depth = depth,
                    onSelect = { onSelect(node.key) },
                )
            }
        }
    }
}

@Composable
private fun KeyFolderRow(
    segment: String,
    depth: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = (depth * 12).dp)
            .testTag("key_folder_$segment"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                if (expanded) "▾" else "▸",
                style = MaterialTheme.typography.labelSmall,
            )
            Text(segment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun KeyTypeFilterButton(
    selectedType: RedisKeyType?,
    enabled: Boolean,
    onTypeFilterChange: (RedisKeyType?) -> Unit,
    buttonSize: Dp,
    embedded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val badgeLetter = selectedType?.filterBadgeLetter()?.uppercaseChar()?.toString()
        ?: ALL_TYPE_FILTER_BADGE_LETTER.toString()
    val badgeBackground = selectedType?.color() ?: MaterialTheme.colorScheme.inverseSurface
    val badgeContent = if (selectedType == null) {
        MaterialTheme.colorScheme.inverseOnSurface
    } else {
        Color.White
    }

    Box {
        val badgeShape = if (embedded) RoundedCornerShape(4.dp) else RoundedCornerShape(8.dp)
        val filterModifier = Modifier
            .size(buttonSize)
            .testTag("keys_type_filter")
        val badgeContentComposable = @Composable {
            Surface(
                onClick = { if (enabled) expanded = true },
                enabled = enabled,
                shape = badgeShape,
                color = badgeBackground,
                modifier = filterModifier,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        badgeLetter,
                        style = if (embedded) {
                            MaterialTheme.typography.labelSmall
                        } else {
                            MaterialTheme.typography.labelMedium
                        },
                        color = badgeContent,
                    )
                }
            }
        }
        if (embedded) {
            badgeContentComposable()
        } else {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { Text(t(StringKeys.Keys.TypeFilterTooltip)) },
                state = rememberTooltipState(),
            ) {
                badgeContentComposable()
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    KeyTypeFilterMenuRow(
                        label = t(StringKeys.Keys.TypeFilterAll),
                        badgeLetter = ALL_TYPE_FILTER_BADGE_LETTER.toString(),
                        badgeBackground = MaterialTheme.colorScheme.inverseSurface,
                        badgeContent = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                },
                onClick = {
                    expanded = false
                    onTypeFilterChange(null)
                },
                modifier = Modifier.testTag("keys_type_filter_all"),
            )
            filterableKeyTypes.forEach { type ->
                DropdownMenuItem(
                    text = {
                        KeyTypeFilterMenuRow(
                            label = type.name,
                            badgeLetter = type.filterBadgeLetter().uppercaseChar().toString(),
                            badgeBackground = type.color(),
                            badgeContent = Color.White,
                        )
                    },
                    onClick = {
                        expanded = false
                        onTypeFilterChange(type)
                    },
                    modifier = Modifier.testTag("keys_type_filter_${type.name.lowercase()}"),
                )
            }
        }
    }
}

@Composable
private fun KeyTypeFilterMenuRow(
    label: String,
    badgeLetter: String,
    badgeBackground: Color,
    badgeContent: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeBackground,
            modifier = Modifier.size(20.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    badgeLetter,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeContent,
                )
            }
        }
        Text(label)
    }
}

@Composable
private fun KeyListRow(
    key: RedisKeySummary,
    selected: Boolean,
    onSelect: () -> Unit,
    depth: Int = 0,
) {
    val dim = RedisTheme.dimensions
    val colors = RedisTheme.colors
    val spacing = RedisTheme.spacing
    Surface(
        color = if (selected) colors.selectedSurface else colors.paneSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp)
            .clickable(onClick = onSelect)
            .testTag("key_${key.key}"),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = spacing.listItemPaddingHorizontal,
                vertical = spacing.listItemPaddingVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(dim.statusDotSize)
                    .clip(CircleShape)
                    .background(key.type.color()),
            )
            Column {
                Text(key.key, style = RedisTheme.typography.keyName, color = colors.textPrimary)
                Text(
                    key.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
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
    loadedKeyCount: Int,
    clusterMode: Boolean,
    loading: Boolean,
    enabled: Boolean,
    onSelectDatabase: (Int) -> Unit,
    fieldHeight: Dp,
    fieldTextStyle: TextStyle,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = databases.firstOrNull { it.index == selectedDatabase }
    val label = when {
        loading -> t(StringKeys.Keys.DatabasesLoading)
        selected != null -> t(
            StringKeys.Keys.DatabaseSelectionFormat,
            selected.index,
            loadedKeyCount,
            selected.keyCount,
        )
        else -> "db$selectedDatabase ($loadedKeyCount/0)"
    }

    if (clusterMode) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { Text(t(StringKeys.Keys.ClusterDbTooltip)) },
            state = rememberTooltipState(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(6.dp),
                modifier = modifier
                    .height(fieldHeight)
                    .testTag("keys_database_dropdown"),
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    style = fieldTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier.testTag("keys_database_dropdown"),
    ) {
        Surface(
            onClick = { if (enabled && !loading) expanded = true },
            enabled = enabled && !loading,
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(fieldHeight),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = fieldTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            databases.forEach { db ->
                DropdownMenuItem(
                    text = { Text(t(StringKeys.Keys.DatabaseFormat, db.index, db.keyCount)) },
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
