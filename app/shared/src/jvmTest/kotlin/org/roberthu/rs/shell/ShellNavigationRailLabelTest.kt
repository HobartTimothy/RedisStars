package org.roberthu.rs.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import org.roberthu.rs.theme.RedisTheme

/**
 * Compose UI checks: selected NavigationRail labels stay visible with sufficient contrast.
 */
@OptIn(ExperimentalTestApi::class)
class ShellNavigationRailLabelTest {

    @Test
    fun whenConnectionsSelected_allPrimaryLabelsVisible() = runComposeUiTest {
        setContent {
            RedisTheme(darkTheme = true) {
                ShellNavigationRail(
                    destination = ShellDestination.Connections,
                    railCollapsed = false,
                    onDestinationSelected = {},
                    onToggleCollapsed = {},
                )
            }
        }
        waitForIdle()
        onNodeWithTag("sidebar").assertExists()
        onNodeWithText("连接管理").assertIsDisplayed()
        onNodeWithText("实时监控").assertIsDisplayed()
        onNodeWithText("慢日志").assertIsDisplayed()
    }

    @Test
    fun switchingSelection_eachSelectedLabelStaysVisible() = runComposeUiTest {
        ShellDestination.primaryDestinations.forEach { selected ->
            setContent {
                RedisTheme(darkTheme = true) {
                    ShellNavigationRail(
                        destination = selected,
                        railCollapsed = false,
                        onDestinationSelected = {},
                        onToggleCollapsed = {},
                    )
                }
            }
            waitForIdle()
            onNodeWithText(selected.label).assertIsDisplayed()
            ShellDestination.primaryDestinations.forEach { item ->
                onNodeWithText(item.label).assertIsDisplayed()
            }
        }
    }

    @Test
    fun whenSettingsSelected_settingsAndPrimaryLabelsVisible() = runComposeUiTest {
        setContent {
            RedisTheme(darkTheme = true) {
                ShellNavigationRail(
                    destination = ShellDestination.Settings,
                    railCollapsed = false,
                    onDestinationSelected = {},
                    onToggleCollapsed = {},
                )
            }
        }
        waitForIdle()
        onNodeWithText("设置").assertIsDisplayed()
        onNodeWithText("连接管理").assertIsDisplayed()
    }
}
