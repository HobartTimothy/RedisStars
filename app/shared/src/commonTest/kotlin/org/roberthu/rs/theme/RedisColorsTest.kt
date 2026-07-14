package org.roberthu.rs.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class RedisColorsTest {
    @Test
    fun connectionIndicator_connected_isGreen() {
        assertEquals(RedisColors.ConnectionOk, connectionIndicatorColor(true, Color.White))
    }

    @Test
    fun connectionIndicator_disconnected_usesOnSurfaceAlpha() {
        val onSurface = Color.White
        val actual = connectionIndicatorColor(false, onSurface)
        assertEquals(onSurface.copy(alpha = 0.38f), actual)
    }
}
