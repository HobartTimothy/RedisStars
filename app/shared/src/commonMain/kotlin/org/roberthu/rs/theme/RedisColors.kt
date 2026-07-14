package org.roberthu.rs.theme

import androidx.compose.ui.graphics.Color

object RedisColors {
    val Background = Color(0xFF1E1E1E)
    val SurfaceContainer = Color(0xFF2D2D2D)
    val Primary = Color(0xFFE53935)
    val ConnectionOk = Color(0xFF4CAF50)
}

fun connectionIndicatorColor(connected: Boolean, onSurface: Color): Color =
    if (connected) RedisColors.ConnectionOk else onSurface.copy(alpha = 0.38f)
