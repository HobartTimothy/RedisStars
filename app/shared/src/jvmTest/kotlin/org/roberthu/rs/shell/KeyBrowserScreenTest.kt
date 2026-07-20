package org.roberthu.rs.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.presentation.AddKeyDialogState
import org.roberthu.rs.presentation.KeyBrowserUiState
import org.roberthu.rs.shell.keys.AddKeyDialog
import org.roberthu.rs.shell.keys.KeyBrowserScreen
import org.roberthu.rs.theme.RedisTheme
import kotlin.test.Test
import kotlin.test.assertEquals
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
                    onOpenAddKey = {},
                    onSelectDatabase = {},
                    onTypeFilterChange = {},
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
                    onOpenAddKey = {},
                    onSelectDatabase = {},
                    onTypeFilterChange = {},
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
                    onOpenAddKey = {},
                    onSelectDatabase = {},
                    onTypeFilterChange = {},
                )
            }
        }

        onNodeWithTag("key_list_empty").assertIsDisplayed()
    }

    @Test
    fun databaseDropdown_isDisplayed() = runComposeUiTest {
        setContent {
            RedisTheme(darkTheme = true) {
                KeyBrowserScreen(
                    state = KeyBrowserUiState(
                        databases = listOf(
                            RedisDatabaseSummary(0, 5),
                            RedisDatabaseSummary(1, 12),
                        ),
                        selectedDatabase = 0,
                    ),
                    enabled = true,
                    onPatternChange = {},
                    onRefresh = {},
                    onLoadMore = {},
                    onCancel = {},
                    onSelect = {},
                    onOpenAddKey = {},
                    onSelectDatabase = {},
                    onTypeFilterChange = {},
                )
            }
        }

        onNodeWithTag("keys_database_dropdown").assertIsDisplayed()
    }

    @Test
    fun typeFilter_selectingHashInvokesCallback() = runComposeUiTest {
        var selected: RedisKeyType? = null
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
                    onOpenAddKey = {},
                    onSelectDatabase = {},
                    onTypeFilterChange = { selected = it },
                )
            }
        }

        onNodeWithTag("keys_type_filter").assertIsDisplayed().performClick()
        onNodeWithTag("keys_type_filter_hash").performClick()
        assertEquals(RedisKeyType.Hash, selected)
    }

    @Test
    fun addKeyDialog_opensWithFields() = runComposeUiTest {
        setContent {
            RedisTheme(darkTheme = true) {
                AddKeyDialog(
                    state = AddKeyDialogState(),
                    databases = listOf(RedisDatabaseSummary(0, 0)),
                    clusterMode = false,
                    onDismiss = {},
                    onKeyChange = {},
                    onDatabaseChange = {},
                    onTypeChange = {},
                    onTtlTextChange = {},
                    onPermanentChange = {},
                    onStringValueChange = {},
                    onHashFieldsChange = {},
                    onListValuesChange = {},
                    onSetMembersChange = {},
                    onZsetEntriesChange = {},
                    onStreamFieldsChange = {},
                    onJsonContentChange = {},
                    onImportClick = {},
                    onSubmit = {},
                )
            }
        }

        onNodeWithTag("add_key_dialog").assertIsDisplayed()
        onNodeWithTag("add_key_name").assertIsDisplayed()
        onNodeWithTag("add_key_type").assertIsDisplayed()
    }
}
