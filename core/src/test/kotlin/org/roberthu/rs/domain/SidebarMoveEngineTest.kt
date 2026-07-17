package org.roberthu.rs.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SidebarMoveEngineTest {
    private val groupA = ConnectionGroup(id = "gA", name = "Group A", sortOrder = 2048L)
    private val groupB = ConnectionGroup(id = "gB", name = "Group B", sortOrder = 3072L)
    private val rootConn = ConnectionProfile(
        id = "cRoot",
        name = "New connection",
        mode = DeploymentMode.Standalone,
        host = "127.0.0.1",
        sortOrder = 1024L,
    )
    private val connA = ConnectionProfile(
        id = "cA",
        name = "Connection A",
        mode = DeploymentMode.Standalone,
        host = "127.0.0.1",
        groupId = "gA",
        sortOrder = 1024L,
    )
    private val connB = ConnectionProfile(
        id = "cB",
        name = "Connection B",
        mode = DeploymentMode.Standalone,
        host = "127.0.0.1",
        groupId = "gA",
        sortOrder = 2048L,
    )
    private val connC = ConnectionProfile(
        id = "cC",
        name = "Connection C",
        mode = DeploymentMode.Standalone,
        host = "127.0.0.1",
        groupId = "gB",
        sortOrder = 1024L,
    )

    @Test
    fun rootConnectionMovesAfterGroup() {
        val profiles = listOf(rootConn)
        val groups = listOf(groupA)
        val outcome = SidebarMoveEngine.applyMove(
            profiles,
            groups,
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Connection,
                itemId = "cRoot",
                targetParentGroupId = null,
                targetIndex = 0,
                placement = SidebarMovePlacement.After,
                referenceItemType = SidebarItemType.Group,
                referenceItemId = "gA",
            ),
        ).getOrThrow()

        assertFalse(outcome.noOp)
        val moved = outcome.profiles.first { it.id == "cRoot" }
        assertEquals(null, moved.groupId)
        assertTrue(moved.sortOrder > groupA.sortOrder)
    }

    @Test
    fun rootConnectionMovesBeforeGroup() {
        val profiles = listOf(
            rootConn.copy(sortOrder = 3072L),
        )
        val groups = listOf(groupA.copy(sortOrder = 1024L))
        val outcome = SidebarMoveEngine.applyMove(
            profiles,
            groups,
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Connection,
                itemId = "cRoot",
                targetParentGroupId = null,
                targetIndex = 0,
                placement = SidebarMovePlacement.Before,
                referenceItemType = SidebarItemType.Group,
                referenceItemId = "gA",
            ),
        ).getOrThrow()

        val moved = outcome.profiles.first { it.id == "cRoot" }
        assertTrue(moved.sortOrder < groupA.sortOrder)
    }

    @Test
    fun connectionMovesInsideGroup() {
        val outcome = SidebarMoveEngine.applyMove(
            listOf(rootConn, connC),
            listOf(groupA),
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Connection,
                itemId = "cRoot",
                targetParentGroupId = "gA",
                targetIndex = 1,
                placement = SidebarMovePlacement.Inside,
                referenceItemType = SidebarItemType.Group,
                referenceItemId = "gA",
            ),
        ).getOrThrow()

        val moved = outcome.profiles.first { it.id == "cRoot" }
        assertEquals("gA", moved.groupId)
        assertEquals("gA", outcome.expandGroupId)
    }

    @Test
    fun connectionMovesWithinGroup() {
        val profiles = listOf(connA, connB)
        val groups = listOf(groupA)
        val outcome = SidebarMoveEngine.applyMove(
            profiles,
            groups,
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Connection,
                itemId = "cB",
                targetParentGroupId = "gA",
                targetIndex = 0,
                placement = SidebarMovePlacement.Before,
                referenceItemType = SidebarItemType.Connection,
                referenceItemId = "cA",
            ),
        ).getOrThrow()

        val moved = outcome.profiles.first { it.id == "cB" }
        assertTrue(moved.sortOrder < connA.sortOrder)
    }

    @Test
    fun connectionMovesAcrossGroups() {
        val profiles = listOf(connA, connB, connC)
        val groups = listOf(groupA, groupB)
        val outcome = SidebarMoveEngine.applyMove(
            profiles,
            groups,
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Connection,
                itemId = "cB",
                targetParentGroupId = "gB",
                targetIndex = 0,
                placement = SidebarMovePlacement.Before,
                referenceItemType = SidebarItemType.Connection,
                referenceItemId = "cC",
            ),
        ).getOrThrow()

        val moved = outcome.profiles.first { it.id == "cB" }
        assertEquals("gB", moved.groupId)
        assertTrue(moved.sortOrder < connC.sortOrder)
    }

    @Test
    fun connectionMovesOutOfGroupToRoot() {
        val profiles = listOf(rootConn, connA)
        val groups = listOf(groupA)
        val outcome = SidebarMoveEngine.applyMove(
            profiles,
            groups,
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Connection,
                itemId = "cA",
                targetParentGroupId = null,
                targetIndex = 0,
                placement = SidebarMovePlacement.Before,
                referenceItemType = SidebarItemType.Connection,
                referenceItemId = "cRoot",
            ),
        ).getOrThrow()

        val moved = outcome.profiles.first { it.id == "cA" }
        assertEquals(null, moved.groupId)
        assertTrue(moved.sortOrder < rootConn.sortOrder)
    }

    @Test
    fun groupMixedSortAtRoot() {
        val profiles = listOf(rootConn)
        val groups = listOf(groupA.copy(sortOrder = 2048L), groupB.copy(sortOrder = 3072L))
        val outcome = SidebarMoveEngine.applyMove(
            profiles,
            groups,
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Group,
                itemId = "gB",
                targetParentGroupId = null,
                targetIndex = 0,
                placement = SidebarMovePlacement.Before,
                referenceItemType = SidebarItemType.Connection,
                referenceItemId = "cRoot",
            ),
        ).getOrThrow()

        val movedGroup = outcome.groups.first { it.id == "gB" }
        assertTrue(movedGroup.sortOrder < rootConn.sortOrder)
    }

    @Test
    fun samePositionDropIsNoOp() {
        val profiles = listOf(rootConn)
        val groups = listOf(groupA)
        val outcome = SidebarMoveEngine.applyMove(
            profiles,
            groups,
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Connection,
                itemId = "cRoot",
                targetParentGroupId = null,
                targetIndex = 0,
                placement = SidebarMovePlacement.Before,
                referenceItemType = SidebarItemType.Connection,
                referenceItemId = "cRoot",
            ),
        ).getOrThrow()

        assertTrue(outcome.noOp)
    }

    @Test
    fun groupCannotMoveInsideAnotherGroup() {
        val result = SidebarMoveEngine.applyMove(
            emptyList(),
            listOf(groupA, groupB),
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Group,
                itemId = "gB",
                targetParentGroupId = "gA",
                targetIndex = 0,
                placement = SidebarMovePlacement.Inside,
                referenceItemType = SidebarItemType.Group,
                referenceItemId = "gA",
            ),
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun migrationPreservesAlphabeticalRootOrder() {
        val profiles = listOf(
            ConnectionProfile(id = "c1", name = "Zeta", mode = DeploymentMode.Standalone, host = "h"),
            ConnectionProfile(id = "c2", name = "Alpha", mode = DeploymentMode.Standalone, host = "h"),
        )
        val groups = listOf(
            ConnectionGroup(id = "g1", name = "Middle", sortOrder = 0L),
        )
        val (migratedProfiles, migratedGroups) = SidebarSortMigration.migrate(profiles, groups)
        val entries = SidebarTreeBuilder.rootEntries(migratedProfiles, migratedGroups)
        assertEquals(listOf("c2", "g1", "c1"), entries.map { it.id })
        assertTrue(migratedProfiles.all { it.sortOrder > 0L })
        assertTrue(migratedGroups.all { it.sortOrder > 0L })
    }
}
