package org.roberthu.rs.presentation

import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.HostPort
import org.roberthu.rs.domain.SshAuthMethod
import org.roberthu.rs.domain.SshTunnelOptions
import org.roberthu.rs.domain.TimeoutOptions
import org.roberthu.rs.domain.TlsOptions
import org.roberthu.rs.i18n.AppI18n
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.ValidationI18n

enum class ConnectionEditorMode {
    Create,
    Edit,
}

enum class ConnectionEditorSection {
    General,
    Advanced,
    Tls,
    Sentinel,
    Cluster,
}

data class HostPortFormState(
    val id: String,
    val host: String,
    val port: String,
)

data class ConnectionFormState(
    val name: String,
    val deploymentMode: DeploymentMode,
    val host: String,
    val port: String,
    val database: String,
    val username: String,
    val password: String,
    val clientName: String,
    val connectTimeoutMs: String,
    val commandTimeoutMs: String,
    val reconnectTimeoutMs: String,
    val tlsEnabled: Boolean,
    val verifyPeer: Boolean,
    val masterName: String,
    val sentinelNodes: List<HostPortFormState>,
    val seedNodes: List<HostPortFormState>,
    val sshEnabled: Boolean = false,
    val sshHost: String = "",
    val sshPort: String = "22",
    val sshUsername: String = "",
    val sshAuthMethod: SshAuthMethod = SshAuthMethod.Password,
    val sshPassword: String = "",
    val sshPrivateKey: String = "",
    val sshPrivateKeyPath: String = "",
    val sshPrivateKeyPassphrase: String = "",
    val sshConnectTimeoutMs: String = "10000",
    val groupId: String? = null,
) {
    fun toConnectionProfile(id: String): ConnectionFormConversionResult {
        val fieldErrors = linkedMapOf<String, String>()

        if (name.isBlank()) {
            fieldErrors["name"] = AppI18n.t(StringKeys.Validation.NameRequired)
        }

        val portValue = when (deploymentMode) {
            DeploymentMode.Standalone -> parsePort(port, "port", fieldErrors)
            else -> null
        }
        val databaseValue = 0
        val connectTimeout = parsePositiveLong(
            connectTimeoutMs,
            "connectTimeoutMs",
            AppI18n.t(StringKeys.ConnectionEditor.ConnectTimeoutLabel),
            fieldErrors,
        )
        val commandTimeout = parsePositiveLong(
            commandTimeoutMs,
            "commandTimeoutMs",
            AppI18n.t(StringKeys.ConnectionEditor.CommandTimeoutLabel),
            fieldErrors,
        )
        val reconnectTimeout = parsePositiveLong(
            reconnectTimeoutMs,
            "reconnectTimeoutMs",
            AppI18n.t(StringKeys.ConnectionEditor.ReconnectTimeoutLabel),
            fieldErrors,
        )

        val sentinel = when (deploymentMode) {
            DeploymentMode.Sentinel -> parseHostPortList(
                nodes = sentinelNodes,
                listKey = "sentinelNodes",
                emptyMessage = AppI18n.t(StringKeys.Validation.SentinelNodeRequired),
                itemLabel = AppI18n.t(StringKeys.Validation.SentinelNodeItem),
                fieldErrors = fieldErrors,
            )
            else -> emptyList()
        }
        val seeds = when (deploymentMode) {
            DeploymentMode.Cluster -> parseHostPortList(
                nodes = seedNodes,
                listKey = "seedNodes",
                emptyMessage = AppI18n.t(StringKeys.Validation.ClusterNodeRequired),
                itemLabel = AppI18n.t(StringKeys.Validation.SeedNodeItem),
                fieldErrors = fieldErrors,
            )
            else -> emptyList()
        }

        if (deploymentMode == DeploymentMode.Sentinel && masterName.isBlank()) {
            fieldErrors["masterName"] = AppI18n.t(StringKeys.Validation.MasterNameRequired)
        }
        if (deploymentMode == DeploymentMode.Standalone && host.isBlank()) {
            fieldErrors["host"] = AppI18n.t(StringKeys.Validation.HostRequired)
        }

        val sshEnabledEffective = sshEnabled && deploymentMode == DeploymentMode.Standalone
        var sshPortValue: Int? = null
        var sshConnectTimeout: Long? = null
        if (sshEnabledEffective) {
            if (sshHost.isBlank()) {
                fieldErrors["sshHost"] = AppI18n.t(StringKeys.Validation.SshHostRequired)
            }
            sshPortValue = parsePort(sshPort, "sshPort", fieldErrors)
            if (sshUsername.isBlank()) {
                fieldErrors["sshUsername"] = AppI18n.t(StringKeys.Validation.SshUsernameRequired)
            }
            sshConnectTimeout = parsePositiveLong(
                sshConnectTimeoutMs,
                "sshConnectTimeoutMs",
                AppI18n.t(StringKeys.ConnectionEditor.SshConnectTimeoutLabel),
                fieldErrors,
            )
            when (sshAuthMethod) {
                SshAuthMethod.Password -> {
                    if (sshPassword.isBlank()) {
                        fieldErrors["sshPassword"] = AppI18n.t(StringKeys.Validation.SshPasswordRequired)
                    }
                }
                SshAuthMethod.PrivateKey -> {
                    if (sshPrivateKey.isBlank() && sshPrivateKeyPath.isBlank()) {
                        fieldErrors["sshPrivateKey"] = AppI18n.t(StringKeys.Validation.SshPrivateKeyRequired)
                    }
                }
            }
        }

        if (fieldErrors.isNotEmpty()) {
            return ConnectionFormConversionResult.Failure(fieldErrors = fieldErrors)
        }

        val profile = ConnectionProfile(
            id = id,
            name = name.trim(),
            mode = deploymentMode,
            host = if (deploymentMode == DeploymentMode.Standalone) host.trim() else "",
            port = portValue ?: 6379,
            seedNodes = seeds,
            sentinelNodes = sentinel,
            masterName = if (deploymentMode == DeploymentMode.Sentinel) masterName.trim() else "",
            database = databaseValue,
            username = username.trim().ifBlank { null },
            password = password.ifBlank { null },
            tls = TlsOptions(enabled = tlsEnabled, verifyPeer = verifyPeer),
            timeouts = TimeoutOptions(
                connectMs = connectTimeout!!,
                commandMs = commandTimeout!!,
                reconnectMs = reconnectTimeout!!,
            ),
            clientName = clientName.trim().ifBlank { null },
            ssh = if (sshEnabledEffective) {
                SshTunnelOptions(
                    enabled = true,
                    host = sshHost.trim(),
                    port = sshPortValue ?: 22,
                    username = sshUsername.trim(),
                    authMethod = sshAuthMethod,
                    password = sshPassword.ifBlank { null },
                    privateKey = sshPrivateKey.ifBlank { null },
                    privateKeyPath = sshPrivateKeyPath.trim().ifBlank { null },
                    privateKeyPassphrase = sshPrivateKeyPassphrase.ifBlank { null },
                    connectTimeoutMs = sshConnectTimeout ?: 10_000,
                )
            } else {
                SshTunnelOptions()
            },
            groupId = groupId,
        )

        val domainErrors = profile.validate().map(ValidationI18n::localize)
        if (domainErrors.isNotEmpty()) {
            return ConnectionFormConversionResult.Failure(
                fieldErrors = emptyMap(),
                domainErrors = domainErrors,
                globalError = domainErrors.joinToString("; "),
            )
        }

        return ConnectionFormConversionResult.Success(profile)
    }

    companion object {
        fun defaults(): ConnectionFormState = ConnectionFormState(
            name = "New connection",
            deploymentMode = DeploymentMode.Standalone,
            host = "localhost",
            port = "6379",
            database = "0",
            username = "",
            password = "",
            clientName = "",
            connectTimeoutMs = "5000",
            commandTimeoutMs = "5000",
            reconnectTimeoutMs = "30000",
            tlsEnabled = false,
            verifyPeer = true,
            masterName = "",
            sentinelNodes = listOf(HostPortFormState(newNodeId(), "", "26379")),
            seedNodes = listOf(HostPortFormState(newNodeId(), "", "6379")),
            sshEnabled = false,
            sshHost = "",
            sshPort = "22",
            sshUsername = "",
            sshAuthMethod = SshAuthMethod.Password,
            sshPassword = "",
            sshPrivateKey = "",
            sshPrivateKeyPath = "",
            sshPrivateKeyPassphrase = "",
            sshConnectTimeoutMs = "10000",
        )

        fun from(profile: ConnectionProfile): ConnectionFormState = ConnectionFormState(
            name = profile.name,
            deploymentMode = profile.mode,
            host = profile.host,
            port = profile.port.toString(),
            database = profile.database.toString(),
            username = profile.username.orEmpty(),
            password = profile.password.orEmpty(),
            clientName = profile.clientName.orEmpty(),
            connectTimeoutMs = profile.timeouts.connectMs.toString(),
            commandTimeoutMs = profile.timeouts.commandMs.toString(),
            reconnectTimeoutMs = profile.timeouts.reconnectMs.toString(),
            tlsEnabled = profile.tls.enabled,
            verifyPeer = profile.tls.verifyPeer,
            masterName = profile.masterName,
            sentinelNodes = profile.sentinelNodes
                .map { HostPortFormState(newNodeId(), it.host, it.port.toString()) }
                .ifEmpty { listOf(HostPortFormState(newNodeId(), "", "26379")) },
            seedNodes = profile.seedNodes
                .map { HostPortFormState(newNodeId(), it.host, it.port.toString()) }
                .ifEmpty { listOf(HostPortFormState(newNodeId(), "", "6379")) },
            sshEnabled = profile.ssh.enabled,
            sshHost = profile.ssh.host,
            sshPort = profile.ssh.port.toString(),
            sshUsername = profile.ssh.username,
            sshAuthMethod = profile.ssh.authMethod,
            sshPassword = profile.ssh.password.orEmpty(),
            sshPrivateKey = profile.ssh.privateKey.orEmpty(),
            sshPrivateKeyPath = profile.ssh.privateKeyPath.orEmpty(),
            sshPrivateKeyPassphrase = profile.ssh.privateKeyPassphrase.orEmpty(),
            sshConnectTimeoutMs = profile.ssh.connectTimeoutMs.toString(),
            groupId = profile.groupId,
        )

        fun newNodeId(): String = "node-${nodeSequence++}"

        private var nodeSequence: Long = 0L
    }
}

