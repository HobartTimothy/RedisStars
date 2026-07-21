package org.roberthu.rs.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.roberthu.rs.ui.theme.darkRedisColorTokens
import org.roberthu.rs.ui.theme.lightRedisColorTokens
import org.roberthu.rs.ui.theme.toMaterialColorScheme

/**
 * @deprecated Use [org.roberthu.rs.ui.theme.RedisTheme] — the unified application theme entry.
 */
@Deprecated(
    message = "Use org.roberthu.rs.ui.theme.RedisTheme for the unified theme entry point",
    replaceWith = ReplaceWith("RedisTheme", "org.roberthu.rs.ui.theme.RedisTheme"),
)
@Composable
fun RedisTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    org.roberthu.rs.ui.theme.RedisTheme(darkTheme = darkTheme, content = content)
}

/** @deprecated Compatibility export for tests — use token-based colors in new code. */
@Deprecated("Use org.roberthu.rs.ui.theme.darkRedisColorTokens()")
val RedisDarkColorScheme: ColorScheme
    get() = darkRedisColorTokens().toMaterialColorScheme()

/** @deprecated Compatibility export for tests — use token-based colors in new code. */
@Deprecated("Use org.roberthu.rs.ui.theme.lightRedisColorTokens()")
val RedisLightColorScheme: ColorScheme
    get() = lightRedisColorTokens().toMaterialColorScheme()

/** Label color used for selected navigation items. */
fun navigationRailSelectedLabelColor(scheme: ColorScheme): Color = scheme.onSecondaryContainer

fun navigationRailSelectedLabelContrastsWithRail(scheme: ColorScheme): Boolean =
    navigationRailSelectedLabelColor(scheme) != scheme.surfaceContainer
