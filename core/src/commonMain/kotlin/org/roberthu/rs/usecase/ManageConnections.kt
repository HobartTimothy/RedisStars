package org.roberthu.rs.usecase

import kotlinx.coroutines.flow.StateFlow
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

    suspend fun create(profile: ConnectionProfile): Result<Unit> = save(profile)

    suspend fun update(profile: ConnectionProfile): Result<Unit> = save(profile)

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
