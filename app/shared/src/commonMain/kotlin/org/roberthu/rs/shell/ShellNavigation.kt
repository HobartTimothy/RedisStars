package org.roberthu.rs.shell

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

/**
 * Pure helper for tests: compact → NavigationBar; otherwise NavigationRail.
 * Compose entrypoint [rememberShellNavigationSuiteType] uses real adaptive info.
 */
fun resolveNavigationSuiteType(isCompactWidth: Boolean): NavigationSuiteType =
    if (isCompactWidth) {
        NavigationSuiteType.NavigationBar
    } else {
        NavigationSuiteType.NavigationRail
    }

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
        NavigationSuiteType.NavigationRail
    }
}
