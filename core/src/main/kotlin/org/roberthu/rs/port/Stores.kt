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

    /** Atomically replaces all groups and profiles, used for sidebar reorder transactions. */
    suspend fun replaceAll(
        profiles: List<ConnectionProfile>,
        groups: List<ConnectionGroup>,
    ) {
        val existingProfiles = list().associateBy { it.id }
        val existingGroups = listGroups().associateBy { it.id }
        existingProfiles.keys.filterNot { it in profiles.map(ConnectionProfile::id).toSet() }
            .forEach { delete(it) }
        existingGroups.keys.filterNot { it in groups.map(ConnectionGroup::id).toSet() }
            .forEach { deleteGroup(it) }
        groups.forEach { upsertGroup(it) }
        profiles.forEach { upsert(it) }
    }
}

data class UserSettings(
    val darkMode: Boolean = true,
    val autoConnect: Boolean = false,
    val recentConnectionId: String? = null,
    val rememberPasswords: Boolean = true,
    val remoteBaseUrl: String = "http://127.0.0.1:8080",
    /**
     * Persisted UI language tag (`zh-CN` / `en-US`).
     * `null` means the user has not chosen yet — resolve from the system language on load.
     */
    val language: String? = null,
    /**
     * Whether the left navigation rail is collapsed.
     * `null` means never explicitly set — the UI defaults to expanded.
     */
    val railCollapsed: Boolean? = null,
)

interface UserSettingsStore {
    suspend fun load(): UserSettings
    suspend fun save(settings: UserSettings)
}
