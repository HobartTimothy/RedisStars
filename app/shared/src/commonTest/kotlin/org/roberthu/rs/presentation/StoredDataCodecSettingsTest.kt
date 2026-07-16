package org.roberthu.rs.presentation

import org.roberthu.rs.domain.AppLanguage
import org.roberthu.rs.port.UserSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoredDataCodecSettingsTest {

    @Test
    fun settingsRoundTrip_includesLanguage() {
        val settings = UserSettings(
            darkMode = false,
            autoConnect = true,
            language = "zh-CN",
        )

        val decoded = StoredDataCodec.decodeSettings(StoredDataCodec.encodeSettings(settings))

        assertEquals(settings, decoded)
    }

    @Test
    fun legacySettingsWithoutLanguage_decodeWithNullLanguage() {
        val legacy = """
            {
              "darkMode": true,
              "autoConnect": false
            }
        """.trimIndent()

        val decoded = StoredDataCodec.decodeSettings(legacy)

        assertNull(decoded.language)
        assertEquals(true, decoded.darkMode)
    }

    @Test
    fun invalidLanguageTag_isPreservedForResolveOnLoad() {
        val settings = UserSettings(language = "not-a-locale")
        val decoded = StoredDataCodec.decodeSettings(StoredDataCodec.encodeSettings(settings))

        assertEquals("not-a-locale", decoded.language)
        assertEquals(
            AppLanguage.EnUS,
            AppLanguage.resolve(storedTag = decoded.language, systemLanguage = "en-US"),
        )
    }
}
