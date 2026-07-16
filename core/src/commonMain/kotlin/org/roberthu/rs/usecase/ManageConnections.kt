package org.roberthu.rs.usecase

import kotlinx.coroutines.flow.StateFlow
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.port.RedisConnectionPort

class ManageConnections(
    private val profileStore: ConnectionProfileStore,
    private val connectionPort: RedisConnectionPort,
) {
    suspend fun list(): List<ConnectionProfile> = profileStore.list()

    suspend fun listGroups(): List<ConnectionGroup> =
        profileStore.listGroups().sortedWith(compareBy({ it.order }, { it.name }))

    suspend fun create(profile: ConnectionProfile): Result<Unit> = save(profile)

    suspend fun update(profile: ConnectionProfile): Result<Unit> = save(profile)

    suspend fun createGroup(group: ConnectionGroup): Result<Unit> {
        val existing = profileStore.listGroups()
            .filter { it.id != group.id }
            .map { it.name.trim() }
        val trimmed = group.copy(name = group.name.trim())
        val errors = trimmed.validate(existing)
        if (errors.isNotEmpty()) {
            return Result.failure(RedisError.Validation(errors.joinToString("; ")))
        }
        profileStore.upsertGroup(trimmed)
        return Result.success(Unit)
    }

    suspend fun updateGroup(group: ConnectionGroup): Result<Unit> = createGroup(group)

    suspend fun deleteGroup(id: String) {
        profileStore.list()
            .filter { it.groupId == id }
            .forEach { profileStore.upsert(it.copy(groupId = null)) }
        profileStore.deleteGroup(id)
    }

    suspend fun setGroupExpanded(id: String, expanded: Boolean): Result<Unit> {
        val group = profileStore.listGroups().firstOrNull { it.id == id }
            ?: return Result.failure(RedisError.Validation("连接组不存在"))
        profileStore.upsertGroup(group.copy(expanded = expanded))
        return Result.success(Unit)
    }

    suspend fun copy(
        sourceId: String,
        newId: String,
        newName: String,
    ): Result<ConnectionProfile> {
        val source = profileStore.list().firstOrNull { it.id == sourceId }
            ?: return Result.failure(
                RedisError.Validation("Connection profile '$sourceId' does not exist"),
            )
        val copied = source.copy(id = newId, name = newName)
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

        profileStore.upsert(profile)
        return Result.success(Unit)
    }

    private fun validate(profile: ConnectionProfile): Result<Unit> {
        val errors = profile.validate()
        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(RedisError.Validation(errors.joinToString("; ")))
        }
    }
}
