package org.roberthu.rs.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.MoveSidebarItemRequest
import org.roberthu.rs.domain.SidebarItemType
import org.roberthu.rs.domain.SidebarMovePlacement
import org.roberthu.rs.domain.SidebarTreeBuilder
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.port.RedisConnectionPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManageConnectionsSidebarMoveTest {
    @Test
    fun moveSidebarItem_persistsAtomically() = runTest {
        val store = InMemoryProfileStore()
        val useCase = ManageConnections(store, RecordingConnectionPort())
        val root = profile("c1", "Alpha", sortOrder = 1024L)
        val group = ConnectionGroup(id = "g1", name = "Dev", sortOrder = 2048L)
        store.seedProfile(root)
        store.seedGroup(group)

        val outcome = useCase.moveSidebarItem(
            MoveSidebarItemRequest(
                itemType = SidebarItemType.Connection,
                itemId = "c1",
                targetParentGroupId = null,
                targetIndex = 0,
                placement = SidebarMovePlacement.After,
                referenceItemType = SidebarItemType.Group,
                referenceItemId = "g1",
            ),
        ).getOrThrow()

        assertTrue(!outcome.noOp)
        val stored = store.list().first { it.id == "c1" }
        assertTrue(stored.sortOrder > group.sortOrder)
        assertEquals(
            listOf("g1", "c1"),
            SidebarTreeBuilder.rootEntries(store.list(), store.listGroups()).map { it.id },
        )
    }

    @Test
    fun createGroup_appendsRootSortOrder() = runTest {
        val store = InMemoryProfileStore()
        val useCase = ManageConnections(store, RecordingConnectionPort())
        store.seedProfile(profile("c1", "Alpha", sortOrder = 1024L))

        useCase.createGroup(ConnectionGroup(id = "g1", name = "Dev")).getOrThrow()

        val group = store.listGroups().single()
        assertTrue(group.sortOrder > 1024L)
    }

    private class InMemoryProfileStore : ConnectionProfileStore {
        private val profiles = linkedMapOf<String, ConnectionProfile>()
        private val groups = linkedMapOf<String, ConnectionGroup>()

        fun seedProfile(vararg profiles: ConnectionProfile) {
            profiles.forEach { this.profiles[it.id] = it }
        }

        fun seedGroup(group: ConnectionGroup) {
            groups[group.id] = group
        }

        override suspend fun list(): List<ConnectionProfile> = profiles.values.toList()

        override suspend fun upsert(profile: ConnectionProfile) {
            profiles[profile.id] = profile
        }

        override suspend fun delete(id: String) {
            profiles.remove(id)
        }

        override suspend fun listGroups(): List<ConnectionGroup> = groups.values.toList()

        override suspend fun upsertGroup(group: ConnectionGroup) {
            groups[group.id] = group
        }

        override suspend fun deleteGroup(id: String) {
            groups.remove(id)
        }

        override suspend fun replaceAll(
            profiles: List<ConnectionProfile>,
            groups: List<ConnectionGroup>,
        ) {
            this.profiles.clear()
            this.groups.clear()
            profiles.forEach { this.profiles[it.id] = it }
            groups.forEach { this.groups[it.id] = it }
        }
    }

    private class RecordingConnectionPort : RedisConnectionPort {
        private val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

        override suspend fun test(profile: ConnectionProfile): Result<Unit> = Result.success(Unit)

        override suspend fun connect(profile: ConnectionProfile): Result<Unit> = Result.success(Unit)

        override suspend fun disconnect() = Unit

        override fun connectionState(): StateFlow<ConnectionState> = state
    }

    private fun profile(id: String, name: String, sortOrder: Long) = ConnectionProfile(
        id = id,
        name = name,
        mode = DeploymentMode.Standalone,
        host = "127.0.0.1",
        sortOrder = sortOrder,
    )
}
