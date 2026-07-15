package org.roberthu.rs.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Material3 [NavigationRailItem] uses [ColorScheme.secondary] as the *selected* label color
 * (`ItemActiveLabelText`). It must contrast with [ColorScheme.surfaceContainer] (rail background).
 */
internal val RedisDarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = RedisColors.Primary,
        onPrimary = Color.White,
        background = RedisColors.Background,
        onBackground = Color(0xFFE0E0E0),
        surface = RedisColors.Background,
        onSurface = Color(0xFFE0E0E0),
        onSurfaceVariant = Color(0xFFB0B0B0),
        surfaceContainer = RedisColors.SurfaceContainer,
        surfaceContainerHigh = RedisColors.SurfaceContainer,
        surfaceContainerHighest = Color(0xFF333333),
        // Selected NavigationRail labels resolve to `secondary` — must not match surfaceContainer.
        secondary = Color(0xFFE0E0E0),
        onSecondary = Color(0xFF1E1E1E),
        // Keep a distinct selected indicator (existing purple-tint pill look).
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        error = Color(0xFFCF6679),
    )

internal val RedisLightColorScheme: ColorScheme =
    lightColorScheme(
        primary = RedisColors.Primary,
        onPrimary = Color.White,
        background = Color(0xFFF5F5F5),
        onBackground = Color(0xFF1E1E1E),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1E1E1E),
        onSurfaceVariant = Color(0xFF5C5C5C),
        surfaceContainer = Color(0xFFEEEEEE),
        surfaceContainerHigh = Color(0xFFE8E8E8),
        surfaceContainerHighest = Color(0xFFE0E0E0),
        secondary = Color(0xFF1E1E1E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        error = Color(0xFFB00020),
    )

/** Label color NavigationRail uses when an item is selected (Material token mapping). */
fun navigationRailSelectedLabelColor(scheme: ColorScheme): Color = scheme.secondary

fun navigationRailSelectedLabelContrastsWithRail(scheme: ColorScheme): Boolean =
    navigationRailSelectedLabelColor(scheme) != scheme.surfaceContainer

@Composable
fun RedisTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) RedisDarkColorScheme else RedisLightColorScheme,
        content = content,
    )
}
