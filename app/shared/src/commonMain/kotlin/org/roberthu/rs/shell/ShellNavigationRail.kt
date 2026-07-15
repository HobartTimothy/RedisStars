package org.roberthu.rs.shell

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Left-pinned activity rail: primary destinations on top, Settings on the bottom.
 * Collapse hides labels and shrinks width; icons + Settings remain clickable.
 */
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
        label = "shellRailWidth",
    )
    val itemColors = NavigationRailItemDefaults.colors(
        // Material maps selected label to ColorScheme.secondary; pin to onSurface for contrast
        // against surfaceContainer rail chrome regardless of theme quirks.
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    NavigationRail(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .testTag("sidebar"),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        IconButton(
            onClick = onToggleCollapsed,
            modifier = Modifier
                .testTag("sidebar_toggle")
                .semantics {
                    contentDescription = if (railCollapsed) {
                        "展开侧边栏"
                    } else {
                        "收起侧边栏"
                    }
                },
        ) {
            Icon(
                imageVector = if (railCollapsed) {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft
                },
                contentDescription = null,
            )
        }

        ShellDestination.primaryDestinations.forEach { item ->
            NavigationRailItem(
                selected = destination == item,
                onClick = { onDestinationSelected(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = if (railCollapsed) {
                    null
                } else {
                    { Text(item.label) }
                },
                alwaysShowLabel = true,
                colors = itemColors,
                modifier = Modifier.testTag("nav_${item.name}"),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))

        NavigationRailItem(
            selected = destination == ShellDestination.Settings,
            onClick = { onDestinationSelected(ShellDestination.Settings) },
            icon = {
                Icon(
                    imageVector = ShellDestination.Settings.icon,
                    contentDescription = ShellDestination.Settings.label,
                )
            },
            label = if (railCollapsed) {
                null
            } else {
                { Text(ShellDestination.Settings.label) }
            },
            alwaysShowLabel = true,
            colors = itemColors,
            modifier = Modifier.testTag("settings_menu_item"),
        )
    }
}
