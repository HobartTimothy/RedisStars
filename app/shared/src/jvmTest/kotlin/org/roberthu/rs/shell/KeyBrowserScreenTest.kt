package org.roberthu.rs.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.presentation.KeyBrowserUiState
import org.roberthu.rs.shell.keys.KeyBrowserScreen
import org.roberthu.rs.theme.RedisTheme
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class KeyBrowserScreenTest {
    @Test
    fun nextCursor_showsWorkingLoadMoreAction() = runComposeUiTest {
        var clicked = false
        setContent {
            RedisTheme(darkTheme = true) {
                KeyBrowserScreen(
                    state = KeyBrowserUiState(
                        keys = listOf(RedisKeySummary("users:1", RedisKeyType.Hash)),
                        nextCursorToken = "next",
                    ),
                    enabled = true,
                    onPatternChange = {},
                    onRefresh = {},
                    onLoadMore = { clicked = true },
                    onCancel = {},
                    onSelect = {},
                )
            }
        }

        onNodeWithTag("load_more").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }

    @Test
    fun scanFailure_showsRecoverableError() = runComposeUiTest {
        setContent {
            RedisTheme(darkTheme = true) {
                KeyBrowserScreen(
                    state = KeyBrowserUiState(error = "Redis is offline"),
                    enabled = true,
                    onPatternChange = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onCancel = {},
                    onSelect = {},
                )
            }
        }

        onNodeWithTag("key_scan_error").assertIsDisplayed()
    }

    @Test
    fun successfulEmptyScan_showsEmptyState() = runComposeUiTest {
        setContent {
            RedisTheme(darkTheme = true) {
                KeyBrowserScreen(
                    state = KeyBrowserUiState(),
                    enabled = true,
                    onPatternChange = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onCancel = {},
                    onSelect = {},
                )
            }
        }

        onNodeWithTag("key_list_empty").assertIsDisplayed()
    }
}
