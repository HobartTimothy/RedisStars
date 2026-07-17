package org.roberthu.rs.shell.connections

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.presentation.ConnectionsUiState
import org.roberthu.rs.theme.RedisTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ConnectionsPaneTest {
    @Test
    fun connectionsAdd_tagDoesNotExist() = runComposeUiTest {
        setContent {
            RedisTheme {
                ConnectionsPane(
                    state = ConnectionsUiState(),
                    onSelect = {},
                    onBeginCreate = {},
                    onOpenAddGroupDialog = {},
                    onUpdateGroupName = {},
                    onConfirmCreateGroup = {},
                    onDismissGroupDialog = {},
                    onToggleGroupExpanded = {},
                    onEdit = {},
                    onSelectEditorSection = {},
                    onUpdateEditorForm = {},
                    onRequestCloseEditor = {},
                    onConfirmDiscardEditor = {},
                    onDismissDiscardConfirmation = {},
                    onSaveEditor = {},
                    onTestEditor = {},
                    onParseClipboardUrl = {},
                    onTest = {},
                    onConnect = {},
                    onDelete = {},
                    onConfirmDelete = {},
                    onDismissDelete = {},
                )
            }
        }

        assertTrue(onAllNodesWithTag("connections_add").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun groupHeader_isDisplayed_andUngroupedHeaderIsHidden() = runComposeUiTest {
        setContent {
            RedisTheme {
                ConnectionsPane(
                    state = ConnectionsUiState(
                        groups = listOf(
                            ConnectionGroup(id = "g1", name = "Dev", expanded = true),
                        ),
                    ),
                    onSelect = {},
                    onBeginCreate = {},
                    onOpenAddGroupDialog = {},
                    onUpdateGroupName = {},
                    onConfirmCreateGroup = {},
                    onDismissGroupDialog = {},
                    onToggleGroupExpanded = {},
                    onEdit = {},
                    onSelectEditorSection = {},
                    onUpdateEditorForm = {},
                    onRequestCloseEditor = {},
                    onConfirmDiscardEditor = {},
                    onDismissDiscardConfirmation = {},
                    onSaveEditor = {},
                    onTestEditor = {},
                    onParseClipboardUrl = {},
                    onTest = {},
                    onConnect = {},
                    onDelete = {},
                    onConfirmDelete = {},
                    onDismissDelete = {},
                )
            }
        }

        onNodeWithTag("connections_group_g1").assertIsDisplayed()
        assertTrue(onAllNodesWithTag("connections_ungrouped").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun groupDialog_tagIsDisplayed() = runComposeUiTest {
        setContent {
            RedisTheme {
                ConnectionsPane(
                    state = ConnectionsUiState(
                        groupDialog = org.roberthu.rs.presentation.GroupDialogState(name = "New"),
                    ),
                    onSelect = {},
                    onBeginCreate = {},
                    onOpenAddGroupDialog = {},
                    onUpdateGroupName = {},
                    onConfirmCreateGroup = {},
                    onDismissGroupDialog = {},
                    onToggleGroupExpanded = {},
                    onEdit = {},
                    onSelectEditorSection = {},
                    onUpdateEditorForm = {},
                    onRequestCloseEditor = {},
                    onConfirmDiscardEditor = {},
                    onDismissDiscardConfirmation = {},
                    onSaveEditor = {},
                    onTestEditor = {},
                    onParseClipboardUrl = {},
                    onTest = {},
                    onConnect = {},
                    onDelete = {},
                    onConfirmDelete = {},
                    onDismissDelete = {},
                )
            }
        }

        onNodeWithTag("connections_group_dialog").assertIsDisplayed()
    }

    @Test
    fun connectionRow_exposesActionsThroughOverflowMenu() = runComposeUiTest {
        val profile = ConnectionProfile(
            id = "c1",
            name = "Cloud",
            mode = DeploymentMode.Standalone,
            host = "localhost",
        )
        var editedId: String? = null

        setContent {
            RedisTheme {
                ConnectionsPane(
                    state = ConnectionsUiState(
                        profiles = listOf(profile),
                        selectedProfileId = profile.id,
                    ),
                    onSelect = {},
                    onBeginCreate = {},
                    onOpenAddGroupDialog = {},
                    onUpdateGroupName = {},
                    onConfirmCreateGroup = {},
                    onDismissGroupDialog = {},
                    onToggleGroupExpanded = {},
                    onEdit = { editedId = it.id },
                    onSelectEditorSection = {},
                    onUpdateEditorForm = {},
                    onRequestCloseEditor = {},
                    onConfirmDiscardEditor = {},
                    onDismissDiscardConfirmation = {},
                    onSaveEditor = {},
                    onTestEditor = {},
                    onParseClipboardUrl = {},
                    onTest = {},
                    onConnect = {},
                    onDelete = {},
                    onConfirmDelete = {},
                    onDismissDelete = {},
                )
            }
        }

        onNodeWithTag("connection_menu_c1").assertIsDisplayed()
        assertTrue(onAllNodesWithTag("connection_edit_c1").fetchSemanticsNodes().isEmpty())

        onNodeWithTag("connection_menu_c1").performClick()
        onNodeWithTag("connection_actions_menu_c1").assertIsDisplayed()
        onNodeWithTag("connection_connect_c1").assertIsDisplayed()
        onNodeWithTag("connection_edit_c1").performClick()

        assertEquals("c1", editedId)
    }
}
