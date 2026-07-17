package org.roberthu.rs.shell.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import org.roberthu.rs.shell.icons.AppIcons
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.ConnectionTagColor
import org.roberthu.rs.domain.ConnectionGroup
import org.roberthu.rs.domain.DatabaseFilterMode
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.KeyListViewMode
import org.roberthu.rs.domain.SshAuthMethod
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.presentation.ConnectionEditorMode
import org.roberthu.rs.presentation.ConnectionFormState
import org.roberthu.rs.presentation.HostPortFormState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralConnectionSection(
    editorMode: ConnectionEditorMode,
    form: ConnectionFormState,
    fieldErrors: Map<String, String>,
    onChange: (ConnectionFormState) -> Unit,
    groups: List<ConnectionGroup> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    val sortedGroups = remember(groups) { groups.sortedBy { it.order } }
    val selectedGroupLabel = sortedGroups.firstOrNull { it.id == form.groupId }?.name
        ?: t(StringKeys.ConnectionEditor.RootGroup)
    val modes = listOf(
        DeploymentMode.Standalone to StringKeys.ConnectionEditor.ModeStandalone,
        DeploymentMode.Sentinel to StringKeys.ConnectionEditor.ModeSentinel,
        DeploymentMode.Cluster to StringKeys.ConnectionEditor.ModeCluster,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = form.name,
            onValueChange = { onChange(form.copy(name = it)) },
            label = { Text(t(StringKeys.ConnectionEditor.NameLabel)) },
            singleLine = true,
            isError = fieldErrors.containsKey("name"),
            supportingText = fieldErrors["name"]?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_name"),
        )

        ExposedDropdownMenuBox(
            expanded = groupExpanded,
            onExpandedChange = { groupExpanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_group"),
        ) {
            OutlinedTextField(
                value = selectedGroupLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(t(StringKeys.ConnectionEditor.GroupLabel)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("connection_editor_group_field"),
            )
            ExposedDropdownMenu(
                expanded = groupExpanded,
                onDismissRequest = { groupExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(t(StringKeys.ConnectionEditor.RootGroup)) },
                    onClick = {
                        groupExpanded = false
                        onChange(form.copy(groupId = null))
                    },
                    modifier = Modifier.testTag("connection_editor_group_root"),
                )
                sortedGroups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        onClick = {
                            groupExpanded = false
                            onChange(form.copy(groupId = group.id))
                        },
                        modifier = Modifier.testTag("connection_editor_group_${group.id}"),
                    )
                }
            }
        }

        if (editorMode == ConnectionEditorMode.Create) {
            Text(
                t(StringKeys.ConnectionEditor.DeploymentModeLabel),
                style = MaterialTheme.typography.labelLarge,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, (mode, labelKey) ->
                    SegmentedButton(
                        selected = form.deploymentMode == mode,
                        onClick = {
                            onChange(
                                form.copy(
                                    deploymentMode = mode,
                                    database = "0",
                                ),
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        modifier = Modifier.testTag("connection_editor_mode_${mode.name.lowercase()}"),
                    ) {
                        Text(t(labelKey))
                    }
                }
            }
        }

        if (form.deploymentMode == DeploymentMode.Standalone) {
            OutlinedTextField(
                value = form.host,
                onValueChange = { onChange(form.copy(host = it)) },
                label = { Text(t(StringKeys.ConnectionEditor.HostLabel)) },
                singleLine = true,
                isError = fieldErrors.containsKey("host"),
                supportingText = fieldErrors["host"]?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_editor_host"),
            )
            OutlinedTextField(
                value = form.port,
                onValueChange = { onChange(form.copy(port = it)) },
                label = { Text(t(StringKeys.ConnectionEditor.PortLabel)) },
                singleLine = true,
                isError = fieldErrors.containsKey("port"),
                supportingText = fieldErrors["port"]?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_editor_port"),
            )
        }

        OutlinedTextField(
            value = form.username,
            onValueChange = { onChange(form.copy(username = it)) },
            label = { Text(t(StringKeys.ConnectionEditor.UsernameLabel)) },
            placeholder = { Text(t(StringKeys.ConnectionEditor.UsernamePlaceholder)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_username"),
        )

        OutlinedTextField(
            value = form.password,
            onValueChange = { onChange(form.copy(password = it)) },
            label = { Text(t(StringKeys.ConnectionEditor.PasswordLabel)) },
            placeholder = { Text(t(StringKeys.ConnectionEditor.PasswordPlaceholder)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.testTag("connection_editor_password_visibility"),
                ) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            AppIcons.VisibilityOff
                        } else {
                            AppIcons.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            t(StringKeys.ConnectionEditor.HidePassword)
                        } else {
                            t(StringKeys.ConnectionEditor.ShowPassword)
                        },
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_password"),
        )
    }
}

@Composable
fun SshConnectionSection(
    form: ConnectionFormState,
    fieldErrors: Map<String, String>,
    onChange: (ConnectionFormState) -> Unit,
    onPickSshPrivateKeyPath: () -> String? = { null },
    modifier: Modifier = Modifier,
) {
    val standalone = form.deploymentMode == DeploymentMode.Standalone
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!standalone) {
            Text(
                t(StringKeys.ConnectionEditor.SshStandaloneHint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(t(StringKeys.ConnectionEditor.SshTunnelToggle), modifier = Modifier.weight(1f))
            Switch(
                checked = form.sshEnabled,
                onCheckedChange = { onChange(form.copy(sshEnabled = it)) },
                modifier = Modifier.testTag("connection_editor_ssh_enabled"),
            )
        }

        if (form.sshEnabled) {
            SshTunnelSection(
                form = form,
                fieldErrors = fieldErrors,
                onChange = onChange,
                onPickSshPrivateKeyPath = onPickSshPrivateKeyPath,
            )
        }
    }
}

@Composable
private fun SshTunnelSection(
    form: ConnectionFormState,
    fieldErrors: Map<String, String>,
    onChange: (ConnectionFormState) -> Unit,
    onPickSshPrivateKeyPath: () -> String?,
) {
    var sshPasswordVisible by remember { mutableStateOf(false) }
    var passphraseVisible by remember { mutableStateOf(false) }
    val authMethods = listOf(
        SshAuthMethod.Password to StringKeys.ConnectionEditor.SshAuthPassword,
        SshAuthMethod.PrivateKey to StringKeys.ConnectionEditor.SshAuthPrivateKey,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connection_editor_ssh_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider()
        Text(t(StringKeys.ConnectionEditor.SshSectionTitle), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = form.sshHost,
            onValueChange = { onChange(form.copy(sshHost = it)) },
            label = { Text(t(StringKeys.ConnectionEditor.SshHostLabel)) },
            singleLine = true,
            isError = fieldErrors.containsKey("sshHost"),
            supportingText = fieldErrors["sshHost"]?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_ssh_host"),
        )
        OutlinedTextField(
            value = form.sshPort,
            onValueChange = { onChange(form.copy(sshPort = it)) },
            label = { Text(t(StringKeys.ConnectionEditor.SshPortLabel)) },
            singleLine = true,
            isError = fieldErrors.containsKey("sshPort"),
            supportingText = fieldErrors["sshPort"]?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_ssh_port"),
        )
        OutlinedTextField(
            value = form.sshUsername,
            onValueChange = { onChange(form.copy(sshUsername = it)) },
            label = { Text(t(StringKeys.ConnectionEditor.SshUsernameLabel)) },
            singleLine = true,
            isError = fieldErrors.containsKey("sshUsername"),
            supportingText = fieldErrors["sshUsername"]?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_ssh_username"),
        )

        Text(t(StringKeys.ConnectionEditor.SshAuthMethodLabel), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            authMethods.forEachIndexed { index, (method, labelKey) ->
                SegmentedButton(
                    selected = form.sshAuthMethod == method,
                    onClick = { onChange(form.copy(sshAuthMethod = method)) },
                    shape = SegmentedButtonDefaults.itemShape(index, authMethods.size),
                    modifier = Modifier.testTag(
                        "connection_editor_ssh_auth_${method.name.lowercase()}",
                    ),
                ) {
                    Text(t(labelKey))
                }
            }
        }

        if (form.sshAuthMethod == SshAuthMethod.Password) {
            OutlinedTextField(
                value = form.sshPassword,
                onValueChange = { onChange(form.copy(sshPassword = it)) },
                label = { Text(t(StringKeys.ConnectionEditor.SshPasswordLabel)) },
                singleLine = true,
                visualTransformation = if (sshPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { sshPasswordVisible = !sshPasswordVisible }) {
                        Icon(
                            imageVector = if (sshPasswordVisible) {
                                AppIcons.VisibilityOff
                            } else {
                                AppIcons.Visibility
                            },
                            contentDescription = null,
                        )
                    }
                },
                isError = fieldErrors.containsKey("sshPassword"),
                supportingText = fieldErrors["sshPassword"]?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_editor_ssh_password"),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = form.sshPrivateKeyPath,
                    onValueChange = { onChange(form.copy(sshPrivateKeyPath = it)) },
                    label = { Text(t(StringKeys.ConnectionEditor.SshPrivateKeyPathLabel)) },
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("connection_editor_ssh_private_key_path"),
                )
                OutlinedButton(
                    onClick = {
                        onPickSshPrivateKeyPath()?.let { path ->
                            onChange(form.copy(sshPrivateKeyPath = path))
                        }
                    },
                    modifier = Modifier.testTag("connection_editor_ssh_private_key_browse"),
                ) {
                    Icon(
                        imageVector = AppIcons.Folder,
                        contentDescription = t(StringKeys.ConnectionEditor.SshPrivateKeyBrowseDescription),
                    )
                    Text(" ${t(StringKeys.ConnectionEditor.SshPrivateKeyBrowse)}")
                }
            }
            OutlinedTextField(
                value = form.sshPrivateKey,
                onValueChange = { onChange(form.copy(sshPrivateKey = it)) },
                label = { Text(t(StringKeys.ConnectionEditor.SshPrivateKeyLabel)) },
                minLines = 3,
                isError = fieldErrors.containsKey("sshPrivateKey"),
                supportingText = fieldErrors["sshPrivateKey"]?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_editor_ssh_private_key"),
            )
            OutlinedTextField(
                value = form.sshPrivateKeyPassphrase,
                onValueChange = { onChange(form.copy(sshPrivateKeyPassphrase = it)) },
                label = { Text(t(StringKeys.ConnectionEditor.SshPrivateKeyPassphraseLabel)) },
                singleLine = true,
                visualTransformation = if (passphraseVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                        Icon(
                            imageVector = if (passphraseVisible) {
                                AppIcons.VisibilityOff
                            } else {
                                AppIcons.Visibility
                            },
                            contentDescription = null,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connection_editor_ssh_passphrase"),
            )
        }

        OutlinedTextField(
            value = form.sshConnectTimeoutMs,
            onValueChange = { onChange(form.copy(sshConnectTimeoutMs = it)) },
            label = { Text(t(StringKeys.ConnectionEditor.SshConnectTimeoutLabel)) },
            singleLine = true,
            isError = fieldErrors.containsKey("sshConnectTimeoutMs"),
            supportingText = fieldErrors["sshConnectTimeoutMs"]?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_ssh_connect_timeout"),
        )
    }
}

@Composable
fun AdvancedConnectionSection(
    form: ConnectionFormState,
    fieldErrors: Map<String, String>,
    onChange: (ConnectionFormState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyListViews = listOf(
        KeyListViewMode.Tree to StringKeys.ConnectionEditor.KeyListViewTree,
        KeyListViewMode.Flat to StringKeys.ConnectionEditor.KeyListViewFlat,
    )
    val databaseFilterModes = listOf(
        DatabaseFilterMode.ShowAll to StringKeys.ConnectionEditor.DatabaseFilterShowAll,
        DatabaseFilterMode.ShowSpecified to StringKeys.ConnectionEditor.DatabaseFilterShowSpecified,
        DatabaseFilterMode.HideSpecified to StringKeys.ConnectionEditor.DatabaseFilterHideSpecified,
    )
    val showDatabaseFilterText = form.databaseFilterMode != DatabaseFilterMode.ShowAll

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AdaptiveTwoColumnRow(
            left = {
                LabeledTextField(
                    label = t(StringKeys.ConnectionEditor.KeyPatternLabel),
                    value = form.keyPattern,
                    onValueChange = { onChange(form.copy(keyPattern = it)) },
                    testTag = "connection_editor_key_pattern",
                )
            },
            right = {
                LabeledTextField(
                    label = t(StringKeys.ConnectionEditor.KeySeparatorLabel),
                    value = form.keySeparator,
                    onValueChange = { onChange(form.copy(keySeparator = it)) },
                    testTag = "connection_editor_key_separator",
                )
            },
        )

        AdaptiveTwoColumnRow(
            left = {
                LabeledTextField(
                    label = t(StringKeys.ConnectionEditor.ConnectTimeoutSecLabel),
                    value = form.connectTimeoutSec,
                    onValueChange = { onChange(form.copy(connectTimeoutSec = it)) },
                    error = fieldErrors["connectTimeoutSec"],
                    testTag = "connection_editor_connect_timeout",
                    suffix = t(StringKeys.ConnectionEditor.SecondsSuffix),
                )
            },
            right = {
                LabeledTextField(
                    label = t(StringKeys.ConnectionEditor.CommandTimeoutSecLabel),
                    value = form.commandTimeoutSec,
                    onValueChange = { onChange(form.copy(commandTimeoutSec = it)) },
                    error = fieldErrors["commandTimeoutSec"],
                    testTag = "connection_editor_command_timeout",
                    suffix = t(StringKeys.ConnectionEditor.SecondsSuffix),
                )
            },
        )

        AdaptiveTwoColumnRow(
            left = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        t(StringKeys.ConnectionEditor.KeyListViewLabel),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        keyListViews.forEachIndexed { index, (mode, labelKey) ->
                            SegmentedButton(
                                selected = form.keyListView == mode,
                                onClick = { onChange(form.copy(keyListView = mode)) },
                                shape = SegmentedButtonDefaults.itemShape(index, keyListViews.size),
                                modifier = Modifier.testTag(
                                    "connection_editor_key_list_view_${mode.name.lowercase()}",
                                ),
                            ) {
                                Text(t(labelKey))
                            }
                        }
                    }
                }
            },
            right = {
                LabeledTextField(
                    label = t(StringKeys.ConnectionEditor.KeyLoadBatchSizeLabel),
                    value = form.keyLoadBatchSize,
                    onValueChange = { onChange(form.copy(keyLoadBatchSize = it)) },
                    error = fieldErrors["keyLoadBatchSize"],
                    testTag = "connection_editor_key_load_batch_size",
                )
            },
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                t(StringKeys.ConnectionEditor.DatabaseFilterModeLabel),
                style = MaterialTheme.typography.labelLarge,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                databaseFilterModes.forEachIndexed { index, (mode, labelKey) ->
                    SegmentedButton(
                        selected = form.databaseFilterMode == mode,
                        onClick = { onChange(form.copy(databaseFilterMode = mode)) },
                        shape = SegmentedButtonDefaults.itemShape(index, databaseFilterModes.size),
                        modifier = Modifier.testTag(
                            "connection_editor_database_filter_${mode.name.lowercase()}",
                        ),
                    ) {
                        Text(t(labelKey))
                    }
                }
            }
        }

        if (showDatabaseFilterText) {
            LabeledTextField(
                label = t(StringKeys.ConnectionEditor.DatabaseFilterTextLabel),
                value = form.databaseFilterText,
                onValueChange = { onChange(form.copy(databaseFilterText = it)) },
                error = fieldErrors["databaseFilterText"],
                testTag = "connection_editor_database_filter_text",
                placeholder = t(StringKeys.ConnectionEditor.DatabaseFilterTextPlaceholder),
            )
        }

        TagColorSelector(
            selected = form.tagColor,
            onSelect = { onChange(form.copy(tagColor = it)) },
        )
    }
}

