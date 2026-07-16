package org.roberthu.rs.shell.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.presentation.ConnectionEditorMode
import org.roberthu.rs.presentation.ConnectionEditorSection
import org.roberthu.rs.presentation.ConnectionEditorUiState
import org.roberthu.rs.presentation.ConnectionFormState

@Composable
fun ConnectionEditorDialog(
    state: ConnectionEditorUiState,
    onSelectSection: (ConnectionEditorSection) -> Unit,
    onUpdateForm: ((ConnectionFormState) -> ConnectionFormState) -> Unit,
    onRequestClose: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscard: () -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onParseUrl: (String) -> Unit,
    onPickSshPrivateKeyPath: () -> String? = { null },
) {
    val clipboardManager = LocalClipboardManager.current
    val busy = state.testing || state.saving

    Dialog(
        onDismissRequest = onRequestClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        // tonalElevation tints Surface with primary (Redis red) — that caused the pink wash.
        // Use an opaque elevated surface + shadow only; never blend primary into the dialog plate.
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 760.dp)
                .heightIn(max = 680.dp)
                .width(760.dp)
                .height(680.dp)
                .testTag("connection_editor_dialog"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                EditorHeader(
                    title = when (state.mode) {
                        ConnectionEditorMode.Create -> t(StringKeys.ConnectionEditor.TitleNew)
                        ConnectionEditorMode.Edit -> t(StringKeys.ConnectionEditor.TitleEdit)
                    },
                    onClose = onRequestClose,
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    SectionNavigation(
                        selected = state.selectedSection,
                        onSelect = onSelectSection,
                        modifier = Modifier
                            .width(136.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                    )
                    VerticalDivider()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        state.globalError?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("connection_editor_global_error"),
                            )
                        }
                        if (state.testSucceeded) {
                            Text(
                                text = t(StringKeys.ConnectionEditor.TestSuccess),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.testTag("connection_editor_test_success"),
                            )
                        }
                        when (state.selectedSection) {
                            ConnectionEditorSection.General -> GeneralConnectionSection(
                                form = state.form,
                                fieldErrors = state.fieldErrors,
                                onChange = { form -> onUpdateForm { form } },
                                onPickSshPrivateKeyPath = onPickSshPrivateKeyPath,
                            )
                            ConnectionEditorSection.Advanced -> AdvancedConnectionSection(
                                form = state.form,
                                fieldErrors = state.fieldErrors,
                                onChange = { form -> onUpdateForm { form } },
                            )
                            ConnectionEditorSection.Tls -> TlsConnectionSection(
                                form = state.form,
                                onChange = { form -> onUpdateForm { form } },
                            )
                            ConnectionEditorSection.Sentinel -> SentinelConnectionSection(
                                form = state.form,
                                fieldErrors = state.fieldErrors,
                                onChange = { form -> onUpdateForm { form } },
                                newNodeId = { ConnectionFormState.newNodeId() },
                            )
                            ConnectionEditorSection.Cluster -> ClusterConnectionSection(
                                form = state.form,
                                fieldErrors = state.fieldErrors,
                                onChange = { form -> onUpdateForm { form } },
                                newNodeId = { ConnectionFormState.newNodeId() },
                            )
                        }
                    }
                }
                HorizontalDivider()
                EditorFooter(
                    busy = busy,
                    testing = state.testing,
                    saving = state.saving,
                    onTest = onTest,
                    onParseUrl = {
                        val text = clipboardManager.getText()?.text.orEmpty()
                        onParseUrl(text)
                    },
                    onCancel = onRequestClose,
                    onSave = onSave,
                )
            }
        }
    }

    if (state.confirmDiscardVisible) {
        AlertDialog(
            onDismissRequest = onDismissDiscard,
            title = { Text(t(StringKeys.ConnectionEditor.DiscardTitle)) },
            text = { Text(t(StringKeys.ConnectionEditor.DiscardMessage)) },
            confirmButton = {
                Button(
                    onClick = onConfirmDiscard,
                    modifier = Modifier.testTag("connection_editor_discard_confirm"),
                ) {
                    Text(t(StringKeys.ConnectionEditor.DiscardConfirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDiscard,
                    modifier = Modifier.testTag("connection_editor_discard_cancel"),
                ) {
                    Text(t(StringKeys.ConnectionEditor.DiscardContinueEditing))
                }
            },
        )
    }
}

@Composable
private fun EditorHeader(
    title: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .testTag("connection_editor_title"),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.testTag("connection_editor_close"),
        ) {
            Icon(Icons.Filled.Close, contentDescription = t(StringKeys.ConnectionEditor.Close))
        }
    }
}

@Composable
private fun SectionNavigation(
    selected: ConnectionEditorSection,
    onSelect: (ConnectionEditorSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        ConnectionEditorSection.General to (StringKeys.ConnectionEditor.SectionGeneral to "connection_editor_section_general"),
        ConnectionEditorSection.Advanced to (StringKeys.ConnectionEditor.SectionAdvanced to "connection_editor_section_advanced"),
        ConnectionEditorSection.Tls to (StringKeys.ConnectionEditor.SectionTls to "connection_editor_section_tls"),
        ConnectionEditorSection.Sentinel to (StringKeys.ConnectionEditor.SectionSentinel to "connection_editor_section_sentinel"),
        ConnectionEditorSection.Cluster to (StringKeys.ConnectionEditor.SectionCluster to "connection_editor_section_cluster"),
    )
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        items.forEach { (section, labelAndTag) ->
            val (labelKey, tag) = labelAndTag
            val isSelected = section == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    )
                    .clickable { onSelect(section) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag(tag),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(18.dp)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                        ),
                )
                Text(
                    text = t(labelKey),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun EditorFooter(
    busy: Boolean,
    testing: Boolean,
    saving: Boolean,
    onTest: () -> Unit,
    onParseUrl: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onTest,
            enabled = !busy,
            modifier = Modifier.testTag("connection_editor_test"),
        ) {
            if (testing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
            Text(t(StringKeys.ConnectionEditor.TestConnection))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onParseUrl,
                enabled = !busy,
                modifier = Modifier.testTag("connection_editor_parse_url"),
            ) {
                Text(t(StringKeys.ConnectionEditor.ParseClipboardUrl))
            }
            TextButton(
                onClick = onCancel,
                enabled = !busy,
                modifier = Modifier.testTag("connection_editor_cancel"),
            ) {
                Text(t(StringKeys.ConnectionEditor.Cancel))
            }
            Button(
                onClick = onSave,
                enabled = !busy,
                modifier = Modifier.testTag("connection_editor_save"),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(t(StringKeys.ConnectionEditor.Save))
            }
        }
    }
}
