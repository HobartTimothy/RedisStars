package org.roberthu.rs.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * Standard icon button with a plain tooltip and accessible content description.
 *
 * Replaces the repeated `TooltipBox → IconButton → Icon` boilerplate that appears
 * in KeyBrowserScreen, ShellNavigationRail, and RuntimeLogsScreen.
 *
 * Tooltip is shown on hover (desktop); [contentDescription] is always applied as
 * a semantic label so screen readers and Compose UI tests can locate the button.
 *
 * @param tooltip Localized label shown in the hover tooltip and used as the
 *   semantic content description. Must come from [t] or equivalent i18n lookup.
 * @param onClick Called when the button is activated (click or Enter/Space).
 * @param imageVector Icon to render inside the button.
 * @param modifier Optional modifier applied to the outermost [TooltipBox].
 * @param enabled Whether the button accepts interactions. Defaults to true.
 * @param testTag Optional test tag applied to the [IconButton] node.
 * @param iconSize Size of the [Icon]. Defaults to the icon default (no explicit size).
 * @param buttonSize Optional square size for the [IconButton] touch target.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedisTooltipIconButton(
    tooltip: String,
    onClick: () -> Unit,
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
    iconSize: Dp? = null,
    buttonSize: Dp? = null,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        val buttonModifier = Modifier
            .semantics { contentDescription = tooltip }
            .let { m -> if (testTag != null) m.testTag(testTag) else m }
            .let { m -> if (buttonSize != null) m.size(buttonSize) else m }

        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
        ) {
            val iconModifier = if (iconSize != null) Modifier.size(iconSize) else Modifier
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = iconModifier,
            )
        }
    }
}
