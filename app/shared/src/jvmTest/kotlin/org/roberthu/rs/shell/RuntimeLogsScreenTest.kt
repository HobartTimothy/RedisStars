package org.roberthu.rs.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import org.roberthu.rs.domain.AppLanguage
import org.roberthu.rs.domain.ApplicationLogEntry
import org.roberthu.rs.domain.ApplicationLogLevel
import org.roberthu.rs.i18n.ProvideAppLanguage
import org.roberthu.rs.i18n.StringCatalog
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.presentation.RuntimeLogsUiState
import org.roberthu.rs.shell.runtimelogs.RuntimeLogsScreen
import org.roberthu.rs.theme.RedisTheme

@OptIn(ExperimentalTestApi::class)
class RuntimeLogsScreenTest {
    @Test
    fun runtimeLogsAccessibleWhenDisconnected() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.ZhCN) {
                RedisTheme(darkTheme = true) {
                    RedisAppShell(
                        state = ShellUiState(destination = ShellDestination.RuntimeLogs),
                        onAction = {},
                        connectionState = ConnectionState.Disconnected,
                        runtimeLogsViewModel = null,
                        connectionsContent = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.RuntimeLogs.Title)).assertExists()
    }

    @Test
    fun showsEmptyStateWhenNoLogs() = runComposeUiTest {
        setContent {
            ProvideAppLanguage(AppLanguage.ZhCN) {
                RedisTheme(darkTheme = true) {
                    RuntimeLogsScreen(
                        state = RuntimeLogsUiState(loading = false),
                        onLevelFilterChange = {},
                        onSearchQueryChange = {},
                        onAutoScrollChange = {},
                        onPausedChange = {},
                        onRefresh = {},
                        onClearDisplay = {},
                        onExport = {},
                        onToggleExpanded = {},
                        onUserPinnedScrollChange = {},
                        onDismissExportMessage = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithTag("runtime_logs_empty_title").assertIsDisplayed()
        onNodeWithText(StringCatalog.t(AppLanguage.ZhCN, StringKeys.RuntimeLogs.EmptyTitle)).assertIsDisplayed()
    }

    @Test
    fun levelFilterChipHidesNonMatchingRows() = runComposeUiTest {
        var levelFilter: ApplicationLogLevel? = null
        val entries = listOf(
            ApplicationLogEntry("1", "2026-07-16 16:00:00.000", ApplicationLogLevel.INFO, "app", "info"),
            ApplicationLogEntry("2", "2026-07-16 16:00:01.000", ApplicationLogLevel.ERROR, "app", "error"),
        )
        setContent {
            ProvideAppLanguage(AppLanguage.EnUS) {
                RedisTheme(darkTheme = false) {
                    RuntimeLogsScreen(
                        state = RuntimeLogsUiState(
                            entries = entries,
                            levelFilter = levelFilter,
                            loading = false,
                        ),
                        onLevelFilterChange = { levelFilter = it },
                        onSearchQueryChange = {},
                        onAutoScrollChange = {},
                        onPausedChange = {},
                        onRefresh = {},
                        onClearDisplay = {},
                        onExport = {},
                        onToggleExpanded = {},
                        onUserPinnedScrollChange = {},
                        onDismissExportMessage = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText("info").assertIsDisplayed()
        onNodeWithText("error").assertIsDisplayed()
        onNodeWithTag("runtime_logs_filter_ERROR").performClick()
        setContent {
            ProvideAppLanguage(AppLanguage.EnUS) {
                RedisTheme(darkTheme = false) {
                    RuntimeLogsScreen(
                        state = RuntimeLogsUiState(
                            entries = entries,
                            levelFilter = ApplicationLogLevel.ERROR,
                            loading = false,
                        ),
                        onLevelFilterChange = {},
                        onSearchQueryChange = {},
                        onAutoScrollChange = {},
                        onPausedChange = {},
                        onRefresh = {},
                        onClearDisplay = {},
                        onExport = {},
                        onToggleExpanded = {},
                        onUserPinnedScrollChange = {},
                        onDismissExportMessage = {},
                    )
                }
            }
        }
        waitForIdle()
        onNodeWithText("error").assertIsDisplayed()
    }
}
