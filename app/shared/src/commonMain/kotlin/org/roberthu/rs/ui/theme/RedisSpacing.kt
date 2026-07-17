package org.roberthu.rs.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens based on a 4dp grid.
 *
 * Use via [RedisTheme.spacing] inside composables.
 */
@Immutable
data class RedisSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

internal val LocalRedisSpacing = staticCompositionLocalOf { RedisSpacing() }