@Composable
fun PlaceholderConnectionSection(
    modifier: Modifier = Modifier,
) {
    Text(
        text = t(StringKeys.ConnectionEditor.ComingSoon),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun TlsConnectionSection(
    form: ConnectionFormState,
    onChange: (ConnectionFormState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(t(StringKeys.ConnectionEditor.TlsEnableLabel), modifier = Modifier.weight(1f))
            Switch(
                checked = form.tlsEnabled,
                onCheckedChange = { enabled ->
                    onChange(
                        form.copy(
                            tlsEnabled = enabled,
                            verifyPeer = if (enabled) form.verifyPeer else form.verifyPeer,
                        ),
                    )
                },
                modifier = Modifier.testTag("connection_editor_tls_enabled"),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(t(StringKeys.ConnectionEditor.TlsVerifyPeerLabel))
                Text(
                    t(StringKeys.ConnectionEditor.TlsVerifyPeerDesc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = form.verifyPeer,
                onCheckedChange = { onChange(form.copy(verifyPeer = it)) },
                enabled = form.tlsEnabled,
                modifier = Modifier.testTag("connection_editor_verify_peer"),
            )
        }
    }
}

@Composable
fun SentinelConnectionSection(
    form: ConnectionFormState,
    fieldErrors: Map<String, String>,
    onChange: (ConnectionFormState) -> Unit,
    newNodeId: () -> String,
    modifier: Modifier = Modifier,
) {
    val enabled = form.deploymentMode == DeploymentMode.Sentinel
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!enabled) {
            Text(
                t(StringKeys.ConnectionEditor.SentinelModeHint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = form.masterName,
            onValueChange = { onChange(form.copy(masterName = it)) },
            label = { Text(t(StringKeys.ConnectionEditor.MasterNameLabel)) },
            enabled = enabled,
            singleLine = true,
            isError = fieldErrors.containsKey("masterName"),
            supportingText = fieldErrors["masterName"]?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_master_name"),
        )
        Text(t(StringKeys.ConnectionEditor.SentinelNodesLabel), style = MaterialTheme.typography.labelLarge)
        HostPortListEditor(
            nodes = form.sentinelNodes,
            enabled = enabled,
            fieldErrors = fieldErrors,
            listKey = "sentinelNodes",
            defaultPort = "26379",
            onChange = { nodes: List<HostPortFormState> -> onChange(form.copy(sentinelNodes = nodes)) },
            newNodeId = newNodeId,
        )
    }
}

@Composable
fun ClusterConnectionSection(
    form: ConnectionFormState,
    fieldErrors: Map<String, String>,
    onChange: (ConnectionFormState) -> Unit,
    newNodeId: () -> String,
    modifier: Modifier = Modifier,
) {
    val enabled = form.deploymentMode == DeploymentMode.Cluster
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!enabled) {
            Text(
                t(StringKeys.ConnectionEditor.ClusterModeHint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(t(StringKeys.ConnectionEditor.ClusterSeedNodesLabel), style = MaterialTheme.typography.labelLarge)
        HostPortListEditor(
            nodes = form.seedNodes,
            enabled = enabled,
            fieldErrors = fieldErrors,
            listKey = "seedNodes",
            defaultPort = "6379",
            onChange = { nodes: List<HostPortFormState> -> onChange(form.copy(seedNodes = nodes)) },
            newNodeId = newNodeId,
        )
    }
}

@Composable
private fun AdaptiveTwoColumnRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth >= 480.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) { left() }
                Column(modifier = Modifier.weight(1f)) { right() }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                left()
                right()
            }
        }
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    placeholder: String? = null,
    suffix: String? = null,
    singleLine: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            placeholder = placeholder?.let { { Text(it) } },
            suffix = suffix?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
        )
    }
}

