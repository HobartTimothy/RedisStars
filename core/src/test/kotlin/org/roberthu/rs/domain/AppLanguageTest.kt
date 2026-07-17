package org.roberthu.rs.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLanguageTest {

    @Test
    fun fromSystemLanguage_chineseVariants_useZhCN() {
        assertEquals(AppLanguage.ZhCN, AppLanguage.fromSystemLanguage("zh"))
        assertEquals(AppLanguage.ZhCN, AppLanguage.fromSystemLanguage("zh-CN"))
        assertEquals(AppLanguage.ZhCN, AppLanguage.fromSystemLanguage("zh-Hans-CN"))
        assertEquals(AppLanguage.ZhCN, AppLanguage.fromSystemLanguage("ZH-TW"))
    }

    @Test
    fun fromSystemLanguage_nonChinese_useEnUS() {
        assertEquals(AppLanguage.EnUS, AppLanguage.fromSystemLanguage("en"))
        assertEquals(AppLanguage.EnUS, AppLanguage.fromSystemLanguage("en-US"))
        assertEquals(AppLanguage.EnUS, AppLanguage.fromSystemLanguage("ja-JP"))
        assertEquals(AppLanguage.EnUS, AppLanguage.fromSystemLanguage(""))
    }

    @Test
    fun resolve_prefersValidStoredLanguage() {
        assertEquals(
            AppLanguage.ZhCN,
            AppLanguage.resolve(storedTag = "zh-CN", systemLanguage = "en-US"),
        )
        assertEquals(
            AppLanguage.EnUS,
            AppLanguage.resolve(storedTag = "en-US", systemLanguage = "zh-CN"),
        )
    }

    @Test
    fun resolve_missingOrInvalidStored_fallsBackToSystem() {
        assertEquals(
            AppLanguage.ZhCN,
            AppLanguage.resolve(storedTag = null, systemLanguage = "zh-CN"),
        )
        assertEquals(
            AppLanguage.EnUS,
            AppLanguage.resolve(storedTag = null, systemLanguage = "fr-FR"),
        )
        assertEquals(
            AppLanguage.EnUS,
            AppLanguage.resolve(storedTag = "not-a-locale", systemLanguage = "en-GB"),
        )
        assertEquals(
            AppLanguage.ZhCN,
            AppLanguage.resolve(storedTag = "bogus", systemLanguage = "zh"),
        )
    }

    @Test
    fun fromTagOrNull_acceptsCanonicalTags() {
        assertEquals(AppLanguage.ZhCN, AppLanguage.fromTagOrNull("zh-CN"))
        assertEquals(AppLanguage.EnUS, AppLanguage.fromTagOrNull("en-US"))
        assertNull(AppLanguage.fromTagOrNull(null))
        assertNull(AppLanguage.fromTagOrNull(""))
        assertNull(AppLanguage.fromTagOrNull("de-DE"))
    }
}
