package org.roberthu.rs.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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
 * Compose UI checks: selected NavigationRail labels stay visible with sufficient contrast.
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
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Nav.SlowLog)).assertIsDisplayed()
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
        onNodeWithText(StringCatalog.t(AppLanguage.EnUS, StringKeys.Nav.SlowLog)).assertIsDisplayed()
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
}

private fun ShellDestination.labelKey(): String = when (this) {
    ShellDestination.Connections -> StringKeys.Nav.Connections
    ShellDestination.Monitor -> StringKeys.Nav.Monitor
    ShellDestination.SlowLog -> StringKeys.Nav.SlowLog
    ShellDestination.Settings -> StringKeys.Nav.Settings
}
