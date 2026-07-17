package org.roberthu.rs.presentation

import org.roberthu.rs.domain.MoveSidebarItemRequest
import org.roberthu.rs.domain.SidebarItemType
import org.roberthu.rs.domain.SidebarMovePlacement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SidebarDragDropResolverTest {
    @Test
    fun groupInsideTargetIsInvalidForGroupDrag() {
        val dragging = SidebarDragItem(SidebarItemType.Group, "g1")
        val target = SidebarHoverTarget(
            itemType = SidebarItemType.Group,
            itemId = "g2",
            zone = SidebarDropZone.Inside,
        )
        assertFalse(SidebarDragDropResolver.isValidTarget(dragging, target))
        assertNull(SidebarDragDropResolver.toMoveRequest(dragging, target))
    }

    @Test
    fun connectionInsideGroupProducesInsideRequest() {
        val dragging = SidebarDragItem(SidebarItemType.Connection, "c1")
        val target = SidebarHoverTarget(
            itemType = SidebarItemType.Group,
            itemId = "g1",
            zone = SidebarDropZone.Inside,
            insideIndex = 2,
        )
        val request = assertNotNull(SidebarDragDropResolver.toMoveRequest(dragging, target))
        assertEquals(SidebarMovePlacement.Inside, request.placement)
        assertEquals("g1", request.referenceItemId)
        assertEquals(2, request.targetIndex)
    }

    @Test
    fun connectionBeforeRootItemStaysAtRoot() {
        val dragging = SidebarDragItem(SidebarItemType.Connection, "c1", parentGroupId = "g1")
        val target = SidebarHoverTarget(
            itemType = SidebarItemType.Group,
            itemId = "g2",
            zone = SidebarDropZone.Before,
        )
        val request = assertNotNull(SidebarDragDropResolver.toMoveRequest(dragging, target))
        assertEquals(SidebarMovePlacement.Before, request.placement)
        assertNull(request.targetParentGroupId)
    }

    @Test
    fun detectGroupZoneUsesQuarterSplit() {
        assertEquals(SidebarDropZone.Before, SidebarDragDropResolver.detectGroupZone(10f, 100f))
        assertEquals(SidebarDropZone.Inside, SidebarDragDropResolver.detectGroupZone(50f, 100f))
        assertEquals(SidebarDropZone.After, SidebarDragDropResolver.detectGroupZone(80f, 100f))
    }

    @Test
    fun detectConnectionZoneUsesHalfSplit() {
        assertEquals(SidebarDropZone.Before, SidebarDragDropResolver.detectConnectionZone(10f, 100f))
        assertEquals(SidebarDropZone.After, SidebarDragDropResolver.detectConnectionZone(60f, 100f))
    }
}
