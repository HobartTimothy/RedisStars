package org.roberthu.rs.usecase

import kotlinx.coroutines.flow.StateFlow
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.MoveSidebarItemRequest
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.SidebarMoveEngine
import org.roberthu.rs.domain.SidebarMoveOutcome
import org.roberthu.rs.domain.SidebarOrder
import org.roberthu.rs.domain.SidebarSortMigration
import org.roberthu.rs.domain.SidebarTreeBuilder
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.port.RedisConnectionPort

class ManageConnections(
    private val profileStore: ConnectionProfileStore,
    private val connectionPort: RedisConnectionPort,
) {
    suspend fun list(): List<ConnectionProfile> =
        migrateIfNeeded().profiles.sortedWith(profileComparator())

    suspend fun listGroups(): List<ConnectionGroup> =
        migrateIfNeeded().groups.sortedWith(groupComparator())

    suspend fun create(profile: ConnectionProfile): Result<Unit> = save(profile)

    suspend fun update(profile: ConnectionProfile): Result<Unit> = save(profile)

    suspend fun createGroup(group: ConnectionGroup): Result<Unit> {
        val data = migrateIfNeeded()
        val existing = data.groups
            .filter { it.id != group.id }
            .map { it.name.trim() }
        val trimmed = group.copy(name = group.name.trim())
        val errors = trimmed.validate(existing)
        if (errors.isNotEmpty()) {
            return Result.failure(RedisError.Validation(errors.joinToString("; ")))
        }
        val sortOrder = SidebarOrder.nextSortOrder(SidebarTreeBuilder.rootSortOrders(data.profiles, data.groups))
        profileStore.upsertGroup(trimmed.copy(sortOrder = sortOrder))
        return Result.success(Unit)
    }

    suspend fun updateGroup(group: ConnectionGroup): Result<Unit> = createGroup(group)

    suspend fun deleteGroup(id: String) {
        val data = migrateIfNeeded()
        val rootOrders = SidebarTreeBuilder.rootSortOrders(data.profiles, data.groups)
        var nextRootOrder = SidebarOrder.nextSortOrder(rootOrders)
        val updatedProfiles = data.profiles.map { profile ->
            if (profile.groupId == id) {
                profile.copy(groupId = null, sortOrder = nextRootOrder.also { nextRootOrder += SidebarOrder.STEP })
            } else {
                profile
            }
        }
        profileStore.replaceAll(
            profiles = updatedProfiles,
            groups = data.groups.filterNot { it.id == id },
        )
    }

    suspend fun setGroupExpanded(id: String, expanded: Boolean): Result<Unit> {
        val group = profileStore.listGroups().firstOrNull { it.id == id }
            ?: return Result.failure(RedisError.Validation("连接组不存在"))
        profileStore.upsertGroup(group.copy(expanded = expanded))
        return Result.success(Unit)
    }

    suspend fun moveSidebarItem(request: MoveSidebarItemRequest): Result<SidebarMoveOutcome> {
        val data = migrateIfNeeded()
        val outcome = SidebarMoveEngine.applyMove(data.profiles, data.groups, request).getOrElse { error ->
            return Result.failure(RedisError.Validation(error.message ?: "Invalid sidebar move"))
        }
        if (outcome.noOp) {
            return Result.success(outcome)
        }

        val expandGroupId = outcome.expandGroupId
        val groupsToPersist = if (expandGroupId != null) {
            outcome.groups.map { group ->
                if (group.id == expandGroupId) group.copy(expanded = true) else group
            }
        } else {
            outcome.groups
        }

        profileStore.replaceAll(outcome.profiles, groupsToPersist)
        return Result.success(outcome.copy(groups = groupsToPersist))
    }

    suspend fun copy(
        sourceId: String,
        newId: String,
        newName: String,
    ): Result<ConnectionProfile> {
        val data = migrateIfNeeded()
        val source = data.profiles.firstOrNull { it.id == sourceId }
            ?: return Result.failure(
                RedisError.Validation("Connection profile '$sourceId' does not exist"),
            )
        val sortOrder = if (source.groupId == null) {
            SidebarOrder.nextSortOrder(SidebarTreeBuilder.rootSortOrders(data.profiles, data.groups))
        } else {
            SidebarOrder.nextSortOrder(SidebarTreeBuilder.groupSortOrders(data.profiles, source.groupId))
        }
        val copied = source.copy(id = newId, name = newName, sortOrder = sortOrder)
        val validation = validate(copied)
        if (validation.isFailure) {
            return Result.failure(validation.exceptionOrNull()!!)
        }

        profileStore.upsert(copied)
        return Result.success(copied)
    }

    suspend fun delete(id: String) {
        profileStore.delete(id)
    }

    suspend fun test(profile: ConnectionProfile): Result<Unit> {
        val validation = validate(profile)
        return if (validation.isFailure) validation else connectionPort.test(profile)
    }

    suspend fun connect(profile: ConnectionProfile): Result<Unit> {
        val validation = validate(profile)
        return if (validation.isFailure) validation else connectionPort.connect(profile)
    }

    suspend fun disconnect() {
        connectionPort.disconnect()
    }

    fun connectionState(): StateFlow<ConnectionState> = connectionPort.connectionState()

    private suspend fun save(profile: ConnectionProfile): Result<Unit> {
        val validation = validate(profile)
        if (validation.isFailure) {
            return validation
        }

        val data = migrateIfNeeded()
        val existing = data.profiles.firstOrNull { it.id == profile.id }
        val profileToSave = when {
            existing == null -> assignSortOrderForNewProfile(data, profile)
            existing.groupId != profile.groupId -> assignSortOrderForGroupChange(data, profile)
            else -> profile.copy(sortOrder = existing.sortOrder)
        }

        profileStore.upsert(profileToSave)
        return Result.success(Unit)
    }

    private fun assignSortOrderForNewProfile(
        data: MigratedConnections,
        profile: ConnectionProfile,
    ): ConnectionProfile {
        val sortOrder = if (profile.groupId == null) {
            SidebarOrder.nextSortOrder(SidebarTreeBuilder.rootSortOrders(data.profiles, data.groups))
        } else {
            SidebarOrder.nextSortOrder(SidebarTreeBuilder.groupSortOrders(data.profiles, profile.groupId))
        }
        return profile.copy(sortOrder = sortOrder)
    }

    private fun assignSortOrderForGroupChange(
        data: MigratedConnections,
        profile: ConnectionProfile,
    ): ConnectionProfile {
        val sortOrder = if (profile.groupId == null) {
            SidebarOrder.nextSortOrder(SidebarTreeBuilder.rootSortOrders(data.profiles, data.groups))
        } else {
            SidebarOrder.nextSortOrder(SidebarTreeBuilder.groupSortOrders(data.profiles, profile.groupId))
        }
        return profile.copy(sortOrder = sortOrder)
    }

    private suspend fun migrateIfNeeded(): MigratedConnections {
        val profiles = profileStore.list()
        val groups = profileStore.listGroups()
        val (migratedProfiles, migratedGroups) = SidebarSortMigration.migrate(profiles, groups)
        if (migratedProfiles != profiles || migratedGroups != groups) {
            profileStore.replaceAll(migratedProfiles, migratedGroups)
        }
        return MigratedConnections(migratedProfiles, migratedGroups)
    }

    private fun validate(profile: ConnectionProfile): Result<Unit> {
        val errors = profile.validate()
        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(RedisError.Validation(errors.joinToString("; ")))
        }
    }

    private fun profileComparator() = compareBy<ConnectionProfile>({ it.sortOrder }, { it.name }, { it.id })

    private fun groupComparator() = compareBy<ConnectionGroup>({ it.sortOrder }, { it.name }, { it.id })

    private data class MigratedConnections(
        val profiles: List<ConnectionProfile>,
        val groups: List<ConnectionGroup>,
    )
}
