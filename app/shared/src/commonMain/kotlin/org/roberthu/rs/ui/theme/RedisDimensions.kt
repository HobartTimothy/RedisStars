package org.roberthu.rs.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Layout dimension tokens for the desktop shell.
 *
 * Use via [RedisTheme.dimensions] inside composables.
 */
@Immutable
data class RedisDimensions(
    // Navigation rail
    val sidebarExpandedWidth: Dp = 208.dp,
    val sidebarCollapsedWidth: Dp = 68.dp,
    val sidebarItemHeight: Dp = 48.dp,
    val sidebarIconSize: Dp = 20.dp,
    val sidebarItemCornerRadius: Dp = 8.dp,

    // Shell chrome
    val toolbarHeight: Dp = 40.dp,
    val statusDotSize: Dp = 8.dp,
    val dividerWidth: Dp = 1.dp,
    val focusBorderWidth: Dp = 2.dp,

    // Pane widths
    val connectionsPaneWidth: Dp = 280.dp,
    val keyBrowserPaneMinWidth: Dp = 260.dp,
    val keyBrowserPaneDefaultWidth: Dp = 300.dp,

    // Dialog
    val dialogMaxWidth: Dp = 760.dp,
    val dialogMaxHeight: Dp = 680.dp,
    val dialogEditorNavWidth: Dp = 148.dp,

    // Lists and rows
    val connectionTagIndicatorSize: Dp = 10.dp,
    val rowHeight: Dp = 36.dp,
    val rowHeightCompact: Dp = 32.dp,
    val iconButtonSize: Dp = 40.dp,
    val iconButtonSizeCompact: Dp = 32.dp,
    val iconSize: Dp = 20.dp,
    val iconSizeSm: Dp = 16.dp,
    val iconSizeLg: Dp = 24.dp,
    val emptyStateIconSize: Dp = 48.dp,

    // Runtime logs column widths
    val logTimestampColumnWidth: Dp = 176.dp,
    val logLevelColumnWidth: Dp = 56.dp,
    val logTargetColumnWidth: Dp = 160.dp,
)

internal val LocalRedisDimensions = staticCompositionLocalOf { RedisDimensions() }
