package org.roberthu.rs.presentation

import kotlinx.coroutines.CoroutineScope
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.SidebarOrder
import org.roberthu.rs.domain.ApplicationLogEntry
import org.roberthu.rs.domain.ApplicationLogLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import org.roberthu.rs.port.ApplicationLogPort
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.util.ApplicationLogLineParser
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
    val applicationLogPort: ApplicationLogPort? = null,
    private val shellViewModelFactory: (UserSettingsStore, CoroutineScope) -> ShellViewModel =
        ::ShellViewModel,
    private val runtimeLogsViewModelFactory: (ApplicationLogPort, CoroutineScope) -> RuntimeLogsViewModel =
        ::RuntimeLogsViewModel,
) {
    fun createShellViewModel(scope: CoroutineScope): ShellViewModel =
        shellViewModelFactory(userSettingsStore, scope)

    fun createConnectionsViewModel(scope: CoroutineScope): ConnectionsViewModel? =
        redisConnectionPort?.let {
            ConnectionsViewModel(connectionProfileStore, it, scope)
        }

    fun createKeyBrowserViewModel(scope: CoroutineScope): KeyBrowserViewModel? {
        val browser = keyBrowserPort ?: return null
        val commands = keyCommandPort ?: return null
        return KeyBrowserViewModel(browser, commands, scope)
    }

    fun createKeyDetailViewModel(scope: CoroutineScope): KeyDetailViewModel? {
        val commands = keyCommandPort ?: return null
        val data = redisDataPort ?: return null
        return KeyDetailViewModel(commands, data, scope)
    }

    fun createRuntimeLogsViewModel(scope: CoroutineScope): RuntimeLogsViewModel? =
        applicationLogPort?.let { runtimeLogsViewModelFactory(it, scope) }

    companion object {
        fun preview(): AppContainer = AppContainer(
            connectionProfileStore = InMemoryConnectionProfileStore(),
            userSettingsStore = InMemoryUserSettingsStore(),
            applicationLogPort = InMemoryApplicationLogPort(),
        )
    }
}

open class InMemoryApplicationLogPort(
    initialEntries: List<ApplicationLogEntry> = emptyList(),
) : ApplicationLogPort {
    private val entries = initialEntries.toMutableList()
    private val events = MutableSharedFlow<ApplicationLogEntry>(extraBufferCapacity = 64)

    fun emit(entry: ApplicationLogEntry) {
        entries.add(entry)
        events.tryEmit(entry)
    }

    override suspend fun loadRecent(maxEntries: Int): Result<List<ApplicationLogEntry>> =
        Result.success(entries.takeLast(maxEntries))

    override fun watch(): Flow<ApplicationLogEntry> = events.asSharedFlow()

    override suspend fun export(entries: List<ApplicationLogEntry>, targetPath: String): Result<Unit> =
        runCatching {
            val content = entries.joinToString("\n") { ApplicationLogLineParser.formatForExport(it) }
            check(content.isNotEmpty())
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
    initialGroups: List<org.roberthu.rs.domain.ConnectionGroup> = emptyList(),
) : ConnectionProfileStore {
    private val profiles = initialProfiles.associateByTo(mutableMapOf()) { it.id }
    private val groups = initialGroups.associateByTo(mutableMapOf()) { it.id }

    override suspend fun list(): List<ConnectionProfile> = profiles.values.toList()

    override suspend fun upsert(profile: ConnectionProfile) {
        profiles[profile.id] = profile
    }

    override suspend fun delete(id: String) {
        profiles.remove(id)
    }

    override suspend fun listGroups(): List<org.roberthu.rs.domain.ConnectionGroup> =
        groups.values.toList()

    override suspend fun upsertGroup(group: org.roberthu.rs.domain.ConnectionGroup) {
        groups[group.id] = group
    }

    override suspend fun deleteGroup(id: String) {
        groups.remove(id)
        var nextRootOrder = SidebarOrder.nextSortOrder(
            profiles.values.filter { it.groupId == null }.map { it.sortOrder } +
                groups.values.map { it.sortOrder },
        )
        profiles.replaceAll { _, profile ->
            if (profile.groupId == id) {
                profile.copy(
                    groupId = null,
                    sortOrder = nextRootOrder.also { nextRootOrder += SidebarOrder.STEP },
                )
            } else {
                profile
            }
        }
    }

    override suspend fun replaceAll(
        profiles: List<ConnectionProfile>,
        groups: List<org.roberthu.rs.domain.ConnectionGroup>,
    ) {
        this.profiles.clear()
        this.groups.clear()
        profiles.forEach { this.profiles[it.id] = it }
        groups.forEach { this.groups[it.id] = it }
    }
}
