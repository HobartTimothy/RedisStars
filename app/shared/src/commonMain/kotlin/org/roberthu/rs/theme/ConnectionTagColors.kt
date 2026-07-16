package org.roberthu.rs.theme

import androidx.compose.ui.graphics.Color
import org.roberthu.rs.domain.ConnectionTagColor

fun ConnectionTagColor.toComposeColor(): Color? = when (this) {
    ConnectionTagColor.None -> null
    ConnectionTagColor.Red -> Color.Red
    ConnectionTagColor.Orange -> Color(0xFFFF9800)
    ConnectionTagColor.Yellow -> Color(0xFFFFEB3B)
    ConnectionTagColor.Green -> Color(0xFF4CAF50)
    ConnectionTagColor.Blue -> Color(0xFF2196F3)
    ConnectionTagColor.Purple -> Color(0xFF9C27B0)
}
