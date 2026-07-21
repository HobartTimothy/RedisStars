package org.roberthu.rs.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import org.roberthu.rs.ui.theme.RedisTheme

enum class RedisIconButtonStyle {
    Plain,
    Outlined,
    Filled,
    Destructive,
}

/**
 * Standard icon button with tooltip, accessibility, and unified interaction states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedisTooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    destructive: Boolean = false,
    compact: Boolean = false,
    style: RedisIconButtonStyle = RedisIconButtonStyle.Plain,
    testTag: String? = null,
    iconSize: Dp? = null,
    buttonSize: Dp? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val dim = RedisTheme.dimensions
    val colors = RedisTheme.colors
    val shapes = RedisTheme.shapes
    val resolvedButtonSize = buttonSize ?: if (compact) dim.iconButtonSizeCompact else dim.iconButtonSize
    val resolvedIconSize = iconSize ?: if (compact) dim.iconSizeSm else dim.iconSize
    val focused by interactionSource.collectIsFocusedAsState()

    val containerColor = when {
        !enabled -> colors.appBackground
        destructive && style == RedisIconButtonStyle.Destructive ->
            MaterialTheme.colorScheme.errorContainer.copy(alpha = if (selected) 1f else 0.7f)
        selected -> colors.selectedSurface
        style == RedisIconButtonStyle.Filled -> colors.brandContainer
        else -> colors.appBackground
    }
    val iconTint = when {
        !enabled -> colors.iconSecondary
        destructive -> MaterialTheme.colorScheme.onErrorContainer
        selected -> colors.onSelectedSurface
        else -> colors.iconSecondary
    }
    val border = when (style) {
        RedisIconButtonStyle.Outlined -> BorderStroke(dim.dividerWidth, colors.divider)
        else -> null
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        val buttonModifier = Modifier
            .semantics { contentDescription = tooltip }
            .let { m -> if (testTag != null) m.testTag(testTag) else m }
            .size(resolvedButtonSize)
            .redisFocusBorder(focused)

        when (style) {
            RedisIconButtonStyle.Plain,
            RedisIconButtonStyle.Filled,
            RedisIconButtonStyle.Destructive,
            -> {
                IconButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = buttonModifier,
                    interactionSource = interactionSource,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = containerColor,
                        disabledContainerColor = colors.appBackground,
                        contentColor = iconTint,
                        disabledContentColor = colors.textDisabled,
                    ),
                ) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(resolvedIconSize),
                        tint = iconTint,
                    )
                }
            }
            RedisIconButtonStyle.Outlined -> {
                Surface(
                    onClick = onClick,
                    enabled = enabled,
                    shape = shapes.small,
                    color = containerColor,
                    border = border,
                    modifier = buttonModifier,
                    interactionSource = interactionSource,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = imageVector,
                            contentDescription = null,
                            modifier = Modifier.size(resolvedIconSize),
                            tint = iconTint,
                        )
                    }
                }
            }
        }
    }
}
