package org.roberthu.rs.presentation

import org.roberthu.rs.domain.ConnectionBrowserOptions
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.ConnectionTagColor
import org.roberthu.rs.domain.DatabaseFilterMode
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.KeyListViewMode
import org.roberthu.rs.domain.SshAuthMethod
import org.roberthu.rs.domain.SshTunnelOptions
import org.roberthu.rs.domain.TimeoutOptions
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

    @Test
    fun legacyArrayStillDecodesProfiles() {
        val legacy = """[{"id":"c1","name":"Legacy","mode":"Standalone","host":"localhost","port":6379,"database":0}]"""
        val decoded = StoredDataCodec.decodeConnections(legacy)
        assertEquals(1, decoded.profiles.size)
        assertEquals("c1", decoded.profiles.single().id)
        assertTrue(decoded.groups.isEmpty())
    }

    @Test
    fun legacyProfileWithoutBrowserFieldDecodesWithDefaults() {
        val legacy = """
            [{
                "id":"c1",
                "name":"Legacy",
                "mode":"Standalone",
                "host":"localhost",
                "port":6379,
                "database":0,
                "timeouts":{"connectMs":5000,"commandMs":5000,"reconnectMs":30000}
            }]
        """.trimIndent()

        val decoded = StoredDataCodec.decodeConnections(legacy).profiles.single()

        assertEquals(ConnectionBrowserOptions(), decoded.browser)
        // Legacy connect/command timeouts (pre-60s default) are preserved verbatim, not migrated.
        assertEquals(TimeoutOptions(connectMs = 5000, commandMs = 5000, reconnectMs = 30_000), decoded.timeouts)
    }

    @Test
    fun browserOptionsRoundTripThroughEncodeDecode() {
        val profile = ConnectionProfile(
            id = "c1",
            name = "Local",
            mode = DeploymentMode.Standalone,
            host = "127.0.0.1",
            browser = ConnectionBrowserOptions(
                keyPattern = "session:*",
                keySeparator = "/",
                keyListView = KeyListViewMode.Flat,
                keyLoadBatchSize = 500,
                databaseFilterMode = DatabaseFilterMode.HideSpecified,
                databaseFilterValues = listOf(0, 1, 2),
                tagColor = ConnectionTagColor.Blue,
            ),
        )

        val decoded = StoredDataCodec.decodeProfiles(
            StoredDataCodec.encodeProfiles(listOf(profile), rememberPasswords = true),
        ).single()

        assertEquals(profile.browser, decoded.browser)
    }

    @Test
    fun newDocumentWithGroupsRoundTrips() {
        val group = ConnectionGroup(id = "g1", name = "Dev", order = 0, expanded = true)
        val profile = ConnectionProfile(
            id = "c1",
            name = "Local",
            mode = DeploymentMode.Standalone,
            host = "127.0.0.1",
            groupId = "g1",
        )
        val encoded = StoredDataCodec.encodeConnections(
            StoredConnections(groups = listOf(group), profiles = listOf(profile)),
            rememberPasswords = false,
        )
        val decoded = StoredDataCodec.decodeConnections(encoded)
        assertEquals(listOf("g1"), decoded.groups.map { it.id })
        assertEquals("g1", decoded.profiles.single().groupId)
    }
}
