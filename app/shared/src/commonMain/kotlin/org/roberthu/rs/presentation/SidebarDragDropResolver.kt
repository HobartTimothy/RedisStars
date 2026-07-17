package org.roberthu.rs.presentation

import org.roberthu.rs.domain.MoveSidebarItemRequest
import org.roberthu.rs.domain.SidebarItemType
import org.roberthu.rs.domain.SidebarMovePlacement

enum class SidebarDropZone {
    Before,
    After,
    Inside,
}

data class SidebarDragItem(
    val itemType: SidebarItemType,
    val itemId: String,
    val parentGroupId: String? = null,
)

data class SidebarHoverTarget(
    val itemType: SidebarItemType,
    val itemId: String,
    val zone: SidebarDropZone,
    val parentGroupId: String? = null,
    val insideIndex: Int? = null,
)

object SidebarDragDropResolver {
    fun detectGroupZone(relativeY: Float, height: Float): SidebarDropZone {
        if (height <= 0f) return SidebarDropZone.Inside
        val ratio = relativeY / height
        return when {
            ratio < 0.25f -> SidebarDropZone.Before
            ratio > 0.75f -> SidebarDropZone.After
            else -> SidebarDropZone.Inside
        }
    }

    fun detectConnectionZone(relativeY: Float, height: Float): SidebarDropZone =
        if (height <= 0f || relativeY / height < 0.5f) {
            SidebarDropZone.Before
        } else {
            SidebarDropZone.After
        }

    fun isValidTarget(
        dragging: SidebarDragItem,
        target: SidebarHoverTarget,
    ): Boolean {
        if (dragging.itemType == SidebarItemType.Group && target.zone == SidebarDropZone.Inside) {
            return false
        }
        if (dragging.itemType == SidebarItemType.Group && target.parentGroupId != null) {
            return false
        }
        if (dragging.itemId == target.itemId &&
            dragging.itemType == target.itemType &&
            target.zone != SidebarDropZone.Inside
        ) {
            return false
        }
        return true
    }

    fun toMoveRequest(
        dragging: SidebarDragItem,
        target: SidebarHoverTarget,
    ): MoveSidebarItemRequest? {
        if (!isValidTarget(dragging, target)) return null

        return when (dragging.itemType) {
            SidebarItemType.Group -> toGroupMoveRequest(dragging, target)
            SidebarItemType.Connection -> toConnectionMoveRequest(dragging, target)
        }
    }

    private fun toGroupMoveRequest(
        dragging: SidebarDragItem,
        target: SidebarHoverTarget,
    ): MoveSidebarItemRequest? = when (target.zone) {
        SidebarDropZone.Inside -> null
        SidebarDropZone.Before -> MoveSidebarItemRequest(
            itemType = SidebarItemType.Group,
            itemId = dragging.itemId,
            targetParentGroupId = null,
            targetIndex = 0,
            placement = SidebarMovePlacement.Before,
            referenceItemType = target.itemType,
            referenceItemId = target.itemId,
        )
        SidebarDropZone.After -> MoveSidebarItemRequest(
            itemType = SidebarItemType.Group,
            itemId = dragging.itemId,
            targetParentGroupId = null,
            targetIndex = 0,
            placement = SidebarMovePlacement.After,
            referenceItemType = target.itemType,
            referenceItemId = target.itemId,
        )
    }

    private fun toConnectionMoveRequest(
        dragging: SidebarDragItem,
        target: SidebarHoverTarget,
    ): MoveSidebarItemRequest = when (target.zone) {
        SidebarDropZone.Inside -> MoveSidebarItemRequest(
            itemType = SidebarItemType.Connection,
            itemId = dragging.itemId,
            targetParentGroupId = target.itemId,
            targetIndex = target.insideIndex ?: Int.MAX_VALUE,
            placement = SidebarMovePlacement.Inside,
            referenceItemType = SidebarItemType.Group,
            referenceItemId = target.itemId,
        )
        SidebarDropZone.Before -> connectionSiblingMove(dragging, target, SidebarMovePlacement.Before)
        SidebarDropZone.After -> connectionSiblingMove(dragging, target, SidebarMovePlacement.After)
    }

    private fun connectionSiblingMove(
        dragging: SidebarDragItem,
        target: SidebarHoverTarget,
        placement: SidebarMovePlacement,
    ): MoveSidebarItemRequest {
        val parentGroupId = when (target.itemType) {
            SidebarItemType.Connection -> target.parentGroupId
            SidebarItemType.Group -> null
        }
        return MoveSidebarItemRequest(
            itemType = SidebarItemType.Connection,
            itemId = dragging.itemId,
            targetParentGroupId = parentGroupId,
            targetIndex = 0,
            placement = placement,
            referenceItemType = target.itemType,
            referenceItemId = target.itemId,
        )
    }
}
