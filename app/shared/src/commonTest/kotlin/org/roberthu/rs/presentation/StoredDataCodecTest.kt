package org.roberthu.rs.presentation

import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.SshAuthMethod
import org.roberthu.rs.domain.SshTunnelOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoredDataCodecTest {
    @Test
    fun profilesDoNotPersistPasswordsByDefault() {
        val profile = ConnectionProfile(
            id = "local",
            name = "Local",
            mode = DeploymentMode.Standalone,
            host = "127.0.0.1",
            password = "do-not-store",
            ssh = SshTunnelOptions(
                enabled = true,
                host = "bastion",
                username = "ubuntu",
                authMethod = SshAuthMethod.Password,
                password = "ssh-secret",
                privateKey = "PRIVATE-KEY-MATERIAL",
                privateKeyPassphrase = "phrase",
            ),
        )

        val encoded = StoredDataCodec.encodeProfiles(listOf(profile), rememberPasswords = false)
        val decoded = StoredDataCodec.decodeProfiles(encoded)

        assertFalse("do-not-store" in encoded)
        assertFalse("ssh-secret" in encoded)
        assertFalse("PRIVATE-KEY-MATERIAL" in encoded)
        assertFalse("phrase" in encoded)
        assertNull(decoded.single().password)
        assertNull(decoded.single().ssh.password)
        assertNull(decoded.single().ssh.privateKey)
        assertNull(decoded.single().ssh.privateKeyPassphrase)
        assertTrue(decoded.single().ssh.enabled)
        assertEquals("bastion", decoded.single().ssh.host)
    }

    @Test
    fun profilesPersistSshSecretsWhenRememberPasswordsEnabled() {
        val profile = ConnectionProfile(
            id = "local",
            name = "Local",
            mode = DeploymentMode.Standalone,
            host = "127.0.0.1",
            ssh = SshTunnelOptions(
                enabled = true,
                host = "bastion",
                username = "ubuntu",
                password = "ssh-secret",
            ),
        )

        val decoded = StoredDataCodec.decodeProfiles(
            StoredDataCodec.encodeProfiles(listOf(profile), rememberPasswords = true),
        )

        assertEquals("ssh-secret", decoded.single().ssh.password)
    }
}
