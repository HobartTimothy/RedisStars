package org.roberthu.rs.presentation

import kotlinx.coroutines.CoroutineScope
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.KeyBrowserPort
import org.roberthu.rs.port.KeyCommandPort
import org.roberthu.rs.port.RedisConnectionPort
import org.roberthu.rs.port.RedisDataPort
import org.roberthu.rs.port.UserSettings
import org.roberthu.rs.port.UserSettingsStore

class AppContainer(
    val connectionProfileStore: ConnectionProfileStore,
    val userSettingsStore: UserSettingsStore,
    val redisConnectionPort: RedisConnectionPort? = null,
    val keyBrowserPort: KeyBrowserPort? = null,
    val keyCommandPort: KeyCommandPort? = null,
    val redisDataPort: RedisDataPort? = null,
    private val shellViewModelFactory: (UserSettingsStore, CoroutineScope) -> ShellViewModel =
        ::ShellViewModel,
) {
    fun createShellViewModel(scope: CoroutineScope): ShellViewModel =
        shellViewModelFactory(userSettingsStore, scope)

    fun createConnectionsViewModel(scope: CoroutineScope): ConnectionsViewModel? =
        redisConnectionPort?.let {
            ConnectionsViewModel(connectionProfileStore, it, scope)
        }

    fun createKeyBrowserViewModel(scope: CoroutineScope): KeyBrowserViewModel? =
        keyBrowserPort?.let { KeyBrowserViewModel(it, scope) }

    fun createKeyDetailViewModel(scope: CoroutineScope): KeyDetailViewModel? {
        val commands = keyCommandPort ?: return null
        val data = redisDataPort ?: return null
        return KeyDetailViewModel(commands, data, scope)
    }

    companion object {
        fun preview(): AppContainer = AppContainer(
            connectionProfileStore = InMemoryConnectionProfileStore(),
            userSettingsStore = InMemoryUserSettingsStore(),
        )
    }
}

class InMemoryUserSettingsStore(
    initialSettings: UserSettings = UserSettings(),
) : UserSettingsStore {
    private var settings = initialSettings

    override suspend fun load(): UserSettings = settings

    override suspend fun save(settings: UserSettings) {
        this.settings = settings
    }
}

class InMemoryConnectionProfileStore(
    initialProfiles: List<ConnectionProfile> = emptyList(),
) : ConnectionProfileStore {
    private val profiles = initialProfiles.associateByTo(mutableMapOf()) { it.id }

    override suspend fun list(): List<ConnectionProfile> = profiles.values.toList()

    override suspend fun upsert(profile: ConnectionProfile) {
        profiles[profile.id] = profile
    }

    override suspend fun delete(id: String) {
        profiles.remove(id)
    }
}
