package org.roberthu.rs.port

import org.roberthu.rs.domain.ConnectionProfile

interface ConnectionProfileStore {
    suspend fun list(): List<ConnectionProfile>
    suspend fun upsert(profile: ConnectionProfile)
    suspend fun delete(id: String)
}

data class UserSettings(
    val darkMode: Boolean = true,
    val autoConnect: Boolean = false,
    val recentConnectionId: String? = null,
    val rememberPasswords: Boolean = false,
    val remoteBaseUrl: String = "http://127.0.0.1:8080",
)

interface UserSettingsStore {
    suspend fun load(): UserSettings
    suspend fun save(settings: UserSettings)
}
