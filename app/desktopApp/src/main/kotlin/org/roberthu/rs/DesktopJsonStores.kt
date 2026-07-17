package org.roberthu.rs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.UserSettings
import org.roberthu.rs.port.UserSettingsStore
import org.roberthu.rs.presentation.StoredConnections
import org.roberthu.rs.presentation.StoredDataCodec
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Persists [UserSettings] as JSON with in-memory caching and serialized access.
 *
 * Consistency: mutations write to disk first and only update the in-memory snapshot after a
 * successful atomic write. On failure the previous memory snapshot is retained, so memory never
 * advances ahead of disk.
 */
class DesktopJsonUserSettingsStore(
    private val file: Path,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fileWriter: (Path, String) -> Unit = ::writeAtomically,
) : UserSettingsStore {
    private val mutex = Mutex()
    private var cached: UserSettings? = null

    /**
     * Current remember-passwords preference from the in-memory snapshot, or the default when
     * settings have not been loaded yet.
     */
    fun rememberPasswordsEnabled(): Boolean =
        cached?.rememberPasswords ?: UserSettings().rememberPasswords

    override suspend fun load(): UserSettings = mutex.withLock {
        cached ?: readSettingsFromDisk().also { cached = it }
    }

    override suspend fun save(settings: UserSettings) {
        mutex.withLock {
            withContext(ioDispatcher) {
                fileWriter(file, StoredDataCodec.encodeSettings(settings))
            }
            cached = settings
        }
    }

    private suspend fun readSettingsFromDisk(): UserSettings = withContext(ioDispatcher) {
        if (file.exists()) {
            StoredDataCodec.decodeSettings(file.readText())
        } else {
            UserSettings()
        }
    }
}

/**
 * Persists connection groups and profiles as JSON with in-memory caching and serialized access.
 *
 * Consistency: same write-first policy as [DesktopJsonUserSettingsStore]. When
 * [rememberPasswords] returns `false`, secrets are stripped from disk encoding while the in-memory
 * snapshot may still hold session passwords for runtime use.
 */
class DesktopJsonConnectionProfileStore(
    private val file: Path,
    private val rememberPasswords: () -> Boolean,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val fileWriter: (Path, String) -> Unit = ::writeAtomically,
) : ConnectionProfileStore {
    private val mutex = Mutex()
    private var cached: StoredConnections? = null

    override suspend fun list(): List<ConnectionProfile> = mutex.withLock {
        loadedConnections().profiles
    }

    override suspend fun upsert(profile: ConnectionProfile) {
        mutex.withLock {
            val current = loadedConnections()
            val profiles = current.profiles.associateByTo(linkedMapOf()) { it.id }
            profiles[profile.id] = profile
            persist(current.copy(profiles = profiles.values.toList()))
        }
    }

    override suspend fun delete(id: String) {
        mutex.withLock {
            val current = loadedConnections()
            persist(current.copy(profiles = current.profiles.filterNot { it.id == id }))
        }
    }

    override suspend fun listGroups(): List<ConnectionGroup> = mutex.withLock {
        loadedConnections().groups
    }

    override suspend fun upsertGroup(group: ConnectionGroup) {
        mutex.withLock {
            val current = loadedConnections()
            val groups = current.groups.associateByTo(linkedMapOf()) { it.id }
            groups[group.id] = group
            persist(current.copy(groups = groups.values.toList()))
        }
    }

    override suspend fun deleteGroup(id: String) {
        mutex.withLock {
            val current = loadedConnections()
            persist(
                current.copy(
                    groups = current.groups.filterNot { it.id == id },
                    profiles = current.profiles.map {
                        if (it.groupId == id) it.copy(groupId = null) else it
                    },
                ),
            )
        }
    }

    private suspend fun loadedConnections(): StoredConnections =
        cached ?: readConnectionsFromDisk().also { cached = it }

    private suspend fun persist(data: StoredConnections) {
        withContext(ioDispatcher) {
            fileWriter(
                file,
                StoredDataCodec.encodeConnections(data, rememberPasswords = rememberPasswords()),
            )
        }
        cached = data
    }

    private suspend fun readConnectionsFromDisk(): StoredConnections = withContext(ioDispatcher) {
        if (file.exists()) {
            StoredDataCodec.decodeConnections(file.readText())
        } else {
            StoredConnections()
        }
    }
}

fun desktopConfigDirectory(): Path {
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    val appData = System.getenv("APPDATA")?.takeIf(String::isNotBlank)
    return if (isWindows && appData != null) {
        Path.of(appData, "RedisStars")
    } else {
        Path.of(System.getProperty("user.home"), ".config", "redis-stars")
    }
}

internal fun writeAtomically(file: Path, content: String) {
    Files.createDirectories(file.parent)
    val temporary = file.resolveSibling("${file.fileName}.tmp")
    temporary.writeText(content)
    try {
        Files.move(
            temporary,
            file,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE,
        )
    } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
    }
}
