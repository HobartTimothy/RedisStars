package org.roberthu.rs.presentation

import org.roberthu.rs.domain.DeploymentMode

data class ParsedRedisConnectionUrl(
    val host: String,
    val port: Int,
    val database: Int,
    val username: String?,
    val password: String?,
    val tlsEnabled: Boolean,
) {
    fun applyTo(form: ConnectionFormState): ConnectionFormState = form.copy(
        deploymentMode = DeploymentMode.Standalone,
        host = host,
        port = port.toString(),
        database = database.toString(),
        username = username.orEmpty(),
        password = password.orEmpty(),
        tlsEnabled = tlsEnabled,
        verifyPeer = if (tlsEnabled) form.verifyPeer else form.verifyPeer,
    )
}

sealed class RedisConnectionUrlParseResult {
    abstract val error: String?

    data class Success(val value: ParsedRedisConnectionUrl) : RedisConnectionUrlParseResult() {
        override val error: String? = null
    }

    data class Failure(override val error: String) : RedisConnectionUrlParseResult()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    val valueOrNull: ParsedRedisConnectionUrl?
        get() = (this as? Success)?.value

    fun getOrThrow(): ParsedRedisConnectionUrl = when (this) {
        is Success -> value
        is Failure -> kotlin.error(error)
    }
}

object RedisConnectionUrlParser {
    fun parse(raw: String): RedisConnectionUrlParseResult {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return RedisConnectionUrlParseResult.Failure("URL must not be blank")
        }

        val schemeSeparator = trimmed.indexOf("://")
        if (schemeSeparator <= 0) {
            return RedisConnectionUrlParseResult.Failure("Invalid Redis URL format")
        }

        val scheme = trimmed.substring(0, schemeSeparator).lowercase()
        val tlsEnabled = when (scheme) {
            "redis" -> false
            "rediss" -> true
            else -> return RedisConnectionUrlParseResult.Failure("Unsupported URL scheme: $scheme")
        }

        val remainder = trimmed.substring(schemeSeparator + 3)
        if (remainder.isEmpty()) {
            return RedisConnectionUrlParseResult.Failure("Host is required")
        }

        // Reject sentinel/cluster style multi-host URLs.
        if (remainder.contains(",")) {
            return RedisConnectionUrlParseResult.Failure("Sentinel and Cluster URLs are not supported")
        }

        val (authority, path) = splitAuthorityAndPath(remainder)
        if (authority.isEmpty()) {
            return RedisConnectionUrlParseResult.Failure("Host is required")
        }

        val credentialsAndHost = splitCredentials(authority)
            ?: return RedisConnectionUrlParseResult.Failure("Invalid Redis URL format")

        val hostPort = credentialsAndHost.hostPort
        if (hostPort.isEmpty() || hostPort.startsWith(":")) {
            return RedisConnectionUrlParseResult.Failure("Host is required")
        }

        val (host, portRaw) = splitHostPort(hostPort)
            ?: return RedisConnectionUrlParseResult.Failure("Host is required")
        if (host.isBlank()) {
            return RedisConnectionUrlParseResult.Failure("Host is required")
        }

        val port = when {
            portRaw == null || portRaw.isEmpty() -> 6379
            else -> {
                val parsed = portRaw.toIntOrNull()
                    ?: return RedisConnectionUrlParseResult.Failure("Port must be a number")
                if (parsed !in 1..65535) {
                    return RedisConnectionUrlParseResult.Failure("Port must be between 1 and 65535")
                }
                parsed
            }
        }

        val database = parseDatabase(path)
            ?: return RedisConnectionUrlParseResult.Failure("Database must be a non-negative integer")

        return RedisConnectionUrlParseResult.Success(
            ParsedRedisConnectionUrl(
                host = host,
                port = port,
                database = database,
                username = credentialsAndHost.username,
                password = credentialsAndHost.password,
                tlsEnabled = tlsEnabled,
            ),
        )
    }

    private data class CredentialsAndHost(
        val username: String?,
        val password: String?,
        val hostPort: String,
    )

    private fun splitAuthorityAndPath(remainder: String): Pair<String, String> {
        val pathIndex = remainder.indexOf('/')
        val queryIndex = remainder.indexOf('?')
        val cut = when {
            pathIndex >= 0 && queryIndex >= 0 -> minOf(pathIndex, queryIndex)
            pathIndex >= 0 -> pathIndex
            queryIndex >= 0 -> queryIndex
            else -> -1
        }
        return if (cut < 0) {
            remainder to ""
        } else {
            remainder.substring(0, cut) to remainder.substring(cut)
        }
    }

    private fun splitCredentials(authority: String): CredentialsAndHost? {
        val at = authority.lastIndexOf('@')
        if (at < 0) {
            return CredentialsAndHost(null, null, authority)
        }
        val userInfo = authority.substring(0, at)
        val hostPort = authority.substring(at + 1)
        val colon = userInfo.indexOf(':')
        return if (colon < 0) {
            CredentialsAndHost(
                username = decode(userInfo).ifBlank { null },
                password = null,
                hostPort = hostPort,
            )
        } else {
            val user = decode(userInfo.substring(0, colon))
            val pass = decode(userInfo.substring(colon + 1))
            CredentialsAndHost(
                username = user.ifBlank { null },
                password = pass.ifEmpty { null },
                hostPort = hostPort,
            )
        }
    }

    private fun splitHostPort(hostPort: String): Pair<String, String?>? {
        if (hostPort.startsWith("[")) {
            val end = hostPort.indexOf(']')
            if (end < 0) return null
            val host = hostPort.substring(1, end)
            val rest = hostPort.substring(end + 1)
            val port = if (rest.startsWith(":")) rest.substring(1) else null
            return host to port
        }
        val colon = hostPort.lastIndexOf(':')
        return if (colon < 0) {
            hostPort to null
        } else {
            hostPort.substring(0, colon) to hostPort.substring(colon + 1)
        }
    }

    private fun parseDatabase(path: String): Int? {
        if (path.isEmpty() || path == "/") return 0
        val withoutQuery = path.substringBefore('?')
        val dbPart = withoutQuery.removePrefix("/").substringBefore('/')
        if (dbPart.isEmpty()) return 0
        val value = dbPart.toIntOrNull() ?: return null
        if (value < 0) return null
        return value
    }

    private fun decode(value: String): String {
        if (!value.contains('%')) return value
        return buildString(value.length) {
            var i = 0
            while (i < value.length) {
                val c = value[i]
                if (c == '%' && i + 2 < value.length) {
                    val hex = value.substring(i + 1, i + 3)
                    val decoded = hex.toIntOrNull(16)
                    if (decoded != null) {
                        append(decoded.toChar())
                        i += 3
                        continue
                    }
                }
                append(c)
                i++
            }
        }
    }
}
