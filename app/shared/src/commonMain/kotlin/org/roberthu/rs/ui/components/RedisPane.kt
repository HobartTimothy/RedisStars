package org.roberthu.rs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.roberthu.rs.ui.theme.RedisTheme

@Composable
fun RedisPane(
    modifier: Modifier = Modifier,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = RedisTheme.colors
    val spacing = RedisTheme.spacing
    Column(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(spacing.panePadding),
        content = content,
    )
}

@Composable
fun RedisPaneHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = RedisTheme.colors
    val spacing = RedisTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = RedisTheme.typography.paneTitle,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun RedisToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = RedisTheme.colors
    val spacing = RedisTheme.spacing
    val dim = RedisTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dim.toolbarHeight)
            .padding(horizontal = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
    HorizontalDivider(color = colors.divider, thickness = dim.dividerWidth)
}

@Composable
fun rememberInteractionSurfaceColor(
    selected: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): androidx.compose.ui.graphics.Color {
    val colors = RedisTheme.colors
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    if (!enabled) return colors.appBackground
    return when {
        selected -> colors.selectedSurface
        pressed -> colors.pressedSurface
        hovered -> colors.hoverSurface
        else -> colors.appBackground
    }
}

@Composable
fun Modifier.redisFocusBorder(
    focused: Boolean,
    cornerRadius: Float = 6f,
): Modifier {
    if (!focused) return this
    val colors = RedisTheme.colors
    val dim = RedisTheme.dimensions
    return drawBehind {
        drawRoundRect(
            color = colors.focusBorder,
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = dim.focusBorderWidth.toPx()),
        )
    }
}
