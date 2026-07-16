package org.roberthu.rs

import kotlinx.coroutines.sync.Mutex
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

class DesktopJsonUserSettingsStore(
    private val file: Path,
) : UserSettingsStore {
    private val lock = Any()

    override suspend fun load(): UserSettings = synchronized(lock) {
        if (file.exists()) StoredDataCodec.decodeSettings(file.readText()) else UserSettings()
    }

    override suspend fun save(settings: UserSettings) {
        synchronized(lock) {
            writeAtomically(file, StoredDataCodec.encodeSettings(settings))
        }
    }
}

class DesktopJsonConnectionProfileStore(
    private val file: Path,
) : ConnectionProfileStore {
    private val lock = Mutex()

    override suspend fun list(): List<ConnectionProfile> = locked { readConnections().profiles }

    override suspend fun upsert(profile: ConnectionProfile) {
        locked {
            val data = readConnections()
            val profiles = data.profiles.associateByTo(linkedMapOf()) { it.id }
            profiles[profile.id] = profile
            writeConnections(data.copy(profiles = profiles.values.toList()))
        }
    }

    override suspend fun delete(id: String) {
        locked {
            val data = readConnections()
            writeConnections(data.copy(profiles = data.profiles.filterNot { it.id == id }))
        }
    }

    override suspend fun listGroups(): List<ConnectionGroup> = locked { readConnections().groups }

    override suspend fun upsertGroup(group: ConnectionGroup) {
        locked {
            val data = readConnections()
            val groups = data.groups.associateByTo(linkedMapOf()) { it.id }
            groups[group.id] = group
            writeConnections(data.copy(groups = groups.values.toList()))
        }
    }

    override suspend fun deleteGroup(id: String) {
        locked {
            val data = readConnections()
            writeConnections(
                data.copy(
                    groups = data.groups.filterNot { it.id == id },
                    profiles = data.profiles.map {
                        if (it.groupId == id) it.copy(groupId = null) else it
                    },
                ),
            )
        }
    }

    private fun readConnections(): StoredConnections =
        if (file.exists()) StoredDataCodec.decodeConnections(file.readText()) else StoredConnections()

    private fun writeConnections(data: StoredConnections) {
        // Always persist Redis/SSH secrets with connection profiles.
        writeAtomically(file, StoredDataCodec.encodeConnections(data, rememberPasswords = true))
    }

    private suspend fun <T> locked(block: suspend () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
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

private fun writeAtomically(file: Path, content: String) {
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
