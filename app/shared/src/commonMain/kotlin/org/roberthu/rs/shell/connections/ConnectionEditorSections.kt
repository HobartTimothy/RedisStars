package org.roberthu.rs.shell.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.SshAuthMethod
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.presentation.ConnectionFormState
import org.roberthu.rs.presentation.HostPortFormState

@Composable
fun GeneralConnectionSection(
    form: ConnectionFormState,
    fieldErrors: Map<String, String>,
    onChange: (ConnectionFormState) -> Unit,
    onPickSshPrivateKeyPath: () -> String? = { null },
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
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

        Text(t(StringKeys.ConnectionEditor.DeploymentModeLabel), style = MaterialTheme.typography.labelLarge)
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
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
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

        if (form.deploymentMode == DeploymentMode.Standalone) {
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
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
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
                        imageVector = Icons.Filled.Folder,
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
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = form.clientName,
            onValueChange = { onChange(form.copy(clientName = it)) },
            label = { Text(t(StringKeys.ConnectionEditor.ClientNameLabel)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connection_editor_client_name"),
        )
        TimeoutField(
            value = form.connectTimeoutMs,
            label = t(StringKeys.ConnectionEditor.ConnectTimeoutLabel),
            error = fieldErrors["connectTimeoutMs"],
            testTag = "connection_editor_connect_timeout",
            onValueChange = { onChange(form.copy(connectTimeoutMs = it)) },
        )
        TimeoutField(
            value = form.commandTimeoutMs,
            label = t(StringKeys.ConnectionEditor.CommandTimeoutLabel),
            error = fieldErrors["commandTimeoutMs"],
            testTag = "connection_editor_command_timeout",
            onValueChange = { onChange(form.copy(commandTimeoutMs = it)) },
        )
        TimeoutField(
            value = form.reconnectTimeoutMs,
            label = t(StringKeys.ConnectionEditor.ReconnectTimeoutLabel),
            error = fieldErrors["reconnectTimeoutMs"],
            testTag = "connection_editor_reconnect_timeout",
            onValueChange = { onChange(form.copy(reconnectTimeoutMs = it)) },
        )
    }
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
private fun TimeoutField(
    value: String,
    label: String,
    error: String?,
    testTag: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}
