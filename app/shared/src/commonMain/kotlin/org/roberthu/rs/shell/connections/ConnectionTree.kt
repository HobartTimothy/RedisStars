package org.roberthu.rs.shell.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.MoveSidebarItemRequest
import org.roberthu.rs.domain.SidebarItemType
import org.roberthu.rs.domain.SidebarRootEntry
import org.roberthu.rs.domain.SidebarTreeBuilder
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.presentation.SidebarDragDropResolver
import org.roberthu.rs.presentation.SidebarDragItem
import org.roberthu.rs.presentation.SidebarDropZone
import org.roberthu.rs.presentation.SidebarHoverTarget
import kotlin.math.roundToInt

internal data class SidebarLayoutEntry(
    val itemType: SidebarItemType,
    val itemId: String,
    val parentGroupId: String? = null,
    val isGroupContent: Boolean = false,
    val insideIndex: Int? = null,
    val bounds: Rect = Rect.Zero,
)

internal class SidebarLayoutRegistry {
    private val entries = linkedMapOf<String, SidebarLayoutEntry>()

    fun update(key: String, entry: SidebarLayoutEntry) {
        entries[key] = entry
    }

    fun remove(key: String) {
        entries.remove(key)
    }

    fun resolve(globalOffset: Offset, dragging: SidebarDragItem?): SidebarHoverTarget? {
        if (dragging == null) return null
        val hit = entries.values.firstOrNull { entry ->
            entry.bounds.contains(globalOffset)
        } ?: return null

        if (hit.isGroupContent) {
            return SidebarHoverTarget(
                itemType = SidebarItemType.Group,
                itemId = hit.itemId,
                zone = SidebarDropZone.Inside,
                insideIndex = hit.insideIndex,
            )
        }

        val relativeY = globalOffset.y - hit.bounds.top
        val height = hit.bounds.height
        val zone = when (hit.itemType) {
            SidebarItemType.Group ->
                SidebarDragDropResolver.detectGroupZone(relativeY, height)
            SidebarItemType.Connection ->
                SidebarDragDropResolver.detectConnectionZone(relativeY, height)
        }
        return SidebarHoverTarget(
            itemType = hit.itemType,
            itemId = hit.itemId,
            zone = zone,
            parentGroupId = hit.parentGroupId,
        )
    }
}

