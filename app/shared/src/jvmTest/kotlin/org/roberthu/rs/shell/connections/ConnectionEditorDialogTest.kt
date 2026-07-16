package org.roberthu.rs.shell.connections

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.SshAuthMethod
import org.roberthu.rs.presentation.ConnectionEditorMode
import org.roberthu.rs.presentation.ConnectionEditorSection
import org.roberthu.rs.presentation.ConnectionEditorUiState
import org.roberthu.rs.presentation.ConnectionFormState
import org.roberthu.rs.presentation.ConnectionsUiState
import org.roberthu.rs.presentation.HostPortFormState
import org.roberthu.rs.theme.RedisTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class ConnectionEditorDialogTest {
    @Test
    fun createEditor_showsDialogWithGeneralSection() = runComposeUiTest {
        val form = ConnectionFormState.defaults()
        setContent {
            RedisTheme {
                ConnectionsPane(
                    state = ConnectionsUiState(
                        editor = ConnectionEditorUiState(
                            mode = ConnectionEditorMode.Create,
                            profileId = "connection-1",
                            selectedSection = ConnectionEditorSection.General,
                            initialForm = form,
                            form = form,
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

        onNodeWithTag("connection_editor_dialog").assertIsDisplayed()
        onNodeWithTag("connection_editor_title").assertIsDisplayed()
        onNodeWithTag("connection_editor_name").assertIsDisplayed()
        onNodeWithTag("connection_editor_host").assertIsDisplayed()
    }

    @Test
    fun switchingSectionsPreservesInput() = runComposeUiTest {
        val form = ConnectionFormState.defaults()
        val editor = mutableStateOf(
            ConnectionEditorUiState(
                mode = ConnectionEditorMode.Create,
                profileId = "connection-1",
                selectedSection = ConnectionEditorSection.General,
                initialForm = form,
                form = form,
            ),
        )
        setContent {
            RedisTheme {
                ConnectionEditorDialog(
                    state = editor.value,
                    onSelectSection = { section ->
                        editor.value = editor.value.copy(selectedSection = section)
                    },
                    onUpdateForm = { transform ->
                        editor.value = editor.value.copy(form = transform(editor.value.form))
                    },
                    onRequestClose = {},
                    onConfirmDiscard = {},
                    onDismissDiscard = {},
                    onTest = {},
                    onSave = {},
                    onParseUrl = {},
                )
            }
        }

        onNodeWithTag("connection_editor_name").performTextClearance()
        onNodeWithTag("connection_editor_name").performTextInput("Kept Name")
        onNodeWithTag("connection_editor_section_advanced").performClick()
        onNodeWithTag("connection_editor_client_name").assertIsDisplayed()
        onNodeWithTag("connection_editor_section_general").performClick()
        onNodeWithTag("connection_editor_name").assertTextContains("Kept Name")
    }

    @Test
    fun edit_showsExistingName() = runComposeUiTest {
        val form = ConnectionFormState.from(
            ConnectionProfile(
                id = "c1",
                name = "Existing Redis",
                mode = DeploymentMode.Standalone,
                host = "localhost",
            ),
        )
        setContent {
            RedisTheme {
                ConnectionEditorDialog(
                    state = ConnectionEditorUiState(
                        mode = ConnectionEditorMode.Edit,
                        profileId = "c1",
                        selectedSection = ConnectionEditorSection.General,
                        initialForm = form,
                        form = form,
                    ),
                    onSelectSection = {},
                    onUpdateForm = {},
                    onRequestClose = {},
                    onConfirmDiscard = {},
                    onDismissDiscard = {},
                    onTest = {},
                    onSave = {},
                    onParseUrl = {},
                )
            }
        }

        onNodeWithTag("connection_editor_title").assertIsDisplayed()
        onNodeWithTag("connection_editor_name").assertTextContains("Existing Redis")
    }

    @Test
    fun cancel_closesCleanEditor() = runComposeUiTest {
        val form = ConnectionFormState.defaults()
        val editor = mutableStateOf<ConnectionEditorUiState?>(
            ConnectionEditorUiState(
                mode = ConnectionEditorMode.Create,
                profileId = "connection-1",
                selectedSection = ConnectionEditorSection.General,
                initialForm = form,
                form = form,
            ),
        )
        setContent {
            RedisTheme {
                editor.value?.let { current ->
                    ConnectionEditorDialog(
                        state = current,
                        onSelectSection = {},
                        onUpdateForm = {},
                        onRequestClose = { editor.value = null },
                        onConfirmDiscard = {},
                        onDismissDiscard = {},
                        onTest = {},
                        onSave = {},
                        onParseUrl = {},
                    )
                }
            }
        }

        onNodeWithTag("connection_editor_cancel").performClick()
        assertNull(editor.value)
    }

    @Test
    fun dirtyCancel_showsDiscardConfirmation() = runComposeUiTest {
        val form = ConnectionFormState.defaults()
        val editor = mutableStateOf(
            ConnectionEditorUiState(
                mode = ConnectionEditorMode.Create,
                profileId = "connection-1",
                selectedSection = ConnectionEditorSection.General,
                initialForm = form,
                form = form.copy(name = "Dirty"),
            ),
        )
        setContent {
            RedisTheme {
                ConnectionEditorDialog(
                    state = editor.value,
                    onSelectSection = {},
                    onUpdateForm = {},
                    onRequestClose = {
                        editor.value = editor.value.copy(confirmDiscardVisible = true)
                    },
                    onConfirmDiscard = {
                        editor.value = editor.value.copy(confirmDiscardVisible = false)
                    },
                    onDismissDiscard = {
                        editor.value = editor.value.copy(confirmDiscardVisible = false)
                    },
                    onTest = {},
                    onSave = {},
                    onParseUrl = {},
                )
            }
        }

        onNodeWithTag("connection_editor_cancel").performClick()
        onNodeWithTag("connection_editor_discard_confirm").assertIsDisplayed()
    }

    @Test
    fun fieldError_isDisplayed() = runComposeUiTest {
        val form = ConnectionFormState.defaults()
        setContent {
            RedisTheme {
                ConnectionEditorDialog(
                    state = ConnectionEditorUiState(
                        mode = ConnectionEditorMode.Create,
                        profileId = "connection-1",
                        selectedSection = ConnectionEditorSection.General,
                        initialForm = form,
                        form = form.copy(port = ""),
                        fieldErrors = mapOf("port" to "Port is required"),
                    ),
                    onSelectSection = {},
                    onUpdateForm = {},
                    onRequestClose = {},
                    onConfirmDiscard = {},
                    onDismissDiscard = {},
                    onTest = {},
                    onSave = {},
                    onParseUrl = {},
                )
            }
        }

        onNodeWithTag("connection_editor_port").assertIsDisplayed()
        onNodeWithTag("connection_editor_port").assertTextContains("Port is required", substring = true)
    }

    @Test
    fun sentinelNodes_canAddAndDelete() = runComposeUiTest {
        val form = ConnectionFormState.defaults().copy(
            deploymentMode = DeploymentMode.Sentinel,
            masterName = "mymaster",
            sentinelNodes = listOf(HostPortFormState("n1", "s1", "26379")),
        )
        val editor = mutableStateOf(
            ConnectionEditorUiState(
                mode = ConnectionEditorMode.Create,
                profileId = "connection-1",
                selectedSection = ConnectionEditorSection.Sentinel,
                initialForm = form,
                form = form,
            ),
        )
        setContent {
            RedisTheme {
                ConnectionEditorDialog(
                    state = editor.value,
                    onSelectSection = {},
                    onUpdateForm = { transform ->
                        editor.value = editor.value.copy(form = transform(editor.value.form))
                    },
                    onRequestClose = {},
                    onConfirmDiscard = {},
                    onDismissDiscard = {},
                    onTest = {},
                    onSave = {},
                    onParseUrl = {},
                )
            }
        }

        onNodeWithTag("connection_editor_sentinelNodes_add").performClick()
        assertEquals(2, editor.value.form.sentinelNodes.size)
        onNodeWithTag("connection_editor_sentinelNodes_1_delete").performClick()
        assertEquals(1, editor.value.form.sentinelNodes.size)
    }

    @Test
    fun clusterNodes_canAddAndDelete() = runComposeUiTest {
        val form = ConnectionFormState.defaults().copy(
            deploymentMode = DeploymentMode.Cluster,
            database = "0",
            seedNodes = listOf(HostPortFormState("n1", "c1", "6379")),
        )
        val editor = mutableStateOf(
            ConnectionEditorUiState(
                mode = ConnectionEditorMode.Create,
                profileId = "connection-1",
                selectedSection = ConnectionEditorSection.Cluster,
                initialForm = form,
                form = form,
            ),
        )
        setContent {
            RedisTheme {
                ConnectionEditorDialog(
                    state = editor.value,
                    onSelectSection = {},
                    onUpdateForm = { transform ->
                        editor.value = editor.value.copy(form = transform(editor.value.form))
                    },
                    onRequestClose = {},
                    onConfirmDiscard = {},
                    onDismissDiscard = {},
                    onTest = {},
                    onSave = {},
                    onParseUrl = {},
                )
            }
        }

        onNodeWithTag("connection_editor_seedNodes_add").performClick()
        assertEquals(2, editor.value.form.seedNodes.size)
        onNodeWithTag("connection_editor_seedNodes_1_delete").performClick()
        assertEquals(1, editor.value.form.seedNodes.size)
    }

    @Test
    fun sshPrivateKeyPath_browseFillsPath() = runComposeUiTest {
        val form = ConnectionFormState.defaults().copy(
            sshEnabled = true,
            sshAuthMethod = SshAuthMethod.PrivateKey,
        )
        val editor = mutableStateOf(
            ConnectionEditorUiState(
                mode = ConnectionEditorMode.Create,
                profileId = "connection-1",
                selectedSection = ConnectionEditorSection.General,
                initialForm = form,
                form = form,
            ),
        )
        setContent {
            RedisTheme {
                ConnectionEditorDialog(
                    state = editor.value,
                    onSelectSection = {},
                    onUpdateForm = { transform ->
                        editor.value = editor.value.copy(form = transform(editor.value.form))
                    },
                    onRequestClose = {},
                    onConfirmDiscard = {},
                    onDismissDiscard = {},
                    onTest = {},
                    onSave = {},
                    onParseUrl = {},
                    onPickSshPrivateKeyPath = { "C:/Users/test/.ssh/id_rsa" },
                )
            }
        }

        onNodeWithTag("connection_editor_ssh_private_key_browse")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals("C:/Users/test/.ssh/id_rsa", editor.value.form.sshPrivateKeyPath)
    }
}
