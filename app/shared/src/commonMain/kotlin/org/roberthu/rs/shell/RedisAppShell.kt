package org.roberthu.rs.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.roberthu.rs.theme.connectionIndicatorColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedisAppShell(
    connectionState: ConnectionUiState,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var destinationName by rememberSaveable {
        mutableStateOf(ShellDestination.Connections.name)
    }
    var railCollapsed by rememberSaveable { mutableStateOf(false) }
    var autoConnect by remember { mutableStateOf(false) }

    val destination = ShellDestination.entries.firstOrNull { it.name == destinationName }
        ?: ShellDestination.Connections

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
                destination = destination,
                railCollapsed = railCollapsed,
                onDestinationSelected = { selected ->
                    destinationName = selected.name
                },
                onToggleCollapsed = {
                    railCollapsed = !railCollapsed
                },
            )
            ShellMainContent(
                connectionState = connectionState,
                destination = destination,
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                autoConnect = autoConnect,
                onAutoConnectChange = { autoConnect = it },
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
                        selected = destination == item,
                        onClick = { destinationName = item.name },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
                item(
                    selected = destination == ShellDestination.Settings,
                    onClick = { destinationName = ShellDestination.Settings.name },
                    icon = {
                        Icon(
                            imageVector = ShellDestination.Settings.icon,
                            contentDescription = ShellDestination.Settings.label,
                        )
                    },
                    label = { Text(ShellDestination.Settings.label) },
                )
            },
        ) {
            ShellMainContent(
                connectionState = connectionState,
                destination = destination,
                darkMode = darkMode,
                onDarkModeChange = onDarkModeChange,
                autoConnect = autoConnect,
                onAutoConnectChange = { autoConnect = it },
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
    connectionState: ConnectionUiState,
    destination: ShellDestination,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    autoConnect: Boolean,
    onAutoConnectChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                                        connected = connectionState.connected,
                                        onSurface = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                                .semantics {
                                    stateDescription = if (connectionState.connected) {
                                        "Connected"
                                    } else {
                                        "Disconnected"
                                    }
                                },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = connectionState.nodeName,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopStart,
        ) {
            when (destination) {
                ShellDestination.Connections -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top,
                    ) {
                        KeyBrowserPane()
                        WorkspacePane(modifier = Modifier.weight(1f))
                    }
                }

                ShellDestination.Monitor -> MonitorPane()
                ShellDestination.SlowLog -> SlowLogPane()
                ShellDestination.Settings -> SettingsScreen(
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    autoConnect = autoConnect,
                    onAutoConnectChange = onAutoConnectChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("settings_screen"),
                )
            }
        }
    }
}
