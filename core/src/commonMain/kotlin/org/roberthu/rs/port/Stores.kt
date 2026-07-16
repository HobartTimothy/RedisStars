package org.roberthu.rs.port

import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile

interface ConnectionProfileStore {
    suspend fun list(): List<ConnectionProfile>
    suspend fun upsert(profile: ConnectionProfile)
    suspend fun delete(id: String)

    suspend fun listGroups(): List<ConnectionGroup> = emptyList()
    suspend fun upsertGroup(group: ConnectionGroup) = Unit
    suspend fun deleteGroup(id: String) = Unit
}

data class UserSettings(
    val darkMode: Boolean = true,
    val autoConnect: Boolean = false,
    val recentConnectionId: String? = null,
    val rememberPasswords: Boolean = true,
    val remoteBaseUrl: String = "http://127.0.0.1:8080",
)

interface UserSettingsStore {
    suspend fun load(): UserSettings
    suspend fun save(settings: UserSettings)
}
