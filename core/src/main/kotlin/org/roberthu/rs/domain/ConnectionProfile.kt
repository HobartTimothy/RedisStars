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
    val connectMs: Long = 60_000,
    val commandMs: Long = 60_000,
    val reconnectMs: Long = 30_000,
)

/** Key browser rendering mode: hierarchical (split on [ConnectionBrowserOptions.keySeparator]) or flat. */
enum class KeyListViewMode { Tree, Flat }

/** Controls which databases are shown in the database switcher. */
enum class DatabaseFilterMode { ShowAll, ShowSpecified, HideSpecified }

/** Optional color tag shown next to a connection in the connections list. */
enum class ConnectionTagColor { None, Red, Orange, Yellow, Green, Blue, Purple }

/**
 * Per-connection preferences for the key browser UI. Persisted alongside [ConnectionProfile] but
 * kept as a separate type so it can evolve (new view modes, filters) without touching connection
 * identity/auth fields.
 */
data class ConnectionBrowserOptions(
    val keyPattern: String = "*",
    val keySeparator: String = ":",
    val keyListView: KeyListViewMode = KeyListViewMode.Tree,
    val keyLoadBatchSize: Int = 10_000,
    val databaseFilterMode: DatabaseFilterMode = DatabaseFilterMode.ShowAll,
    /** Parsed DB indices; empty when [databaseFilterMode] is [DatabaseFilterMode.ShowAll]. */
    val databaseFilterValues: List<Int> = emptyList(),
    val tagColor: ConnectionTagColor = ConnectionTagColor.None,
)

/** Coerces free-form/legacy values (blank pattern, out-of-range batch size, stale filters) into safe bounds. */
fun ConnectionBrowserOptions.normalized(): ConnectionBrowserOptions = copy(
    keyPattern = keyPattern.ifBlank { "*" },
    keySeparator = keySeparator.ifBlank { ":" },
    keyLoadBatchSize = keyLoadBatchSize.coerceIn(1, 100_000),
    databaseFilterValues = if (databaseFilterMode == DatabaseFilterMode.ShowAll) {
        emptyList()
    } else {
        databaseFilterValues.distinct().sorted()
    },
)

/** Applies [databaseFilterMode]/[databaseFilterValues] to narrow the databases shown in the switcher. */
fun ConnectionBrowserOptions.filterDatabases(all: List<RedisDatabaseSummary>): List<RedisDatabaseSummary> =
    when (databaseFilterMode) {
        DatabaseFilterMode.ShowAll -> all
        DatabaseFilterMode.ShowSpecified -> all.filter { it.index in databaseFilterValues }
        DatabaseFilterMode.HideSpecified -> all.filter { it.index !in databaseFilterValues }
    }

/**
 * Parses/formats the compact database index list syntax used by the "specified databases" filter
 * text field, e.g. `"0,1,3-5"` <-> `[0, 1, 3, 4, 5]`.
 */
object DatabaseIndexListParser {
    fun parse(raw: String): Result<List<Int>> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())

        val values = sortedSetOf<Int>()
        for (segment in trimmed.split(",")) {
            val token = segment.trim()
            if (token.isEmpty()) {
                return Result.failure(IllegalArgumentException("Database list contains an empty entry"))
            }
            if ("-" in token) {
                val bounds = token.split("-")
                val start = bounds.getOrNull(0)?.trim()?.toIntOrNull()
                val end = bounds.getOrNull(1)?.trim()?.toIntOrNull()
                if (bounds.size != 2 || start == null || end == null || start < 0 || end < 0 || start > end) {
                    return Result.failure(IllegalArgumentException("Invalid database range: \"$token\""))
                }
                values += start..end
            } else {
                val value = token.toIntOrNull()
                if (value == null || value < 0) {
                    return Result.failure(IllegalArgumentException("Invalid database index: \"$token\""))
                }
                values += value
            }
        }
        return Result.success(values.toList())
    }

    fun format(values: List<Int>): String {
        val sorted = values.distinct().sorted()
        if (sorted.isEmpty()) return ""

        val parts = mutableListOf<String>()
        var rangeStart = sorted[0]
        var rangeEnd = sorted[0]
        for (index in 1 until sorted.size) {
            val current = sorted[index]
            if (current == rangeEnd + 1) {
                rangeEnd = current
            } else {
                parts += formatRange(rangeStart, rangeEnd)
                rangeStart = current
                rangeEnd = current
            }
        }
        parts += formatRange(rangeStart, rangeEnd)
        return parts.joinToString(",")
    }

    private fun formatRange(start: Int, end: Int): String =
        if (start == end) start.toString() else "$start-$end"
}

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
    val browser: ConnectionBrowserOptions = ConnectionBrowserOptions(),
    /** When null, the profile is shown under the implicit ungrouped section. */
    val groupId: String? = null,
    /** Sort position within [groupId] scope, or root scope when [groupId] is null. */
    val sortOrder: Long = 0L,
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
