package org.roberthu.rs.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.roberthu.rs.shell.ShellDestination
import org.roberthu.rs.shell.ShellUiState

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

    @Test
    fun darkScheme_selectedNavLabelContrastsWithRailBackground() {
        assertTrue(navigationRailSelectedLabelContrastsWithRail(RedisDarkColorScheme))
        assertNotEquals(
            RedisDarkColorScheme.surfaceContainer,
            navigationRailSelectedLabelColor(RedisDarkColorScheme),
        )
    }

    @Test
    fun lightScheme_selectedNavLabelContrastsWithRailBackground() {
        assertTrue(navigationRailSelectedLabelContrastsWithRail(RedisLightColorScheme))
        assertNotEquals(
            RedisLightColorScheme.surfaceContainer,
            navigationRailSelectedLabelColor(RedisLightColorScheme),
        )
    }

    @Test
    fun surfaceTintIsTransparentToAvoidPrimaryWashOnElevatedSurfaces() {
        assertEquals(Color.Transparent, RedisDarkColorScheme.surfaceTint)
        assertEquals(Color.Transparent, RedisLightColorScheme.surfaceTint)
    }

    @Test
    fun switchingSelection_preservesAllPrimaryLabels() {
        // Regression: selection must not drop label content (labels are destination.label).
        var state = ShellUiState()
        ShellDestination.primaryDestinations.forEach { item ->
            state = state.navigateTo(item)
            assertEquals(item.label, state.destination.label)
            ShellDestination.primaryDestinations.forEach { other ->
                assertTrue(other.label.isNotBlank())
            }
        }
    }
}
