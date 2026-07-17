package org.roberthu.rs.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import org.roberthu.rs.theme.RedisTheme as LegacyRedisTheme

/**
 * Extended [RedisTheme] entry point that adds spacing, dimension, and motion
 * composition locals on top of the existing Material3 colour/typography theme.
 *
 * Existing call sites using `org.roberthu.rs.theme.RedisTheme` continue to work.
 * New code should access tokens via [RedisTheme.spacing], [RedisTheme.dimensions],
 * and [RedisTheme.motion].
 *
 * Usage:
 * ```kotlin
 * RedisTheme(darkTheme = true) {
 *     val spacing = RedisTheme.spacing
 *     val dim = RedisTheme.dimensions
 * }
 * ```
 */
@Composable
fun RedisTheme(
    darkTheme: Boolean = true,
    spacing: RedisSpacing = RedisSpacing(),
    dimensions: RedisDimensions = RedisDimensions(),
    motion: RedisMotion = RedisMotion(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRedisSpacing provides spacing,
        LocalRedisDimensions provides dimensions,
        LocalRedisMotion provides motion,
    ) {
        LegacyRedisTheme(darkTheme = darkTheme, content = content)
    }
}

/**
 * Accessor object for design tokens inside any composable under [RedisTheme].
 */
object RedisTheme {
    val spacing: RedisSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalRedisSpacing.current

    val dimensions: RedisDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalRedisDimensions.current

    val motion: RedisMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalRedisMotion.current
}
