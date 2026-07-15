package org.roberthu.rs.redis

import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.SocketOptions
import io.lettuce.core.api.StatefulConnection
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.codec.RedisCodec
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.RedisError
import java.time.Duration

sealed class LettuceClientHandle : AutoCloseable {
    abstract val mode: DeploymentMode
    abstract fun <K, V> connect(codec: RedisCodec<K, V>): StatefulConnection<K, V>

    class Standard internal constructor(
        override val mode: DeploymentMode,
        private val client: RedisClient,
        private val tunnel: SshLocalTunnel? = null,
    ) : LettuceClientHandle() {
        override fun <K, V> connect(codec: RedisCodec<K, V>): StatefulConnection<K, V> =
            client.connect(codec)

        override fun close() {
            runCatching { client.shutdown() }
            tunnel.closeQuietly()
        }
    }

    class Cluster internal constructor(
        private val client: RedisClusterClient,
    ) : LettuceClientHandle() {
        override val mode: DeploymentMode = DeploymentMode.Cluster

        override fun <K, V> connect(codec: RedisCodec<K, V>): StatefulConnection<K, V> =
            client.connect(codec)

        override fun close() = client.shutdown()
    }
}

class LettuceClientFactory {
    fun create(profile: ConnectionProfile): LettuceClientHandle {
        val validationErrors = profile.validate()
        if (validationErrors.isNotEmpty()) {
            throw RedisError.Validation(validationErrors.joinToString("; "))
        }

        val tunnel = SshLocalTunnel.openIfNeeded(profile)
        val effectiveHost = tunnel?.localHost ?: profile.host
        val effectivePort = tunnel?.localPort ?: profile.port

        val socketOptions = SocketOptions.builder()
            .connectTimeout(Duration.ofMillis(profile.timeouts.connectMs))
            .build()
        return try {
            when (profile.mode) {
                DeploymentMode.Standalone -> {
                    val client = RedisClient.create(
                        redisUri(profile, effectiveHost, effectivePort, profile.database),
                    )
                    client.setOptions(
                        ClientOptions.builder()
                            .autoReconnect(false)
                            .socketOptions(socketOptions)
                            .build(),
                    )
                    LettuceClientHandle.Standard(profile.mode, client, tunnel)
                }

                DeploymentMode.Sentinel -> {
                    val client = RedisClient.create(sentinelUri(profile))
                    client.setOptions(
                        ClientOptions.builder()
                            .autoReconnect(false)
                            .socketOptions(socketOptions)
                            .build(),
                    )
                    LettuceClientHandle.Standard(profile.mode, client)
                }

                DeploymentMode.Cluster -> {
                    val client = RedisClusterClient.create(profile.seedNodes.map { seed ->
                        redisUri(profile, seed.host, seed.port, database = 0)
                    })
                    client.setOptions(
                        ClusterClientOptions.builder()
                            .autoReconnect(false)
                            .socketOptions(socketOptions)
                            .build(),
                    )
                    LettuceClientHandle.Cluster(client)
                }
            }
        } catch (failure: Throwable) {
            tunnel.closeQuietly()
            throw failure
        }
    }

    private fun sentinelUri(profile: ConnectionProfile): RedisURI {
        val first = profile.sentinelNodes.first()
        val builder = RedisURI.Builder.sentinel(first.host, first.port, profile.masterName)
        profile.sentinelNodes.drop(1).forEach { sentinel ->
            builder.withSentinel(sentinel.host, sentinel.port)
        }
        return configure(builder, profile, profile.database).build()
    }

    private fun redisUri(
        profile: ConnectionProfile,
        host: String,
        port: Int,
        database: Int,
    ): RedisURI = configure(
        RedisURI.Builder.redis(host, port),
        profile,
        database,
    ).build()

    private fun configure(
        builder: RedisURI.Builder,
        profile: ConnectionProfile,
        database: Int,
    ): RedisURI.Builder {
        val username = profile.username
        val password = profile.password
        builder
            .withDatabase(database)
            .withSsl(profile.tls.enabled)
            .withVerifyPeer(profile.tls.verifyPeer)
            .withTimeout(Duration.ofMillis(profile.timeouts.commandMs))
        when {
            username != null -> builder.withAuthentication(
                username,
                password?.toCharArray() ?: CharArray(0),
            )
            password != null -> builder.withPassword(password.toCharArray())
        }
        profile.clientName?.let(builder::withClientName)
        return builder
    }
}

private fun SshLocalTunnel?.closeQuietly() {
    try {
        this?.close()
    } catch (_: Throwable) {
        // Cleanup must not mask the original failure.
    }
}
