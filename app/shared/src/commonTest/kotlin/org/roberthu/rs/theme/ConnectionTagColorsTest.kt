package org.roberthu.rs.theme

import androidx.compose.ui.graphics.Color
import org.roberthu.rs.domain.ConnectionTagColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConnectionTagColorsTest {
    @Test
    fun none_returnsNull() {
        assertNull(ConnectionTagColor.None.toComposeColor())
    }

    @Test
    fun colors_mapToComposeConstants() {
        assertEquals(Color.Red, ConnectionTagColor.Red.toComposeColor())
        assertEquals(Color(0xFFFF9800), ConnectionTagColor.Orange.toComposeColor())
        assertEquals(Color(0xFFFFEB3B), ConnectionTagColor.Yellow.toComposeColor())
        assertEquals(Color(0xFF4CAF50), ConnectionTagColor.Green.toComposeColor())
        assertEquals(Color(0xFF2196F3), ConnectionTagColor.Blue.toComposeColor())
        assertEquals(Color(0xFF9C27B0), ConnectionTagColor.Purple.toComposeColor())
    }
}
