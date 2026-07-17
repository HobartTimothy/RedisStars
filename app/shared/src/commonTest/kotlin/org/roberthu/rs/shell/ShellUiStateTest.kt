package org.roberthu.rs.shell

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType

class ShellUiStateTest {

    @Test
    fun toggleRailCollapsed_flipsExpandedAndCollapsed() {
        val expanded = ShellUiState(railCollapsed = false)
        val collapsed = expanded.toggleRailCollapsed()
        assertTrue(collapsed.railCollapsed)
        assertFalse(collapsed.toggleRailCollapsed().railCollapsed)
    }

    @Test
    fun navigateTo_settings_updatesDestination() {
        val next = ShellUiState().navigateTo(ShellDestination.Settings)
        assertEquals(ShellDestination.Settings, next.destination)
    }

    @Test
    fun navigateTo_preservesCollapsedFlag() {
        val next = ShellUiState(railCollapsed = true)
            .navigateTo(ShellDestination.Settings)
        assertTrue(next.railCollapsed)
        assertEquals(ShellDestination.Settings, next.destination)
    }

    @Test
    fun railWidth_matchesCollapsedState() {
        assertEquals(ShellRailDefaults.ExpandedWidth, ShellRailDefaults.width(collapsed = false))
        assertEquals(ShellRailDefaults.CollapsedWidth, ShellRailDefaults.width(collapsed = true))
    }

    @Test
    fun expandedWidth_isWiderThanCollapsed() {
        assertTrue(ShellRailDefaults.ExpandedWidth > ShellRailDefaults.CollapsedWidth)
    }

    @Test
    fun shouldUsePinnedLeftRail_onlyForNavigationRail() {
        assertTrue(shouldUsePinnedLeftRail(NavigationSuiteType.NavigationRail))
        assertFalse(shouldUsePinnedLeftRail(NavigationSuiteType.NavigationBar))
    }
}
