package org.roberthu.rs.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion / animation duration tokens in milliseconds.
 *
 * Use via [RedisTheme.motion] inside composables.
 */
@Immutable
data class RedisMotion(
    /** Very short transitions: icon swaps, colour changes. */
    val fast: Int = 120,
    /** Standard UI transitions: sidebar collapse, panel appearance. */
    val normal: Int = 200,
    /** Larger layout changes. */
    val slow: Int = 300,
)

internal val LocalRedisMotion = staticCompositionLocalOf { RedisMotion() }
