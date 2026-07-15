package org.roberthu.rs.presentation

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.HostPort
import org.roberthu.rs.domain.SshAuthMethod
import org.roberthu.rs.domain.SshTunnelOptions
import org.roberthu.rs.domain.TimeoutOptions
import org.roberthu.rs.domain.TlsOptions
import org.roberthu.rs.port.UserSettings

object StoredDataCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun encodeSettings(settings: UserSettings): String =
        json.encodeToString(StoredSettings.from(settings))

    fun decodeSettings(value: String): UserSettings =
        json.decodeFromString<StoredSettings>(value).toDomain()

    fun encodeProfiles(profiles: List<ConnectionProfile>, rememberPasswords: Boolean): String =
        json.encodeToString(profiles.map { StoredProfile.from(it, rememberPasswords) })

    fun decodeProfiles(value: String): List<ConnectionProfile> =
        json.decodeFromString<List<StoredProfile>>(value).map(StoredProfile::toDomain)
}

@Serializable
private data class StoredSettings(
    val darkMode: Boolean = true,
    val autoConnect: Boolean = false,
    val recentConnectionId: String? = null,
    val rememberPasswords: Boolean = false,
    val remoteBaseUrl: String = "http://127.0.0.1:8080",
) {
    fun toDomain() = UserSettings(
        darkMode = darkMode,
        autoConnect = autoConnect,
        recentConnectionId = recentConnectionId,
        rememberPasswords = rememberPasswords,
        remoteBaseUrl = remoteBaseUrl,
    )

    companion object {
        fun from(settings: UserSettings) = StoredSettings(
            settings.darkMode,
            settings.autoConnect,
            settings.recentConnectionId,
            settings.rememberPasswords,
            settings.remoteBaseUrl,
        )
    }
}

@Serializable
private data class StoredHostPort(val host: String, val port: Int)

@Serializable
private data class StoredTlsOptions(
    val enabled: Boolean = false,
    val verifyPeer: Boolean = true,
)

@Serializable
private data class StoredTimeoutOptions(
    val connectMs: Long = 5_000,
    val commandMs: Long = 5_000,
    val reconnectMs: Long = 30_000,
)

@Serializable
private data class StoredSshOptions(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val authMethod: String = SshAuthMethod.Password.name,
    val password: String? = null,
    val privateKey: String? = null,
    val privateKeyPath: String? = null,
    val privateKeyPassphrase: String? = null,
    val connectTimeoutMs: Long = 10_000,
)

@Serializable
private data class StoredProfile(
    val id: String,
    val name: String,
    val mode: String,
    val host: String = "",
    val port: Int = 6379,
    val seedNodes: List<StoredHostPort> = emptyList(),
    val sentinelNodes: List<StoredHostPort> = emptyList(),
    val masterName: String = "",
    val database: Int = 0,
    val username: String? = null,
    val password: String? = null,
    val tls: StoredTlsOptions = StoredTlsOptions(),
    val timeouts: StoredTimeoutOptions = StoredTimeoutOptions(),
    val clientName: String? = null,
    val ssh: StoredSshOptions = StoredSshOptions(),
) {
    fun toDomain() = ConnectionProfile(
        id = id,
        name = name,
        mode = DeploymentMode.valueOf(mode),
        host = host,
        port = port,
        seedNodes = seedNodes.map { HostPort(it.host, it.port) },
        sentinelNodes = sentinelNodes.map { HostPort(it.host, it.port) },
        masterName = masterName,
        database = database,
        username = username,
        password = password,
        tls = TlsOptions(tls.enabled, tls.verifyPeer),
        timeouts = TimeoutOptions(timeouts.connectMs, timeouts.commandMs, timeouts.reconnectMs),
        clientName = clientName,
        ssh = SshTunnelOptions(
            enabled = ssh.enabled,
            host = ssh.host,
            port = ssh.port,
            username = ssh.username,
            authMethod = runCatching { SshAuthMethod.valueOf(ssh.authMethod) }
                .getOrDefault(SshAuthMethod.Password),
            password = ssh.password,
            privateKey = ssh.privateKey,
            privateKeyPath = ssh.privateKeyPath,
            privateKeyPassphrase = ssh.privateKeyPassphrase,
            connectTimeoutMs = ssh.connectTimeoutMs,
        ),
    )

    companion object {
        fun from(profile: ConnectionProfile, rememberPassword: Boolean) = StoredProfile(
            id = profile.id,
            name = profile.name,
            mode = profile.mode.name,
            host = profile.host,
            port = profile.port,
            seedNodes = profile.seedNodes.map { StoredHostPort(it.host, it.port) },
            sentinelNodes = profile.sentinelNodes.map { StoredHostPort(it.host, it.port) },
            masterName = profile.masterName,
            database = profile.database,
            username = profile.username,
            password = profile.password.takeIf { rememberPassword },
            tls = StoredTlsOptions(profile.tls.enabled, profile.tls.verifyPeer),
            timeouts = StoredTimeoutOptions(
                profile.timeouts.connectMs,
                profile.timeouts.commandMs,
                profile.timeouts.reconnectMs,
            ),
            clientName = profile.clientName,
            ssh = StoredSshOptions(
                enabled = profile.ssh.enabled,
                host = profile.ssh.host,
                port = profile.ssh.port,
                username = profile.ssh.username,
                authMethod = profile.ssh.authMethod.name,
                password = profile.ssh.password.takeIf { rememberPassword },
                privateKey = profile.ssh.privateKey.takeIf { rememberPassword },
                privateKeyPath = profile.ssh.privateKeyPath,
                privateKeyPassphrase = profile.ssh.privateKeyPassphrase.takeIf { rememberPassword },
                connectTimeoutMs = profile.ssh.connectTimeoutMs,
            ),
        )
    }
}
