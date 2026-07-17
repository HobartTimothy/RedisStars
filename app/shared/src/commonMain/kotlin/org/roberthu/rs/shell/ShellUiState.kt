package org.roberthu.rs.shell

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.AppLanguage

/**
 * Single source of truth for shell chrome navigation + left-rail collapse.
 * Kept as a data class so behavior can be unit-tested without Compose UI.
 */
data class ShellUiState(
    val destination: ShellDestination = ShellDestination.Connections,
    val railCollapsed: Boolean = false,
    val darkMode: Boolean = true,
    val autoConnect: Boolean = false,
    val language: AppLanguage = AppLanguage.EnUS,
    val bannerError: String? = null,
) {
    fun toggleRailCollapsed(): ShellUiState = copy(railCollapsed = !railCollapsed)

    fun navigateTo(destination: ShellDestination): ShellUiState = copy(destination = destination)
}

sealed interface ShellUiAction {
    data class Navigate(val destination: ShellDestination) : ShellUiAction
    data object ToggleRail : ShellUiAction
    data class SetDarkMode(val enabled: Boolean) : ShellUiAction
    data class SetAutoConnect(val enabled: Boolean) : ShellUiAction
    data class SetLanguage(val language: AppLanguage) : ShellUiAction
    data object DismissError : ShellUiAction
}

object ShellRailDefaults {
    val ExpandedWidth: Dp = 208.dp
    val CollapsedWidth: Dp = 68.dp

    fun width(collapsed: Boolean): Dp =
        if (collapsed) CollapsedWidth else ExpandedWidth
}

/**
 * Desktop-left rail is used whenever adaptive layout resolves to [NavigationSuiteType.NavigationRail].
 * Compact windows fall back to the adaptive suite (bottom bar) with Settings as the trailing item.
 */
fun shouldUsePinnedLeftRail(
    layoutType: androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType,
): Boolean =
    layoutType == androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType.NavigationRail