@Composable
internal fun ConnectionTree(
    profiles: List<ConnectionProfile>,
    groups: List<ConnectionGroup>,
    selectedProfileId: String?,
    selectedGroupId: String?,
    dragEnabled: Boolean,
    onSelect: (ConnectionProfile) -> Unit,
    onToggleGroupExpanded: (String) -> Unit,
    onSelectGroup: (String?) -> Unit,
    onConnect: (ConnectionProfile) -> Unit,
    onTest: (ConnectionProfile) -> Unit,
    onEdit: (ConnectionProfile) -> Unit,
    onDelete: (ConnectionProfile) -> Unit,
    onMoveSidebarItem: (MoveSidebarItemRequest) -> Unit,
    onOpenGroupContextMenu: (Offset, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val registry = remember { SidebarLayoutRegistry() }
    var dragging by remember { mutableStateOf<SidebarDragItem?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var hoverTarget by remember { mutableStateOf<SidebarHoverTarget?>(null) }

    val rootEntries = remember(profiles, groups) {
        SidebarTreeBuilder.rootEntries(profiles, groups)
    }

    fun finishDrag(request: MoveSidebarItemRequest?) {
        if (request != null) {
            onMoveSidebarItem(request)
        }
        dragging = null
        hoverTarget = null
    }

    Box(modifier = modifier) {
        LazyColumn {
            rootEntries.forEach { entry ->
                when (entry) {
                    is SidebarRootEntry.UngroupedConnection -> {
                        item(key = "root-${entry.profile.id}") {
                            DraggableConnectionRow(
                                profile = entry.profile,
                                selected = entry.profile.id == selectedProfileId,
                                indented = false,
                                dragEnabled = dragEnabled,
                                dragging = dragging?.itemId == entry.profile.id,
                                dropZone = hoverTargetForConnection(hoverTarget, entry.profile.id, null),
                                registry = registry,
                                layoutKey = "root-conn-${entry.profile.id}",
                                parentGroupId = null,
                                onSelect = onSelect,
                                onConnect = onConnect,
                                onTest = onTest,
                                onEdit = onEdit,
                                onDelete = onDelete,
                                onDragStart = {
                                    dragging = SidebarDragItem(SidebarItemType.Connection, entry.profile.id)
                                },
                                onDrag = { position ->
                                    dragPosition = position
                                    hoverTarget = registry.resolve(position, dragging)
                                },
                                onDragEnd = {
                                    val request = dragging?.let { item ->
                                        hoverTarget?.let { target ->
                                            SidebarDragDropResolver.toMoveRequest(item, target)
                                        }
                                    }
                                    finishDrag(request)
                                },
                                onDragCancel = { finishDrag(null) },
                            )
                        }
                    }
                    is SidebarRootEntry.GroupHeader -> {
                        val group = entry.group
                        val groupProfiles = SidebarTreeBuilder.profilesInGroup(profiles, group.id)
                        item(key = "group-header-${group.id}") {
                            DraggableGroupHeaderRow(
                                group = group,
                                count = groupProfiles.size,
                                selected = group.id == selectedGroupId,
                                dragEnabled = dragEnabled,
                                dragging = dragging?.itemId == group.id,
                                dropZone = hoverTargetForGroup(hoverTarget, group.id),
                                highlightInside = hoverTarget?.itemId == group.id &&
                                    hoverTarget?.zone == SidebarDropZone.Inside,
                                registry = registry,
                                layoutKey = "group-header-${group.id}",
                                onToggle = { onToggleGroupExpanded(group.id) },
                                onSelect = { onSelectGroup(group.id) },
                                onContextMenu = { offset ->
                                    onOpenGroupContextMenu(offset, group.id)
                                },
                                onDragStart = {
                                    dragging = SidebarDragItem(SidebarItemType.Group, group.id)
                                },
                                onDrag = { position ->
                                    dragPosition = position
                                    hoverTarget = registry.resolve(position, dragging)
                                },
                                onDragEnd = {
                                    val request = dragging?.let { item ->
                                        hoverTarget?.let { target ->
                                            SidebarDragDropResolver.toMoveRequest(item, target)
                                        }
                                    }
                                    finishDrag(request)
                                },
                                onDragCancel = { finishDrag(null) },
                                modifier = Modifier.testTag("connections_group_${group.id}"),
                            )
                        }
                        if (group.expanded) {
                            item(key = "group-content-${group.id}") {
                                GroupContentDropZone(
                                    groupId = group.id,
                                    profileCount = groupProfiles.size,
                                    highlightInside = hoverTarget?.itemId == group.id &&
                                        hoverTarget?.zone == SidebarDropZone.Inside,
                                    registry = registry,
                                    layoutKey = "group-content-${group.id}",
                                )
                            }
                            items(groupProfiles, key = { "group-${group.id}-${it.id}" }) { profile ->
                                DraggableConnectionRow(
                                    profile = profile,
                                    selected = profile.id == selectedProfileId,
                                    indented = true,
                                    dragEnabled = dragEnabled,
                                    dragging = dragging?.itemId == profile.id,
                                    dropZone = hoverTargetForConnection(hoverTarget, profile.id, group.id),
                                    registry = registry,
                                    layoutKey = "group-conn-${group.id}-${profile.id}",
                                    parentGroupId = group.id,
                                    onSelect = onSelect,
                                    onConnect = onConnect,
                                    onTest = onTest,
                                    onEdit = onEdit,
                                    onDelete = onDelete,
                                    onDragStart = {
                                        dragging = SidebarDragItem(
                                            SidebarItemType.Connection,
                                            profile.id,
                                            parentGroupId = group.id,
                                        )
                                    },
                                    onDrag = { position ->
                                        dragPosition = position
                                        hoverTarget = registry.resolve(position, dragging)
                                    },
                                    onDragEnd = {
                                        val request = dragging?.let { item ->
                                            hoverTarget?.let { target ->
                                                SidebarDragDropResolver.toMoveRequest(item, target)
                                            }
                                        }
                                        finishDrag(request)
                                    },
                                    onDragCancel = { finishDrag(null) },
                                )
                            }
                        }
                    }
                }
            }
        }

        DragOverlay(
            dragging = dragging,
            profiles = profiles,
            groups = groups,
            dragPosition = dragPosition,
        )
    }
}

@Composable
private fun GroupContentDropZone(
    groupId: String,
    profileCount: Int,
    highlightInside: Boolean,
    registry: SidebarLayoutRegistry,
    layoutKey: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (profileCount == 0) 28.dp else 4.dp)
            .padding(start = 16.dp)
            .onGloballyPositioned { coordinates ->
                registry.update(
                    layoutKey,
                    SidebarLayoutEntry(
                        itemType = SidebarItemType.Group,
                        itemId = groupId,
                        isGroupContent = true,
                        insideIndex = profileCount,
                        bounds = coordinates.boundsInRoot(),
                    ),
                )
            },
    ) {
        if (highlightInside && profileCount == 0) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    t(StringKeys.Connections.MoveToGroup),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.DragOverlay(
    dragging: SidebarDragItem?,
    profiles: List<ConnectionProfile>,
    groups: List<ConnectionGroup>,
    dragPosition: Offset,
) {
    val draggingLabel = dragging?.let { item ->
        when (item.itemType) {
            SidebarItemType.Connection -> profiles.firstOrNull { it.id == item.itemId }?.name
            SidebarItemType.Group -> groups.firstOrNull { it.id == item.itemId }?.name
        }
    } ?: return
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset {
                IntOffset(
                    dragPosition.x.roundToInt(),
                    dragPosition.y.roundToInt(),
                )
            }
            .graphicsLayer { alpha = 0.88f },
    ) {
        Text(
            draggingLabel,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun hoverTargetForConnection(
    hover: SidebarHoverTarget?,
    itemId: String,
    parentGroupId: String?,
): SidebarDropZone? {
    if (hover == null || hover.itemId != itemId || hover.parentGroupId != parentGroupId) return null
    return when (hover.zone) {
        SidebarDropZone.Before, SidebarDropZone.After -> hover.zone
        SidebarDropZone.Inside -> null
    }
}

private fun hoverTargetForGroup(
    hover: SidebarHoverTarget?,
    groupId: String,
): SidebarDropZone? {
    if (hover == null || hover.itemId != groupId) return null
    return when (hover.zone) {
        SidebarDropZone.Before, SidebarDropZone.After -> hover.zone
        SidebarDropZone.Inside -> null
    }
}

@Composable
private fun DraggableGroupHeaderRow(
    group: ConnectionGroup,
    count: Int,
    selected: Boolean,
    dragEnabled: Boolean,
    dragging: Boolean,
    dropZone: SidebarDropZone?,
    highlightInside: Boolean,
    registry: SidebarLayoutRegistry,
    layoutKey: String,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onContextMenu: (Offset) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                registry.update(
                    layoutKey,
                    SidebarLayoutEntry(
                        itemType = SidebarItemType.Group,
                        itemId = group.id,
                        bounds = coordinates.boundsInRoot(),
                    ),
                )
            },
    ) {
        GroupHeaderRow(
            group = group,
            count = count,
            selected = selected,
            dragEnabled = dragEnabled,
            dragging = dragging,
            highlightInside = highlightInside,
            onToggle = onToggle,
            onSelect = onSelect,
            onContextMenu = onContextMenu,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
        )
        DropIndicatorLine(dropZone)
    }
}

@Composable
private fun DraggableConnectionRow(
    profile: ConnectionProfile,
    selected: Boolean,
    indented: Boolean,
    dragEnabled: Boolean,
    dragging: Boolean,
    dropZone: SidebarDropZone?,
    registry: SidebarLayoutRegistry,
    layoutKey: String,
    parentGroupId: String?,
    onSelect: (ConnectionProfile) -> Unit,
    onConnect: (ConnectionProfile) -> Unit,
    onTest: (ConnectionProfile) -> Unit,
    onEdit: (ConnectionProfile) -> Unit,
    onDelete: (ConnectionProfile) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Box(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            registry.update(
                layoutKey,
                SidebarLayoutEntry(
                    itemType = SidebarItemType.Connection,
                    itemId = profile.id,
                    parentGroupId = parentGroupId,
                    bounds = coordinates.boundsInRoot(),
                ),
            )
        },
    ) {
        ConnectionRow(
            profile = profile,
            selected = selected,
            indented = indented,
            dragEnabled = dragEnabled,
            dragging = dragging,
            onSelect = onSelect,
            onConnect = onConnect,
            onTest = onTest,
            onEdit = onEdit,
            onDelete = onDelete,
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
        )
        DropIndicatorLine(dropZone)
    }
}

@Composable
private fun BoxScope.DropIndicatorLine(zone: SidebarDropZone?) {
    if (zone == null) return
    Box(
        modifier = Modifier
            .align(if (zone == SidebarDropZone.Before) Alignment.TopCenter else Alignment.BottomCenter)
            .fillMaxWidth()
            .height(2.dp)
            .padding(horizontal = 4.dp)
            .background(MaterialTheme.colorScheme.primary),
    )
}

/**
 * Enables drag-to-reorder on a whole sidebar row after a long press, so the sidebar no
 * longer needs a dedicated drag handle. Drag positions are reported in root coordinates
 * so the caller can resolve drop targets against the shared layout registry.
 */
internal fun Modifier.sidebarLongPressDrag(
    enabled: Boolean,
    dragKey: Any?,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
): Modifier = composed {
    if (!enabled) {
        return@composed this
    }
    val coordinatesHolder = remember { LayoutCoordinatesHolder() }
    this
        .onGloballyPositioned { coordinatesHolder.coordinates = it }
        .pointerInput(dragKey) {
            detectDragGesturesAfterLongPress(
                onDragStart = { onDragStart() },
                onDrag = { change, _ ->
                    change.consume()
                    val rootPosition = coordinatesHolder.coordinates
                        ?.localToRoot(change.position)
                        ?: change.position
                    onDrag(rootPosition)
                },
                onDragEnd = { onDragEnd() },
                onDragCancel = { onDragCancel() },
            )
        }
}

private class LayoutCoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}
