package org.roberthu.rs

import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.port.ConnectionProfileStore
import org.roberthu.rs.port.UserSettings
import org.roberthu.rs.port.UserSettingsStore
import org.roberthu.rs.presentation.StoredDataCodec
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.coroutines.sync.Mutex

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
    private val settingsStore: UserSettingsStore,
) : ConnectionProfileStore {
    private val lock = Mutex()

    override suspend fun list(): List<ConnectionProfile> = locked { readProfiles() }

    override suspend fun upsert(profile: ConnectionProfile) {
        locked {
            val profiles = readProfiles().associateByTo(linkedMapOf()) { it.id }
            profiles[profile.id] = profile
            writeProfiles(profiles.values.toList())
        }
    }

    override suspend fun delete(id: String) {
        locked {
            writeProfiles(readProfiles().filterNot { it.id == id })
        }
    }

    private fun readProfiles(): List<ConnectionProfile> =
        if (file.exists()) StoredDataCodec.decodeProfiles(file.readText()) else emptyList()

    private suspend fun writeProfiles(profiles: List<ConnectionProfile>) {
        val rememberPasswords = settingsStore.load().rememberPasswords
        writeAtomically(file, StoredDataCodec.encodeProfiles(profiles, rememberPasswords))
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
