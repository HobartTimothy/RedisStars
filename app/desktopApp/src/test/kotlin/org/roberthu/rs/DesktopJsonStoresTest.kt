package org.roberthu.rs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.SshAuthMethod
import org.roberthu.rs.domain.SshTunnelOptions
import org.roberthu.rs.port.UserSettings
import org.roberthu.rs.presentation.StoredDataCodec
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class DesktopJsonStoresTest {

    @Test
    fun settingsLoadCachesSubsequentReads() = runBlocking {
        val file = tempFile("settings.json")
        file.writeSettings(UserSettings(darkMode = false))
        val dispatcher = RecordingDispatcher()
        try {
            val store = DesktopJsonUserSettingsStore(file, dispatcher)

            assertEquals(UserSettings(darkMode = false), store.load())
            file.writeSettings(UserSettings(darkMode = true))
            assertEquals(UserSettings(darkMode = false), store.load())
            assertEquals(1, dispatcher.dispatchedOperations.get())
        } finally {
            dispatcher.shutdown()
        }
    }

    @Test
    fun settingsSaveUsesInjectedDispatcher() = runBlocking {
        val file = tempFile("settings.json")
        val dispatcher = RecordingDispatcher()
        try {
            val store = DesktopJsonUserSettingsStore(file, dispatcher)
            val callerThread = Thread.currentThread().name

            store.save(UserSettings(darkMode = false))

            assertTrue(dispatcher.dispatchedOperations.get() >= 1)
            assertTrue(dispatcher.executionThreadNames.any { it != callerThread })
        } finally {
            dispatcher.shutdown()
        }
    }

    @Test
    fun settingsCorruptedJsonFailsLoad() = runBlocking {
        val file = tempFile("settings.json")
        Files.writeString(file, "{not-json")
        val dispatcher = RecordingDispatcher()
        try {
            val store = DesktopJsonUserSettingsStore(file, dispatcher)

            try {
                store.load()
                fail("Expected corrupted settings JSON to fail")
            } catch (_: Exception) {
                // expected
            }
        } finally {
            dispatcher.shutdown()
        }
    }

    @Test
    fun settingsSaveFailureKeepsPreviousSnapshot() = runBlocking {
        val file = tempFile("settings.json")
        val dispatcher = RecordingDispatcher()
        val writes = AtomicInteger(0)
        try {
            val store = DesktopJsonUserSettingsStore(
                file = file,
                ioDispatcher = dispatcher,
                fileWriter = { path, content ->
                    if (writes.incrementAndGet() >= 2) {
                        error("disk full")
                    }
                    writeAtomically(path, content)
                },
            )

            store.save(UserSettings(darkMode = false))

            try {
                store.save(UserSettings(darkMode = true))
                fail("Expected save failure")
            } catch (_: IllegalStateException) {
                // expected
            }

            assertEquals(UserSettings(darkMode = false), store.load())
            assertEquals(UserSettings(darkMode = false), StoredDataCodec.decodeSettings(file.readText()))
        } finally {
            dispatcher.shutdown()
        }
    }

    @Test
    fun profilesCorruptedJsonFailsLoad() = runBlocking {
        val file = tempFile("profiles.json")
        Files.writeString(file, "{broken")
        val dispatcher = RecordingDispatcher()
        try {
            val store = profileStore(file, rememberPasswords = { true }, dispatcher)

            try {
                store.list()
                fail("Expected corrupted profiles JSON to fail")
            } catch (_: Exception) {
                // expected
            }
        } finally {
            dispatcher.shutdown()
        }
    }

    @Test
    fun profilesRememberPasswordsFalseDoesNotPersistSecrets() = runBlocking {
        val file = tempFile("profiles.json")
        val settingsFile = tempFile("settings.json")
        val dispatcher = RecordingDispatcher()
        try {
            val settingsStore = DesktopJsonUserSettingsStore(
                file = settingsFile,
                ioDispatcher = dispatcher,
            )
            settingsStore.save(UserSettings(rememberPasswords = false))

            val store = DesktopJsonConnectionProfileStore(
                file = file,
                rememberPasswords = settingsStore::rememberPasswordsEnabled,
                ioDispatcher = dispatcher,
            )
            val profile = profileWithSecrets()

            store.upsert(profile)

            val encoded = file.readText()
            assertFalse("redis-secret" in encoded)
            assertFalse("ssh-secret" in encoded)
            assertFalse("PRIVATE-KEY" in encoded)
            assertFalse("key-passphrase" in encoded)

            val listed = store.list().single()
            assertEquals("redis-secret", listed.password)
            assertEquals("ssh-secret", listed.ssh.password)
            assertEquals("PRIVATE-KEY", listed.ssh.privateKey)
            assertEquals("key-passphrase", listed.ssh.privateKeyPassphrase)
        } finally {
            dispatcher.shutdown()
        }
    }

    @Test
    fun profilesRememberPasswordsTruePersistsSecrets() = runBlocking {
        val file = tempFile("profiles.json")
        val dispatcher = RecordingDispatcher()
        try {
            val store = profileStore(file, rememberPasswords = { true }, dispatcher)
            val profile = profileWithSecrets()

            store.upsert(profile)

            val encoded = file.readText()
            assertTrue("redis-secret" in encoded)
            assertTrue("ssh-secret" in encoded)
            assertTrue("PRIVATE-KEY" in encoded)
            assertTrue("key-passphrase" in encoded)
        } finally {
            dispatcher.shutdown()
        }
    }

    @Test
    fun profilesSaveFailureKeepsPreviousSnapshot() = runBlocking {
        val file = tempFile("profiles.json")
        val writes = AtomicInteger(0)
        val dispatcher = RecordingDispatcher()
        try {
            val store = DesktopJsonConnectionProfileStore(
                file = file,
                rememberPasswords = { true },
                ioDispatcher = dispatcher,
                fileWriter = { path, content ->
                    if (writes.incrementAndGet() >= 2) {
                        error("disk full")
                    }
                    writeAtomically(path, content)
                },
            )
            val original = ConnectionProfile(
                id = "c1",
                name = "Original",
                mode = DeploymentMode.Standalone,
                host = "127.0.0.1",
            )
            val updated = original.copy(name = "Updated")

            store.upsert(original)

            try {
                store.upsert(updated)
                fail("Expected save failure")
            } catch (_: IllegalStateException) {
                // expected
            }

            assertEquals("Original", store.list().single().name)
            assertEquals("Original", StoredDataCodec.decodeProfiles(file.readText()).single().name)
        } finally {
            dispatcher.shutdown()
        }
    }

    @Test
    fun profilesConcurrentSavesRemainConsistent() = runTest {
        val file = tempFile("profiles.json")
        val dispatcher = RecordingDispatcher()
        try {
            val store = profileStore(file, rememberPasswords = { true }, dispatcher)

            val jobs = (1..20).map { index ->
                async {
                    store.upsert(
                        ConnectionProfile(
                            id = "c$index",
                            name = "Profile $index",
                            mode = DeploymentMode.Standalone,
                            host = "127.0.0.1",
                        ),
                    )
                }
            }
            jobs.awaitAll()

            val persisted = StoredDataCodec.decodeProfiles(file.readText())
            val listed = store.list()

            assertEquals(persisted.size, listed.size)
            assertEquals(persisted.map { it.id }.toSet(), listed.map { it.id }.toSet())
            assertEquals(listed.size, listed.map { it.id }.toSet().size)
        } finally {
            dispatcher.shutdown()
        }
    }

    @Test
    fun profilesDeleteGroupUpdatesCacheAndDisk() = runBlocking {
        val file = tempFile("profiles.json")
        val dispatcher = RecordingDispatcher()
        try {
            val store = profileStore(file, rememberPasswords = { false }, dispatcher)
            val group = ConnectionGroup(id = "g1", name = "Dev")
            val profile = ConnectionProfile(
                id = "c1",
                name = "Local",
                mode = DeploymentMode.Standalone,
                host = "127.0.0.1",
                groupId = "g1",
            )

            store.upsertGroup(group)
            store.upsert(profile)
            store.deleteGroup("g1")

            assertTrue(store.listGroups().isEmpty())
            assertNull(store.list().single().groupId)
            assertTrue(StoredDataCodec.decodeConnections(file.readText()).groups.isEmpty())
        } finally {
            dispatcher.shutdown()
        }
    }

    private fun profileStore(
        file: Path,
        rememberPasswords: () -> Boolean,
        ioDispatcher: RecordingDispatcher,
    ) = DesktopJsonConnectionProfileStore(
        file = file,
        rememberPasswords = rememberPasswords,
        ioDispatcher = ioDispatcher,
    )

    private fun profileWithSecrets() = ConnectionProfile(
        id = "c1",
        name = "Secure",
        mode = DeploymentMode.Standalone,
        host = "127.0.0.1",
        password = "redis-secret",
        ssh = SshTunnelOptions(
            enabled = true,
            host = "bastion",
            username = "ubuntu",
            authMethod = SshAuthMethod.Password,
            password = "ssh-secret",
            privateKey = "PRIVATE-KEY",
            privateKeyPassphrase = "key-passphrase",
        ),
    )

    private fun tempFile(name: String): Path {
        val directory = Files.createTempDirectory("redis-stars-store-test")
        return directory.resolve(name)
    }

    private fun Path.writeSettings(settings: UserSettings) {
        parent.toFile().mkdirs()
        writeAtomically(this, StoredDataCodec.encodeSettings(settings))
    }
}

private class RecordingDispatcher : CoroutineDispatcher() {
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "recording-io") }
    val dispatchedOperations = AtomicInteger(0)
    val executionThreadNames = mutableListOf<String>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        executor.execute {
            synchronized(executionThreadNames) {
                dispatchedOperations.incrementAndGet()
                executionThreadNames.add(Thread.currentThread().name)
            }
            block.run()
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