sealed class ConnectionFormConversionResult {
    abstract val fieldErrors: Map<String, String>
    abstract val domainErrors: List<String>
    abstract val globalError: String?

    data class Success(val profile: ConnectionProfile) : ConnectionFormConversionResult() {
        override val fieldErrors: Map<String, String> = emptyMap()
        override val domainErrors: List<String> = emptyList()
        override val globalError: String? = null
    }

    data class Failure(
        override val fieldErrors: Map<String, String> = emptyMap(),
        override val domainErrors: List<String> = emptyList(),
        override val globalError: String? = null,
    ) : ConnectionFormConversionResult()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrThrow(): ConnectionProfile = when (this) {
        is Success -> profile
        is Failure -> error(
            globalError ?: fieldErrors.values.firstOrNull()
                ?: AppI18n.t(StringKeys.Errors.ValidationFailed),
        )
    }
}

data class ConnectionEditorUiState(
    val mode: ConnectionEditorMode,
    val profileId: String,
    val selectedSection: ConnectionEditorSection,
    val initialForm: ConnectionFormState,
    val form: ConnectionFormState,
    val pendingGroupId: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val globalError: String? = null,
    val testing: Boolean = false,
    val saving: Boolean = false,
    val testSucceeded: Boolean = false,
    val confirmDiscardVisible: Boolean = false,
) {
    val isDirty: Boolean get() = form != initialForm

    val title: String
        get() = when (mode) {
            ConnectionEditorMode.Create -> AppI18n.t(StringKeys.ConnectionEditor.TitleNew)
            ConnectionEditorMode.Edit -> AppI18n.t(StringKeys.ConnectionEditor.TitleEdit)
        }
}

