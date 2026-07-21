package org.roberthu.rs.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Brand accent — Redis red. Shared across light and dark themes. */
val RedisBrandRed = Color(0xFFE53935)

/**
 * Semantic application colors. Access via [RedisTheme.colors] inside composables.
 *
 * Do not scatter raw [Color] values in feature screens; use these tokens or
 * [MaterialTheme.colorScheme] mappings derived from them.
 */
@Immutable
data class RedisColorTokens(
    val brandAccent: Color,
    val onBrandAccent: Color,
    val brandContainer: Color,
    val onBrandContainer: Color,
    val appBackground: Color,
    val paneSurface: Color,
    val toolbarSurface: Color,
    val selectedSurface: Color,
    val onSelectedSurface: Color,
    val hoverSurface: Color,
    val pressedSurface: Color,
    val focusBorder: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val iconPrimary: Color,
    val iconSecondary: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
    val connectionOk: Color,
)

fun darkRedisColorTokens(): RedisColorTokens = RedisColorTokens(
    brandAccent = RedisBrandRed,
    onBrandAccent = Color(0xFFFFFFFF),
    brandContainer = Color(0xFF4A2A2A),
    onBrandContainer = Color(0xFFFFDAD6),
    appBackground = Color(0xFF1E1E1E),
    paneSurface = Color(0xFF252525),
    toolbarSurface = Color(0xFF2A2A2A),
    selectedSurface = Color(0xFF4A2A2A),
    onSelectedSurface = Color(0xFFFFDAD6),
    hoverSurface = Color(0x14FFFFFF),
    pressedSurface = Color(0x1FFFFFFF),
    focusBorder = Color(0xFFE53935),
    divider = Color(0xFF3A3A3A),
    textPrimary = Color(0xFFE8E8E8),
    textSecondary = Color(0xFFB0B0B0),
    textDisabled = Color(0xFF757575),
    iconPrimary = Color(0xFFE0E0E0),
    iconSecondary = Color(0xFF9E9E9E),
    success = Color(0xFF4CAF50),
    warning = Color(0xFFFFB74D),
    error = Color(0xFFCF6679),
    info = Color(0xFF64B5F6),
    connectionOk = Color(0xFF4CAF50),
)

fun lightRedisColorTokens(): RedisColorTokens = RedisColorTokens(
    brandAccent = RedisBrandRed,
    onBrandAccent = Color(0xFFFFFFFF),
    brandContainer = Color(0xFFFFDAD6),
    onBrandContainer = Color(0xFF5C1A18),
    appBackground = Color(0xFFF3F3F3),
    paneSurface = Color(0xFFFFFFFF),
    toolbarSurface = Color(0xFFF8F8F8),
    selectedSurface = Color(0xFFFFDAD6),
    onSelectedSurface = Color(0xFF5C1A18),
    hoverSurface = Color(0x0A000000),
    pressedSurface = Color(0x14000000),
    focusBorder = Color(0xFFC62828),
    divider = Color(0xFFE0E0E0),
    textPrimary = Color(0xFF1E1E1E),
    textSecondary = Color(0xFF5C5C5C),
    textDisabled = Color(0xFF9E9E9E),
    iconPrimary = Color(0xFF1E1E1E),
    iconSecondary = Color(0xFF757575),
    success = Color(0xFF2E7D32),
    warning = Color(0xFFED6C02),
    error = Color(0xFFB00020),
    info = Color(0xFF0288D1),
    connectionOk = Color(0xFF2E7D32),
)

internal fun RedisColorTokens.toMaterialColorScheme(): ColorScheme {
    val tokens = this
    return if (tokens.appBackground.luminance() < 0.5f) {
        darkColorScheme(
            primary = tokens.brandAccent,
            onPrimary = tokens.onBrandAccent,
            primaryContainer = tokens.brandContainer,
            onPrimaryContainer = tokens.onBrandContainer,
            background = tokens.appBackground,
            onBackground = tokens.textPrimary,
            surface = tokens.appBackground,
            onSurface = tokens.textPrimary,
            onSurfaceVariant = tokens.textSecondary,
            surfaceTint = Color.Transparent,
            surfaceContainer = tokens.toolbarSurface,
            surfaceContainerHigh = tokens.paneSurface,
            surfaceContainerHighest = Color(0xFF333333),
            secondary = tokens.textPrimary,
            onSecondary = tokens.appBackground,
            secondaryContainer = tokens.selectedSurface,
            onSecondaryContainer = tokens.onSelectedSurface,
            error = tokens.error,
            scrim = Color.Black.copy(alpha = 0.52f),
            outline = tokens.divider,
            outlineVariant = tokens.divider,
        )
    } else {
        lightColorScheme(
            primary = tokens.brandAccent,
            onPrimary = tokens.onBrandAccent,
            primaryContainer = tokens.brandContainer,
            onPrimaryContainer = tokens.onBrandContainer,
            background = tokens.appBackground,
            onBackground = tokens.textPrimary,
            surface = tokens.paneSurface,
            onSurface = tokens.textPrimary,
            onSurfaceVariant = tokens.textSecondary,
            surfaceTint = Color.Transparent,
            surfaceContainer = tokens.toolbarSurface,
            surfaceContainerHigh = Color(0xFFECECEC),
            surfaceContainerHighest = Color(0xFFE0E0E0),
            secondary = tokens.textPrimary,
            onSecondary = Color.White,
            secondaryContainer = tokens.selectedSurface,
            onSecondaryContainer = tokens.onBrandContainer,
            error = tokens.error,
            scrim = Color.Black.copy(alpha = 0.40f),
            outline = tokens.divider,
            outlineVariant = tokens.divider,
        )
    }
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue

internal val LocalRedisColorTokens = staticCompositionLocalOf { darkRedisColorTokens() }
