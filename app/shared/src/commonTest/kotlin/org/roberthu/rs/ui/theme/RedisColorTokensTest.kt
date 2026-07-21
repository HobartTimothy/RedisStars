package org.roberthu.rs.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RedisColorTokensTest {
    @Test
    fun darkTokens_selectedSurfaceUsesBrandTint_notPurple() {
        val tokens = darkRedisColorTokens()
        assertEquals(tokens.brandContainer, tokens.selectedSurface)
        assertNotEquals(androidx.compose.ui.graphics.Color(0xFF4A4458), tokens.selectedSurface)
    }

    @Test
    fun materialScheme_mapsSelectedContainerToBrandTokens() {
        val scheme = darkRedisColorTokens().toMaterialColorScheme()
        val tokens = darkRedisColorTokens()
        assertEquals(tokens.selectedSurface, scheme.secondaryContainer)
        assertEquals(tokens.onSelectedSurface, scheme.onSecondaryContainer)
    }

    @Test
    fun lightAndDark_tokens_shareBrandAccent() {
        assertEquals(darkRedisColorTokens().brandAccent, lightRedisColorTokens().brandAccent)
    }

    @Test
    fun darkTokens_textPrimaryHasReasonableContrastAgainstPane() {
        val tokens = darkRedisColorTokens()
        assertTrue(tokens.textPrimary.luminance() > tokens.paneSurface.luminance())
    }
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
