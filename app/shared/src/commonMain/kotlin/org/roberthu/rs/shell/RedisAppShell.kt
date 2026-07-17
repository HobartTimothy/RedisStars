package org.roberthu.rs.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t
import org.roberthu.rs.platform.FileSavePicker
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.presentation.RuntimeLogsViewModel
import org.roberthu.rs.theme.connectionIndicatorColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedisAppShell(
    state: ShellUiState,
    onAction: (ShellUiAction) -> Unit,
    modifier: Modifier = Modifier,
    connectionState: ConnectionState = ConnectionState.Disconnected,
    runtimeLogsViewModel: RuntimeLogsViewModel? = null,
    runtimeLogSavePicker: FileSavePicker = FileSavePicker { _, _ -> null },
    buildInfo: org.roberthu.rs.presentation.BuildInfo = PreviewBuildInfo,
    connectionsContent: @Composable () -> Unit,
) {
    val layoutType = rememberShellNavigationSuiteType()
    val usePinnedLeftRail = shouldUsePinnedLeftRail(layoutType)

    if (usePinnedLeftRail) {
        // Explicit left | content row. Top-aligned so the rail never floats mid-window.
        Row(
            modifier = modifier
                .fillMaxSize()
                .testTag("shell_root_row"),
            verticalAlignment = Alignment.Top,
        ) {
            ShellNavigationRail(
                destination = state.destination,
                railCollapsed = state.railCollapsed,
                onDestinationSelected = { onAction(ShellUiAction.Navigate(it)) },
                onToggleCollapsed = { onAction(ShellUiAction.ToggleRail) },
            )
            ShellMainContent(
                state = state,
                onAction = onAction,
                connectionState = connectionState,
                runtimeLogsViewModel = runtimeLogsViewModel,
                runtimeLogSavePicker = runtimeLogSavePicker,
                connectionsContent = connectionsContent,
                buildInfo = buildInfo,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag("main_content"),
            )
        }
    } else {
        NavigationSuiteScaffold(
            modifier = modifier.fillMaxSize(),
            layoutType = layoutType,
            navigationSuiteItems = {
                ShellDestination.primaryDestinations.forEach { item ->
                    item(
                        selected = state.destination == item,
                        onClick = { onAction(ShellUiAction.Navigate(item)) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label(),
                            )
                        },
                        label = { Text(item.label()) },
                    )
                }
                item(
                    selected = state.destination == ShellDestination.Settings,
                    onClick = {
                        onAction(ShellUiAction.Navigate(ShellDestination.Settings))
                    },
                    icon = {
                        Icon(
                            imageVector = ShellDestination.Settings.icon,
                            contentDescription = ShellDestination.Settings.label(),
                        )
                    },
                    label = { Text(ShellDestination.Settings.label()) },
                )
            },
        ) {
            ShellMainContent(
                state = state,
                onAction = onAction,
                connectionState = connectionState,
                runtimeLogsViewModel = runtimeLogsViewModel,
                runtimeLogSavePicker = runtimeLogSavePicker,
                connectionsContent = connectionsContent,
                buildInfo = buildInfo,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("main_content"),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellMainContent(
    state: ShellUiState,
    onAction: (ShellUiAction) -> Unit,
    connectionState: ConnectionState,
    runtimeLogsViewModel: RuntimeLogsViewModel?,
    runtimeLogSavePicker: FileSavePicker,
    connectionsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    buildInfo: org.roberthu.rs.presentation.BuildInfo = PreviewBuildInfo,
) {
    val connected = connectionState is ConnectionState.Connected
    val connectedStateDescription = t(StringKeys.Shell.StatusConnected)
    val disconnectedStateDescription = t(StringKeys.Shell.StatusDisconnected)
    val statusText = when (connectionState) {
        is ConnectionState.Connected -> connectionState.displayName
        ConnectionState.Connecting -> t(StringKeys.Shell.StatusConnecting)
        is ConnectionState.Reconnecting -> t(StringKeys.Shell.StatusReconnecting, connectionState.attempt)
        is ConnectionState.Failed -> t(StringKeys.Shell.StatusFailed)
        ConnectionState.Disconnected -> t(StringKeys.Shell.StatusDisconnected)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    connectionIndicatorColor(
                                        connected = connected,
                                        onSurface = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                                .semantics {
                                    stateDescription = if (connected) {
                                        connectedStateDescription
                                    } else {
                                        disconnectedStateDescription
                                    }
                                },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                expandedHeight = 48.dp,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            state.bannerError?.let { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onAction(ShellUiAction.DismissError) }) {
                        Text(t(StringKeys.Shell.Dismiss))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopStart,
            ) {
                when (state.destination) {
                    ShellDestination.Connections -> {
                        connectionsContent()
                    }

                    ShellDestination.Monitor -> MonitorPane()
                    ShellDestination.RuntimeLogs -> RuntimeLogsPane(
                        viewModel = runtimeLogsViewModel,
                        runtimeLogSavePicker = runtimeLogSavePicker,
                    )
                    ShellDestination.Settings -> SettingsScreen(
                        darkMode = state.darkMode,
                        onDarkModeChange = {
                            onAction(ShellUiAction.SetDarkMode(it))
                        },
                        autoConnect = state.autoConnect,
                        onAutoConnectChange = {
                            onAction(ShellUiAction.SetAutoConnect(it))
                        },
                        language = state.language,
                        onLanguageChange = {
                            onAction(ShellUiAction.SetLanguage(it))
                        },
                        buildInfo = buildInfo,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("settings_screen"),
                    )
                }
            }
        }
    }
}
