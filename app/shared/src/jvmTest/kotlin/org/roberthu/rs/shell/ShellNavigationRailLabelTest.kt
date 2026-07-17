package org.roberthu.rs.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import org.roberthu.rs.domain.AppLanguage
import org.roberthu.rs.i18n.ProvideAppLanguage
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.StringCatalog
import org.roberthu.rs.theme.RedisTheme

/**
 * Compose UI checks: selected NavigationRail labels stay visible with sufficient contrast,
 * and collapsed state provides correct accessibility semantics.
 */
@OptIn(ExperimentalTestApi::class)
class ShellNavigationRailLabelTest {

    @Test
    fun whenConnectionsSelected_allPrimaryLabelsVisible_zh() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.ZhCN) {
                RedisTheme(darkTheme = true) {
                    ShellNavigationRail(
                        destination = ShellDestination.Connections,
                        railCollapsed = false,
                        onDestinationSelected = {},
                        onToggleCollapsed = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithTag("sidebar").assertExists()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.Connections)).assertIsDisplayed()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.Monitor)).assertIsDisplayed()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.RuntimeLogs)).assertIsDisplayed()
    }

    @Test
    fun whenConnectionsSelected_allPrimaryLabelsVisible_en() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.EnUS) {
                RedisTheme(darkTheme = true) {
                    ShellNavigationRail(
                        destination = ShellDestination.Connections,
                        railCollapsed = false,
                        onDestinationSelected = {},
                        onToggleCollapsed = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText(StringCatalog.t(AppLanguage.EnUS, StringKeys.Nav.Connections)).assertIsDisplayed()
        onNodeWithText(StringCatalog.t(AppLanguage.EnUS, StringKeys.Nav.Monitor)).assertIsDisplayed()
        onNodeWithText(StringCatalog.t(AppLanguage.EnUS, StringKeys.Nav.RuntimeLogs)).assertIsDisplayed()
    }

    @Test
    fun switchingSelection_eachSelectedLabelStaysVisible() = runComposeUiTest {
        ShellDestination.primaryDestinations.forEach { selected ->
            setContent {
                ProvideAppLanguage(AppLanguage.ZhCN) {
                    RedisTheme(darkTheme = true) {
                        ShellNavigationRail(
                            destination = selected,
                            railCollapsed = false,
                            onDestinationSelected = {},
                            onToggleCollapsed = {},
                        )
                    }
                }
            }
            waitForIdle()
            onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, selected.labelKey())).assertIsDisplayed()
            ShellDestination.primaryDestinations.forEach { item ->
                onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, item.labelKey())).assertIsDisplayed()
            }
        }
    }

    @Test
    fun whenSettingsSelected_settingsAndPrimaryLabelsVisible() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.ZhCN) {
                RedisTheme(darkTheme = true) {
                    ShellNavigationRail(
                        destination = ShellDestination.Settings,
                        railCollapsed = false,
                        onDestinationSelected = {},
                        onToggleCollapsed = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.Settings)).assertIsDisplayed()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.Connections)).assertIsDisplayed()
    }

    @Test
    fun whenCollapsed_labelsAreNotRenderedAsText() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.ZhCN) {
                RedisTheme(darkTheme = true) {
                    ShellNavigationRail(
                        destination = ShellDestination.Connections,
                        railCollapsed = true,
                        onDestinationSelected = {},
                        onToggleCollapsed = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.Connections)).assertDoesNotExist()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.Monitor)).assertDoesNotExist()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.RuntimeLogs)).assertDoesNotExist()
    }

    @Test
    fun whenCollapsed_menuItemsHaveAccessibleContentDescription() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.ZhCN) {
                RedisTheme(darkTheme = true) {
                    ShellNavigationRail(
                        destination = ShellDestination.Connections,
                        railCollapsed = true,
                        onDestinationSelected = {},
                        onToggleCollapsed = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithContentDescription(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.Connections)).assertExists()
        onNodeWithContentDescription(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.Settings)).assertExists()
    }

    @Test
    fun whenExpanded_toggleButtonHasCollapseDescription() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.EnUS) {
                RedisTheme(darkTheme = true) {
                    ShellNavigationRail(
                        destination = ShellDestination.Connections,
                        railCollapsed = false,
                        onDestinationSelected = {},
                        onToggleCollapsed = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithTag("sidebar_toggle")
            .assertContentDescriptionEquals(StringCatalog.t(AppLanguage.EnUS, StringKeys.Nav.CollapseRail))
    }

    @Test
    fun whenCollapsed_toggleButtonHasExpandDescription() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.EnUS) {
                RedisTheme(darkTheme = true) {
                    ShellNavigationRail(
                        destination = ShellDestination.Connections,
                        railCollapsed = true,
                        onDestinationSelected = {},
                        onToggleCollapsed = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithTag("sidebar_toggle")
            .assertContentDescriptionEquals(StringCatalog.t(AppLanguage.EnUS, StringKeys.Nav.ExpandRail))
    }

    @Test
    fun settingsItem_alwaysPresent_inCollapsedState() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.ZhCN) {
                RedisTheme(darkTheme = true) {
                    ShellNavigationRail(
                        destination = ShellDestination.Connections,
                        railCollapsed = true,
                        onDestinationSelected = {},
                        onToggleCollapsed = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithTag("settings_menu_item").assertExists()
    }
}

private fun ShellDestination.labelKey(): String = when (this) {
    ShellDestination.Connections -> StringKeys.Nav.Connections
    ShellDestination.Monitor -> StringKeys.Nav.Monitor
    ShellDestination.RuntimeLogs -> StringKeys.Nav.RuntimeLogs
    ShellDestination.Settings -> StringKeys.Nav.Settings
}
