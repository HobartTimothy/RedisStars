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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.shell.icons.AppIcons
import org.roberthu.rs.ui.components.RedisTooltipIconButton
import org.roberthu.rs.ui.theme.RedisAppConstants
import org.roberthu.rs.ui.theme.RedisTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellNavigationRail(
    destination: ShellDestination,
    railCollapsed: Boolean,
    onDestinationSelected: (ShellDestination) -> Unit,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = RedisTheme.dimensions
    val spacing = RedisTheme.spacing
    val motion = RedisTheme.motion
    val colors = RedisTheme.colors
    val shapes = RedisTheme.shapes

    val railWidth by animateDpAsState(
        targetValue = ShellRailDefaults.width(railCollapsed),
        animationSpec = tween(durationMillis = motion.normal),
        label = "shellRailWidth",
    )

    val expandLabel = t(StringKeys.Nav.ExpandRail)
    val collapseLabel = t(StringKeys.Nav.CollapseRail)
    val toggleLabel = if (railCollapsed) expandLabel else collapseLabel

    Column(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(colors.toolbarSurface)
            .testTag("sidebar"),
        verticalArrangement = Arrangement.Top,
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        SidebarHeader(
            railCollapsed = railCollapsed,
            toggleLabel = toggleLabel,
            onToggleCollapsed = onToggleCollapsed,
            itemHeight = dim.sidebarItemHeight,
            iconSize = dim.sidebarIconSize,
            horizontalPadding = spacing.lg,
        )

        Spacer(modifier = Modifier.height(spacing.xs))

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

        Spacer(modifier = Modifier.height(spacing.sm))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarHeader(
    railCollapsed: Boolean,
    toggleLabel: String,
    onToggleCollapsed: () -> Unit,
    itemHeight: Dp,
    iconSize: Dp,
    horizontalPadding: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(itemHeight),
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
                    iconSize = iconSize,
                )
            }
        } else {
            // Expanded: app name on the left, collapse button on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalPadding, end = RedisTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = RedisAppConstants.AppName,
                    style = RedisTheme.typography.paneTitle,
                    color = RedisTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SidebarToggleButton(
                    railCollapsed = railCollapsed,
                    toggleLabel = toggleLabel,
                    onToggleCollapsed = onToggleCollapsed,
                    iconSize = iconSize,
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
    iconSize: Dp,
) {
    val railStateDescription = if (railCollapsed) {
        t(StringKeys.Nav.RailStateCollapsed)
    } else {
        t(StringKeys.Nav.RailStateExpanded)
    }
    RedisTooltipIconButton(
        tooltip = toggleLabel,
        onClick = onToggleCollapsed,
        imageVector = if (railCollapsed) AppIcons.SidebarOpen else AppIcons.SidebarClose,
        testTag = "sidebar_toggle",
        iconSize = iconSize,
        style = org.roberthu.rs.ui.components.RedisIconButtonStyle.Plain,
        modifier = Modifier.semantics {
            contentDescription = toggleLabel
            stateDescription = railStateDescription
        },
    )
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
    val dim = RedisTheme.dimensions
    val spacing = RedisTheme.spacing
    val motion = RedisTheme.motion
    val colors = RedisTheme.colors
    val shapes = RedisTheme.shapes

    val bgColor = when {
        selected -> colors.selectedSurface
        isHovered -> colors.hoverSurface
        else -> colors.toolbarSurface
    }
    val contentTint = if (selected) colors.onSelectedSurface else colors.iconSecondary

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dim.sidebarItemHeight)
                .padding(horizontal = if (railCollapsed) 0.dp else spacing.lg)
                .clip(shapes.medium)
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
                tint = contentTint,
                modifier = Modifier.size(dim.sidebarIconSize),
            )
            AnimatedVisibility(
                visible = !railCollapsed,
                enter = fadeIn(animationSpec = tween(RedisTheme.motion.fast)),
                exit = fadeOut(animationSpec = tween(RedisTheme.motion.fast)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(spacing.md))
                    Text(
                        text = label,
                        color = contentTint,
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
