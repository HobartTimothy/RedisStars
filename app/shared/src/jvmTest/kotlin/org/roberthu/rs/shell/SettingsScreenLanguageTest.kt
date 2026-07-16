package org.roberthu.rs.i18n

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import org.roberthu.rs.domain.AppLanguage
import org.roberthu.rs.shell.SettingsScreen
import org.roberthu.rs.theme.RedisTheme

@OptIn(ExperimentalTestApi::class)
class SettingsScreenLanguageTest {

    @Test
    fun zhLanguage_showsChineseSettingsCopy() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.ZhCN) {
                RedisTheme(darkTheme = true) {
                    SettingsScreen(
                        darkMode = true,
                        onDarkModeChange = {},
                        autoConnect = false,
                        onAutoConnectChange = {},
                        language = AppLanguage.ZhCN,
                        onLanguageChange = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Settings.Language)).assertIsDisplayed()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.Settings.LanguageDesc)).assertIsDisplayed()
        onNodeWithTag("settings_language_zh").assertIsDisplayed()
    }

    @Test
    fun enLanguage_showsEnglishSettingsCopy() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.EnUS) {
                RedisTheme(darkTheme = true) {
                    SettingsScreen(
                        darkMode = true,
                        onDarkModeChange = {},
                        autoConnect = false,
                        onAutoConnectChange = {},
                        language = AppLanguage.EnUS,
                        onLanguageChange = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText(StringCatalog.t(AppLanguage.EnUS, StringKeys.Settings.Language)).assertIsDisplayed()
        onNodeWithText(StringCatalog.t(AppLanguage.EnUS, StringKeys.Settings.LanguageDesc)).assertIsDisplayed()
        onNodeWithTag("settings_language_en").assertIsDisplayed()
    }
}
