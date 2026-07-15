package org.roberthu.rs.presentation

import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

class StoredDataCodecTest {
    @Test
    fun profilesDoNotPersistPasswordsByDefault() {
        val profile = ConnectionProfile(
            id = "local",
            name = "Local",
            mode = DeploymentMode.Standalone,
            host = "127.0.0.1",
            password = "do-not-store",
        )

        val encoded = StoredDataCodec.encodeProfiles(listOf(profile), rememberPasswords = false)
        val decoded = StoredDataCodec.decodeProfiles(encoded)

        assertFalse("do-not-store" in encoded)
        assertNull(decoded.single().password)
    }
}
