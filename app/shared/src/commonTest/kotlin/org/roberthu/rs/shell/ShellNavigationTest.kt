package org.roberthu.rs.shell

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import kotlin.test.Test
import kotlin.test.assertEquals

class ShellNavigationTest {
    @Test
    fun nonCompact_forcesNavigationRail() {
        assertEquals(
            NavigationSuiteType.NavigationRail,
            resolveNavigationSuiteType(isCompactWidth = false),
        )
    }

    @Test
    fun compact_usesNavigationBar() {
        // Production compact navigation delegates to calculateFromAdaptiveInfo; this verifies the
        // documented width-only contract of the pure resolver.
        assertEquals(
            NavigationSuiteType.NavigationBar,
            resolveNavigationSuiteType(isCompactWidth = true),
        )
    }
}
