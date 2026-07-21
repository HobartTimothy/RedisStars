package org.roberthu.rs.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class RedisShapeTokens(
    val extraSmall: Shape = RoundedCornerShape(4.dp),
    val small: Shape = RoundedCornerShape(6.dp),
    val medium: Shape = RoundedCornerShape(8.dp),
    val large: Shape = RoundedCornerShape(12.dp),
)

fun createRedisMaterialShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
)

internal val LocalRedisShapes = staticCompositionLocalOf { RedisShapeTokens() }