@Composable
private fun TagColorSelector(
    selected: ConnectionTagColor,
    onSelect: (ConnectionTagColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tagColors = listOf(
        ConnectionTagColor.None to null,
        ConnectionTagColor.Red to Color(0xFFE53935),
        ConnectionTagColor.Orange to Color(0xFFFB8C00),
        ConnectionTagColor.Yellow to Color(0xFFFDD835),
        ConnectionTagColor.Green to Color(0xFF43A047),
        ConnectionTagColor.Blue to Color(0xFF1E88E5),
        ConnectionTagColor.Purple to Color(0xFF8E24AA),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            t(StringKeys.ConnectionEditor.TagColorLabel),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tagColors.forEach { (color, displayColor) ->
                TagColorChip(
                    color = color,
                    displayColor = displayColor,
                    selected = selected == color,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun TagColorChip(
    color: ConnectionTagColor,
    displayColor: Color?,
    selected: Boolean,
    onSelect: (ConnectionTagColor) -> Unit,
) {
    val borderWidth = if (selected) 3.dp else 1.dp
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val chipModifier = Modifier
        .size(32.dp)
        .border(borderWidth, borderColor, CircleShape)
        .clickable { onSelect(color) }
        .testTag("connection_editor_tag_color_${color.name.lowercase()}")

    if (color == ConnectionTagColor.None) {
        Box(
            modifier = chipModifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Box(
            modifier = chipModifier.padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(displayColor!!, CircleShape),
            )
        }
    }
}
