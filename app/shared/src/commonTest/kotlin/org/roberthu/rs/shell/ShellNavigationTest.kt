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
        assertEquals(
            NavigationSuiteType.NavigationBar,
            resolveNavigationSuiteType(isCompactWidth = true),
        )
    }
}
