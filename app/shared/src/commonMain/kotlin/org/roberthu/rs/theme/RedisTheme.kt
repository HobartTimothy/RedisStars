package org.roberthu.rs.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RedisDarkColorScheme = darkColorScheme(
    primary = RedisColors.Primary,
    onPrimary = Color.White,
    background = RedisColors.Background,
    onBackground = Color(0xFFE0E0E0),
    surface = RedisColors.Background,
    onSurface = Color(0xFFE0E0E0),
    surfaceContainer = RedisColors.SurfaceContainer,
    surfaceContainerHigh = RedisColors.SurfaceContainer,
    surfaceContainerHighest = Color(0xFF333333),
    secondary = RedisColors.SurfaceContainer,
    onSecondary = Color(0xFFE0E0E0),
    error = Color(0xFFCF6679),
)

@Composable
fun RedisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RedisDarkColorScheme,
        content = content,
    )
}
