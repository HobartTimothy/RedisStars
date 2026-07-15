package org.roberthu.rs.shell

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

/**
 * Pure resolver used by the production non-compact path and direct unit tests.
 *
 * Its compact contract remains NavigationBar, while [rememberShellNavigationSuiteType] delegates
 * compact adaptive behavior to Material's navigation suite defaults.
 */
fun resolveNavigationSuiteType(isCompactWidth: Boolean): NavigationSuiteType =
    if (isCompactWidth) {
        NavigationSuiteType.NavigationBar
    } else {
        NavigationSuiteType.NavigationRail
    }

/**
 * Resolves shell navigation from adaptive window information.
 *
 * Non-compact windows use the testable [resolveNavigationSuiteType] policy. Compact windows
 * delegate to [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo] so Material can account
 * for adaptive details beyond width that the pure helper intentionally does not model.
 */
@Composable
fun rememberShellNavigationSuiteType(
    adaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
): NavigationSuiteType {
    val isCompact = !adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
    )
    return if (isCompact) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    } else {
        resolveNavigationSuiteType(isCompactWidth = false)
    }
}
