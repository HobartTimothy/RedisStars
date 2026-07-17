package org.roberthu.rs.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.shell.icons.AppIcons

private val ItemHeight = 48.dp
private val IconSize = 20.dp
private val ItemHorizontalPadding = 16.dp
private val IconTextGap = 12.dp
private val ItemCornerRadius = 8.dp
private val SidebarAnimDurationMs = 200
private val TextAnimDurationMs = 120

/**
 * Left-pinned Gemini-style navigation sidebar.
 *
 * Expanded: shows logo, app name, collapse button, icon + label rows, Settings pinned at bottom.
 * Collapsed: shows expand button and icon-only rows; tooltips reveal labels on hover.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellNavigationRail(
    destination: ShellDestination,
    railCollapsed: Boolean,
    onDestinationSelected: (ShellDestination) -> Unit,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val railWidth by animateDpAsState(
        targetValue = ShellRailDefaults.width(railCollapsed),
        animationSpec = tween(durationMillis = SidebarAnimDurationMs),
        label = "shellRailWidth",
    )

    val expandLabel = t(StringKeys.Nav.ExpandRail)
    val collapseLabel = t(StringKeys.Nav.CollapseRail)
    val toggleLabel = if (railCollapsed) expandLabel else collapseLabel

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .testTag("sidebar"),
        verticalArrangement = Arrangement.Top,
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        SidebarHeader(
            railCollapsed = railCollapsed,
            toggleLabel = toggleLabel,
            onToggleCollapsed = onToggleCollapsed,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── Primary destinations ──────────────────────────────────────────────
        ShellDestination.primaryDestinations.forEach { item ->
            SidebarMenuItem(
                item = item,
                selected = destination == item,
                railCollapsed = railCollapsed,
                onSelected = { onDestinationSelected(item) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Settings (pinned bottom) ──────────────────────────────────────────
        SidebarMenuItem(
            item = ShellDestination.Settings,
            selected = destination == ShellDestination.Settings,
            railCollapsed = railCollapsed,
            onSelected = { onDestinationSelected(ShellDestination.Settings) },
            modifier = Modifier.testTag("settings_menu_item"),
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarHeader(
    railCollapsed: Boolean,
    toggleLabel: String,
    onToggleCollapsed: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ItemHeight),
    ) {
        if (railCollapsed) {
            // Collapsed: centered expand button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SidebarToggleButton(
                    railCollapsed = railCollapsed,
                    toggleLabel = toggleLabel,
                    onToggleCollapsed = onToggleCollapsed,
                )
            }
        } else {
            // Expanded: app name on the left, collapse button on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ItemHorizontalPadding, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "RedisStars",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SidebarToggleButton(
                    railCollapsed = railCollapsed,
                    toggleLabel = toggleLabel,
                    onToggleCollapsed = onToggleCollapsed,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarToggleButton(
    railCollapsed: Boolean,
    toggleLabel: String,
    onToggleCollapsed: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(toggleLabel) } },
        state = rememberTooltipState(),
    ) {
        IconButton(
            onClick = onToggleCollapsed,
            modifier = Modifier
                .testTag("sidebar_toggle")
                .semantics {
                    contentDescription = toggleLabel
                    stateDescription = if (railCollapsed) "collapsed" else "expanded"
                },
        ) {
            Icon(
                imageVector = if (railCollapsed) AppIcons.SidebarOpen else AppIcons.SidebarClose,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarMenuItem(
    item: ShellDestination,
    selected: Boolean,
    railCollapsed: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = item.label()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val selectedBg = MaterialTheme.colorScheme.secondaryContainer
    val hoverBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val bgColor = when {
        selected -> selectedBg
        isHovered -> hoverBg
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val iconTint = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemHeight)
                .padding(horizontal = if (railCollapsed) 0.dp else ItemHorizontalPadding)
                .clip(RoundedCornerShape(ItemCornerRadius))
                .background(bgColor)
                .hoverable(interactionSource)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    onClick = onSelected,
                )
                .semantics {
                    contentDescription = label
                }
                .then(modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (railCollapsed) Arrangement.Center else Arrangement.Start,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(IconSize),
            )
            AnimatedVisibility(
                visible = !railCollapsed,
                enter = fadeIn(animationSpec = tween(TextAnimDurationMs)),
                exit = fadeOut(animationSpec = tween(TextAnimDurationMs)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(IconTextGap))
                    Text(
                        text = label,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        softWrap = false,
                    )
                }
            }
        }
    }

    if (railCollapsed) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(label) } },
            state = rememberTooltipState(),
        ) {
            rowContent()
        }
    } else {
        rowContent()
    }
}
