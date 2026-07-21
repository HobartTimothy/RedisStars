package org.roberthu.rs.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Single application-level theme entry point.
 *
 * Provides Material 3 [ColorScheme], [Typography], [Shapes], and Redis design tokens
 * ([colors], [spacing], [dimensions], [motion], [typography], [shapes]).
 */
@Composable
fun RedisTheme(
    darkTheme: Boolean = true,
    colorTokens: RedisColorTokens = if (darkTheme) darkRedisColorTokens() else lightRedisColorTokens(),
    spacing: RedisSpacing = RedisSpacing(),
    dimensions: RedisDimensions = RedisDimensions(),
    motion: RedisMotion = RedisMotion(),
    typographyTokens: RedisTypographyTokens = createRedisTypographyTokens(),
    shapeTokens: RedisShapeTokens = RedisShapeTokens(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRedisColorTokens provides colorTokens,
        LocalRedisSpacing provides spacing,
        LocalRedisDimensions provides dimensions,
        LocalRedisMotion provides motion,
        LocalRedisTypography provides typographyTokens,
        LocalRedisShapes provides shapeTokens,
    ) {
        MaterialTheme(
            colorScheme = colorTokens.toMaterialColorScheme(),
            typography = createRedisMaterialTypography(),
            shapes = createRedisMaterialShapes(),
            content = content,
        )
    }
}

/** Accessor object for design tokens inside any composable under [RedisTheme]. */
object RedisTheme {
    val colors: RedisColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalRedisColorTokens.current

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

    val typography: RedisTypographyTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalRedisTypography.current

    val shapes: RedisShapeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalRedisShapes.current
}
