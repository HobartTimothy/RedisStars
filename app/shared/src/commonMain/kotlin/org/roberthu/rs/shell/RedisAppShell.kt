package org.roberthu.rs.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.roberthu.rs.theme.connectionIndicatorColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedisAppShell(
    connectionState: ConnectionUiState,
    modifier: Modifier = Modifier,
) {
    var destination by rememberSaveable { mutableStateOf(ShellDestination.Connections) }
    val layoutType = rememberShellNavigationSuiteType()

    NavigationSuiteScaffold(
        modifier = modifier.fillMaxSize(),
        layoutType = layoutType,
        navigationSuiteItems = {
            ShellDestination.entries.forEach { item ->
                item(
                    selected = destination == item,
                    onClick = { destination = item },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                        )
                    },
                    label = { Text(item.label) },
                )
            }
        },
    ) {
        Scaffold(
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
                                    ),
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
            ) {
                when (destination) {
                    ShellDestination.Connections -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            KeyBrowserPane()
                            WorkspacePane(modifier = Modifier.weight(1f))
                        }
                    }

                    ShellDestination.Monitor -> MonitorPane()
                    ShellDestination.SlowLog -> SlowLogPane()
                }
            }
        }
    }
}
