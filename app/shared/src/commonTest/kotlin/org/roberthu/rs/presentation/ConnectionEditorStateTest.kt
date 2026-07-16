package org.roberthu.rs.presentation

import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.HostPort
import org.roberthu.rs.domain.TimeoutOptions
import org.roberthu.rs.domain.TlsOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionEditorStateTest {
    @Test
    fun from_profile_mapsStandaloneFieldsToFormStrings() {
        val profile = ConnectionProfile(
            id = "c1",
            name = "Local",
            mode = DeploymentMode.Standalone,
            host = "127.0.0.1",
            port = 6380,
            database = 2,
            username = "user",
            password = "secret",
            tls = TlsOptions(enabled = true, verifyPeer = false),
            timeouts = TimeoutOptions(1000, 2000, 3000),
            clientName = "app",
        )

        val form = ConnectionFormState.from(profile)

        assertEquals("Local", form.name)
        assertEquals(DeploymentMode.Standalone, form.deploymentMode)
        assertEquals("127.0.0.1", form.host)
        assertEquals("6380", form.port)
        assertEquals("2", form.database)
        assertEquals("user", form.username)
        assertEquals("secret", form.password)
        assertEquals("app", form.clientName)
        assertEquals("1000", form.connectTimeoutMs)
        assertEquals("2000", form.commandTimeoutMs)
        assertEquals("3000", form.reconnectTimeoutMs)
        assertTrue(form.tlsEnabled)
        assertFalse(form.verifyPeer)
    }

    @Test
    fun toConnectionProfile_standalone_succeeds() {
        val form = ConnectionFormState.defaults().copy(
            name = "Prod",
            host = "redis.example",
            port = "6379",
            database = "1",
            username = "u",
            password = "p",
        )

        val result = form.toConnectionProfile("id-1")

        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()
        assertEquals("id-1", profile.id)
        assertEquals("Prod", profile.name)
        assertEquals(DeploymentMode.Standalone, profile.mode)
        assertEquals("redis.example", profile.host)
        assertEquals(6379, profile.port)
        assertEquals(0, profile.database)
        assertEquals("u", profile.username)
        assertEquals("p", profile.password)
    }

    @Test
    fun toConnectionProfile_sentinel_succeeds() {
        val form = ConnectionFormState.defaults().copy(
            name = "Sentinel",
            deploymentMode = DeploymentMode.Sentinel,
            masterName = "mymaster",
            database = "3",
            sentinelNodes = listOf(
                HostPortFormState("n1", "s1.example", "26379"),
                HostPortFormState("n2", "s2.example", "26380"),
            ),
        )

        val profile = form.toConnectionProfile("s1").getOrThrow()

        assertEquals(DeploymentMode.Sentinel, profile.mode)
        assertEquals("mymaster", profile.masterName)
        assertEquals(0, profile.database)
        assertEquals(
            listOf(HostPort("s1.example", 26379), HostPort("s2.example", 26380)),
            profile.sentinelNodes,
        )
    }

    @Test
    fun toConnectionProfile_cluster_succeedsAndForcesDatabaseZero() {
        val form = ConnectionFormState.defaults().copy(
            name = "Cluster",
            deploymentMode = DeploymentMode.Cluster,
            database = "5",
            seedNodes = listOf(
                HostPortFormState("n1", "c1.example", "7000"),
            ),
        )

        val profile = form.toConnectionProfile("cl1").getOrThrow()

        assertEquals(DeploymentMode.Cluster, profile.mode)
        assertEquals(0, profile.database)
        assertEquals(listOf(HostPort("c1.example", 7000)), profile.seedNodes)
    }

    @Test
    fun toConnectionProfile_emptyPort_reportsFieldError() {
        val form = ConnectionFormState.defaults().copy(port = "")

        val result = form.toConnectionProfile("id")

        assertTrue(result.isFailure)
        assertEquals("Port is required", result.fieldErrors["port"])
    }

    @Test
    fun toConnectionProfile_nonNumericPort_reportsFieldError() {
        val form = ConnectionFormState.defaults().copy(port = "abc")

        val result = form.toConnectionProfile("id")

        assertTrue(result.isFailure)
        assertEquals("Port must be a number", result.fieldErrors["port"])
    }

    @Test
    fun toConnectionProfile_portOutOfRange_reportsFieldError() {
        val form = ConnectionFormState.defaults().copy(port = "70000")

        val result = form.toConnectionProfile("id")

        assertTrue(result.isFailure)
        assertEquals("Port must be between 1 and 65535", result.fieldErrors["port"])
    }

    @Test
    fun toConnectionProfile_ignoresFormDatabase_alwaysUsesZero() {
        val form = ConnectionFormState.defaults().copy(
            name = "Local",
            database = "x",
        )

        val profile = form.toConnectionProfile("id").getOrThrow()
        assertEquals(0, profile.database)
    }

    @Test
    fun toConnectionProfile_clusterDatabaseForceZero_evenWhenFormSaysOtherwise() {
        val form = ConnectionFormState.defaults().copy(
            name = "C",
            deploymentMode = DeploymentMode.Cluster,
            database = "9",
            seedNodes = listOf(HostPortFormState("n1", "host", "6379")),
        )

        val profile = form.toConnectionProfile("id").getOrThrow()
        assertEquals(0, profile.database)
    }

    @Test
    fun toConnectionProfile_timeoutMustBePositive() {
        val form = ConnectionFormState.defaults().copy(connectTimeoutMs = "0")

        val result = form.toConnectionProfile("id")

        assertTrue(result.isFailure)
        assertNotNull(result.fieldErrors["connectTimeoutMs"])
    }

    @Test
    fun toConnectionProfile_sentinelMissingNodes_reportsError() {
        val form = ConnectionFormState.defaults().copy(
            name = "S",
            deploymentMode = DeploymentMode.Sentinel,
            masterName = "mymaster",
            sentinelNodes = emptyList(),
        )

        val result = form.toConnectionProfile("id")

        assertTrue(result.isFailure)
        assertTrue(
            result.fieldErrors.containsKey("sentinelNodes") ||
                result.globalError?.contains("sentinel", ignoreCase = true) == true ||
                result.domainErrors.any { it.contains("sentinel", ignoreCase = true) },
        )
    }

    @Test
    fun toConnectionProfile_clusterMissingNodes_reportsError() {
        val form = ConnectionFormState.defaults().copy(
            name = "C",
            deploymentMode = DeploymentMode.Cluster,
            seedNodes = emptyList(),
        )

        val result = form.toConnectionProfile("id")

        assertTrue(result.isFailure)
        assertTrue(
            result.fieldErrors.containsKey("seedNodes") ||
                result.globalError?.contains("seed", ignoreCase = true) == true ||
                result.domainErrors.any { it.contains("seed", ignoreCase = true) },
        )
    }

    @Test
    fun isDirty_falseWhenFormEqualsInitial() {
        val form = ConnectionFormState.defaults()
        val state = ConnectionEditorUiState(
            mode = ConnectionEditorMode.Create,
            profileId = "connection-1",
            selectedSection = ConnectionEditorSection.General,
            initialForm = form,
            form = form,
        )

        assertFalse(state.isDirty)
    }

    @Test
    fun isDirty_trueWhenFormDiffersFromInitial() {
        val initial = ConnectionFormState.defaults()
        val state = ConnectionEditorUiState(
            mode = ConnectionEditorMode.Create,
            profileId = "connection-1",
            selectedSection = ConnectionEditorSection.General,
            initialForm = initial,
            form = initial.copy(name = "Changed"),
        )

        assertTrue(state.isDirty)
    }

    @Test
    fun blankUsernameAndPasswordBecomeNull() {
        val form = ConnectionFormState.defaults().copy(
            username = "  ",
            password = "",
        )

        val profile = form.toConnectionProfile("id").getOrThrow()

        assertNull(profile.username)
        assertNull(profile.password)
    }

    @Test
    fun sshTunnelStandaloneFormConverts() {
        val form = ConnectionFormState.defaults().copy(
            sshEnabled = true,
            sshHost = "bastion.example",
            sshPort = "22",
            sshUsername = "ubuntu",
            sshAuthMethod = org.roberthu.rs.domain.SshAuthMethod.Password,
            sshPassword = "secret",
            sshConnectTimeoutMs = "8000",
        )

        val profile = form.toConnectionProfile("id").getOrThrow()

        assertTrue(profile.ssh.enabled)
        assertEquals("bastion.example", profile.ssh.host)
        assertEquals(22, profile.ssh.port)
        assertEquals("ubuntu", profile.ssh.username)
        assertEquals("secret", profile.ssh.password)
        assertEquals(8000, profile.ssh.connectTimeoutMs)
    }
}
