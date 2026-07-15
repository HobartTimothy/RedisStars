package org.roberthu.rs.redis

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Parameters
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.password.PasswordUtils
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.SshAuthMethod
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Opens an SSH local port forward:
 * `127.0.0.1:<ephemeral>` → SSH server → Redis [ConnectionProfile.host]:[ConnectionProfile.port].
 */
class SshLocalTunnel private constructor(
    private val ssh: SSHClient,
    private val serverSocket: ServerSocket,
    private val forwarderThread: Thread,
) : AutoCloseable {
    val localHost: String = "127.0.0.1"
    val localPort: Int = serverSocket.localPort

    private val closed = AtomicBoolean(false)

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { serverSocket.close() }
        runCatching { ssh.disconnect() }
        runCatching { ssh.close() }
        runCatching { forwarderThread.join(2_000) }
    }

    companion object {
        fun openIfNeeded(profile: ConnectionProfile): SshLocalTunnel? {
            if (!profile.ssh.enabled) return null
            if (profile.mode != DeploymentMode.Standalone) {
                throw RedisError.Validation("SSH tunnel is only supported in Standalone mode")
            }
            return open(profile)
        }

        fun open(profile: ConnectionProfile): SshLocalTunnel {
            val sshOptions = profile.ssh
            val ssh = SSHClient()
            // Desktop Redis clients commonly skip interactive host-key prompts.
            // Known hosts are still loaded when present so saved keys are preferred.
            runCatching { ssh.loadKnownHosts() }
            ssh.addHostKeyVerifier(PromiscuousVerifier())
            ssh.connectTimeout = sshOptions.connectTimeoutMs
                .coerceAtLeast(1)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            ssh.timeout = ssh.connectTimeout

            try {
                ssh.connect(sshOptions.host.trim(), sshOptions.port)
                authenticate(ssh, profile)
                val serverSocket = ServerSocket()
                serverSocket.reuseAddress = true
                serverSocket.bind(InetSocketAddress("127.0.0.1", 0))
                val params = Parameters(
                    "127.0.0.1",
                    serverSocket.localPort,
                    profile.host.trim(),
                    profile.port,
                )
                val forwarder = ssh.newLocalPortForwarder(params, serverSocket)
                val forwarderThread = thread(
                    name = "ssh-tunnel-${profile.id}",
                    isDaemon = true,
                ) {
                    runCatching { forwarder.listen() }
                }
                return SshLocalTunnel(ssh, serverSocket, forwarderThread)
            } catch (failure: Throwable) {
                runCatching { ssh.disconnect() }
                runCatching { ssh.close() }
                throw RedisError.Network(
                    failure.message?.takeIf { it.isNotBlank() } ?: "SSH tunnel failed",
                )
            }
        }

        private fun authenticate(ssh: SSHClient, profile: ConnectionProfile) {
            val sshOptions = profile.ssh
            val username = sshOptions.username.trim()
            when (sshOptions.authMethod) {
                SshAuthMethod.Password -> {
                    val password = sshOptions.password
                        ?: throw RedisError.Validation("SSH password must not be blank")
                    ssh.authPassword(username, password)
                }
                SshAuthMethod.PrivateKey -> {
                    val passphrase = sshOptions.privateKeyPassphrase
                    val passwordFinder = passphrase
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { PasswordUtils.createOneOff(it.toCharArray()) }
                    val keyPath = sshOptions.privateKeyPath?.trim().orEmpty()
                    val keyPem = sshOptions.privateKey.orEmpty()
                    val keyProvider: KeyProvider = when {
                        keyPath.isNotEmpty() -> ssh.loadKeys(keyPath, passwordFinder)
                        keyPem.isNotEmpty() -> ssh.loadKeys(keyPem, null, passwordFinder)
                        else -> throw RedisError.Validation(
                            "SSH private key or private key path is required",
                        )
                    }
                    ssh.authPublickey(username, keyProvider)
                }
            }
        }
    }
}