private fun parsePort(
    raw: String,
    key: String,
    fieldErrors: MutableMap<String, String>,
): Int? {
    if (raw.isBlank()) {
        fieldErrors[key] = AppI18n.t(StringKeys.Validation.PortRequired)
        return null
    }
    val value = raw.toIntOrNull()
    if (value == null) {
        fieldErrors[key] = AppI18n.t(StringKeys.Validation.PortInvalidNumber)
        return null
    }
    if (value !in 1..65535) {
        fieldErrors[key] = AppI18n.t(StringKeys.Validation.PortRange)
        return null
    }
    return value
}

private fun parseNonNegativeInt(
    raw: String,
    key: String,
    label: String,
    fieldErrors: MutableMap<String, String>,
): Int? {
    if (raw.isBlank()) {
        fieldErrors[key] = AppI18n.t(StringKeys.Validation.LabelRequired, label)
        return null
    }
    val value = raw.toIntOrNull()
    if (value == null) {
        fieldErrors[key] = AppI18n.t(StringKeys.Validation.LabelInvalidNumber, label)
        return null
    }
    if (value < 0) {
        fieldErrors[key] = AppI18n.t(StringKeys.Validation.LabelMinZero, label)
        return null
    }
    return value
}

private fun parsePositiveLong(
    raw: String,
    key: String,
    label: String,
    fieldErrors: MutableMap<String, String>,
): Long? {
    if (raw.isBlank()) {
        fieldErrors[key] = AppI18n.t(StringKeys.Validation.LabelRequired, label)
        return null
    }
    val value = raw.toLongOrNull()
    if (value == null) {
        fieldErrors[key] = AppI18n.t(StringKeys.Validation.LabelInvalidNumber, label)
        return null
    }
    if (value <= 0) {
        fieldErrors[key] = AppI18n.t(StringKeys.Validation.LabelGreaterThanZero, label)
        return null
    }
    return value
}

private fun parseHostPortList(
    nodes: List<HostPortFormState>,
    listKey: String,
    emptyMessage: String,
    itemLabel: String,
    fieldErrors: MutableMap<String, String>,
): List<HostPort> {
    if (nodes.isEmpty()) {
        fieldErrors[listKey] = emptyMessage
        return emptyList()
    }
    val parsed = mutableListOf<HostPort>()
    nodes.forEachIndexed { index, node ->
        val hostKey = "$listKey.$index.host"
        val portKey = "$listKey.$index.port"
        if (node.host.isBlank()) {
            fieldErrors[hostKey] = AppI18n.t(StringKeys.Validation.NodeHostRequired, itemLabel, index + 1)
        }
        val port = parsePort(node.port, portKey, fieldErrors)
        if (node.host.isNotBlank() && port != null) {
            parsed += HostPort(node.host.trim(), port)
        }
    }
    if (listKey !in fieldErrors && parsed.isEmpty() && nodes.isNotEmpty()) {
        fieldErrors[listKey] = emptyMessage
    }
    return parsed
}
