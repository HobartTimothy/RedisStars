package org.roberthu.rs.i18n

/**
 * Localizes validation messages from domain/presentation layers. Recognizes [StringKeys] values
 * directly; maps legacy English messages from [org.roberthu.rs.domain.ConnectionProfile.validate];
 * leaves unknown technical/server messages unchanged.
 */
object ValidationI18n {
    fun localize(message: String): String {
        if (StringCatalog.hasKey(message)) {
            return AppI18n.t(message)
        }
        return localizeLegacyDomainError(message)
    }

    fun localizeJoin(messages: String): String =
        messages.split("; ").joinToString("; ") { part ->
            localize(part.trim())
        }

    private fun localizeLegacyDomainError(message: String): String {
        val nodePortRange = Regex("""^(Sentinel|Seed) node (\d+) must have a host and port between 1 and 65535$""")
        nodePortRange.matchEntire(message)?.let { match ->
            val itemKey = when (match.groupValues[1]) {
                "Sentinel" -> StringKeys.Validation.SentinelNodeItem
                else -> StringKeys.Validation.SeedNodeItem
            }
            val index = match.groupValues[2].toInt()
            return AppI18n.t(StringKeys.Validation.NodePortRange, AppI18n.t(itemKey), index)
        }

        return when (message) {
            "Name must not be blank" -> AppI18n.t(StringKeys.Validation.NameRequired)
            "Host must not be blank" -> AppI18n.t(StringKeys.Validation.HostRequired)
            "Port must be between 1 and 65535" -> AppI18n.t(StringKeys.Validation.PortRange)
            "At least one sentinel node is required" ->
                AppI18n.t(StringKeys.Validation.SentinelNodeRequired)
            "At least one seed node is required" ->
                AppI18n.t(StringKeys.Validation.ClusterNodeRequired)
            "Master name must not be blank" -> AppI18n.t(StringKeys.Validation.MasterNameRequired)
            "Database must be 0 in cluster mode" ->
                AppI18n.t(StringKeys.Validation.ClusterDatabaseMustBeZero)
            "SSH tunnel is only supported in Standalone mode" ->
                AppI18n.t(StringKeys.Validation.SshTunnelStandaloneOnly)
            "SSH host must not be blank" -> AppI18n.t(StringKeys.Validation.SshHostRequired)
            "SSH port must be between 1 and 65535" -> AppI18n.t(StringKeys.Validation.SshPortRange)
            "SSH username must not be blank" -> AppI18n.t(StringKeys.Validation.SshUsernameRequired)
            "SSH password must not be blank" -> AppI18n.t(StringKeys.Validation.SshPasswordRequired)
            "SSH private key or private key path is required" ->
                AppI18n.t(StringKeys.Validation.SshPrivateKeyRequired)
            "Connect timeout must be greater than 0" ->
                AppI18n.t(
                    StringKeys.Validation.LabelGreaterThanZero,
                    AppI18n.t(StringKeys.ConnectionEditor.ConnectTimeoutLabel),
                )
            "Command timeout must be greater than 0" ->
                AppI18n.t(
                    StringKeys.Validation.LabelGreaterThanZero,
                    AppI18n.t(StringKeys.ConnectionEditor.CommandTimeoutLabel),
                )
            "Reconnect timeout must be greater than 0" ->
                AppI18n.t(
                    StringKeys.Validation.LabelGreaterThanZero,
                    AppI18n.t(StringKeys.ConnectionEditor.ReconnectTimeoutLabel),
                )
            "SSH connect timeout must be greater than 0" ->
                AppI18n.t(
                    StringKeys.Validation.LabelGreaterThanZero,
                    AppI18n.t(StringKeys.ConnectionEditor.SshConnectTimeoutLabel),
                )
            else -> message
        }
    }
}
