package org.roberthu.rs.domain

data class HostPort(
    val host: String,
    val port: Int,
)

data class TlsOptions(
    val enabled: Boolean,
    val verifyPeer: Boolean = true,
)

data class TimeoutOptions(
    val connectMs: Long = 5_000,
    val commandMs: Long = 5_000,
    val reconnectMs: Long = 30_000,
)

enum class SshAuthMethod {
    Password,
    PrivateKey,
}

/**
 * SSH local port-forward tunnel used only for [DeploymentMode.Standalone].
 * When enabled, Redis traffic is proxied through [host]:[port] to the Redis endpoint.
 */
data class SshTunnelOptions(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 22,
    val username: String = "",
    val authMethod: SshAuthMethod = SshAuthMethod.Password,
    val password: String? = null,
    val privateKey: String? = null,
    val privateKeyPath: String? = null,
    val privateKeyPassphrase: String? = null,
    val connectTimeoutMs: Long = 10_000,
)

data class ConnectionProfile(
    val id: String,
    val name: String,
    val mode: DeploymentMode,
    val host: String = "",
    val port: Int = 6379,
    val seedNodes: List<HostPort> = emptyList(),
    val sentinelNodes: List<HostPort> = emptyList(),
    val masterName: String = "",
    val database: Int = 0,
    val username: String? = null,
    val password: String? = null,
    val tls: TlsOptions = TlsOptions(enabled = false),
    val timeouts: TimeoutOptions = TimeoutOptions(),
    val clientName: String? = null,
    val ssh: SshTunnelOptions = SshTunnelOptions(),
    /** When null, the profile is shown under the implicit ungrouped section. */
    val groupId: String? = null,
) {
    fun validate(): List<String> = buildList {
        if (name.isBlank()) {
            add("Name must not be blank")
        }

        when (mode) {
            DeploymentMode.Standalone -> {
                if (host.isBlank()) {
                    add("Host must not be blank")
                }
                if (port !in 1..65535) {
                    add("Port must be between 1 and 65535")
                }
            }

            DeploymentMode.Sentinel -> {
                if (sentinelNodes.isEmpty()) {
                    add("At least one sentinel node is required")
                }
                sentinelNodes.forEachIndexed { index, node ->
                    if (node.host.isBlank() || node.port !in 1..65535) {
                        add("Sentinel node ${index + 1} must have a host and port between 1 and 65535")
                    }
                }
                if (masterName.isBlank()) {
                    add("Master name must not be blank")
                }
            }

            DeploymentMode.Cluster -> {
                if (seedNodes.isEmpty()) {
                    add("At least one seed node is required")
                }
                seedNodes.forEachIndexed { index, node ->
                    if (node.host.isBlank() || node.port !in 1..65535) {
                        add("Seed node ${index + 1} must have a host and port between 1 and 65535")
                    }
                }
                if (database != 0) {
                    add("Database must be 0 in cluster mode")
                }
            }
        }

        if (ssh.enabled) {
            if (mode != DeploymentMode.Standalone) {
                add("SSH tunnel is only supported in Standalone mode")
            }
            if (ssh.host.isBlank()) {
                add("SSH host must not be blank")
            }
            if (ssh.port !in 1..65535) {
                add("SSH port must be between 1 and 65535")
            }
            if (ssh.username.isBlank()) {
                add("SSH username must not be blank")
            }
            if (ssh.connectTimeoutMs <= 0) {
                add("SSH connect timeout must be greater than 0")
            }
            when (ssh.authMethod) {
                SshAuthMethod.Password -> {
                    if (ssh.password.isNullOrBlank()) {
                        add("SSH password must not be blank")
                    }
                }
                SshAuthMethod.PrivateKey -> {
                    val hasKeyMaterial =
                        !ssh.privateKey.isNullOrBlank() || !ssh.privateKeyPath.isNullOrBlank()
                    if (!hasKeyMaterial) {
                        add("SSH private key or private key path is required")
                    }
                }
            }
        }

        if (timeouts.connectMs <= 0) {
            add("Connect timeout must be greater than 0")
        }
        if (timeouts.commandMs <= 0) {
            add("Command timeout must be greater than 0")
        }
        if (timeouts.reconnectMs <= 0) {
            add("Reconnect timeout must be greater than 0")
        }
    }
}
