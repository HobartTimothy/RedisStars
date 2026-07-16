package org.roberthu.rs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.roberthu.rs.i18n.AppI18n
import org.roberthu.rs.i18n.ProvideAppLanguage
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.platform.FilePathPicker
import org.roberthu.rs.platform.TextFileImporter
import org.roberthu.rs.presentation.AppContainer
import org.roberthu.rs.port.ConnectionState
import org.roberthu.rs.shell.RedisAppShell
import org.roberthu.rs.shell.connections.ConnectionsWorkspace
import org.roberthu.rs.theme.RedisTheme

@Composable
@Preview
fun App(modifier: Modifier = Modifier) {
    val container = remember { AppContainer.preview() }
    App(container = container, modifier = modifier)
}

@Composable
fun App(
    container: AppContainer,
    modifier: Modifier = Modifier,
    textFileImporter: TextFileImporter = TextFileImporter { null },
    sshPrivateKeyPathPicker: FilePathPicker = FilePathPicker { _ -> null },
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(container, scope) {
        container.createShellViewModel(scope)
    }
    val connectionsViewModel = remember(container, scope) {
        container.createConnectionsViewModel(scope)
    }
    val keyBrowserViewModel = remember(container, scope) {
        container.createKeyBrowserViewModel(scope)
    }
    val keyDetailViewModel = remember(container, scope) {
        container.createKeyDetailViewModel(scope)
    }
    val state by viewModel.state.collectAsState()
    val connectionsState = connectionsViewModel?.state?.collectAsState()?.value

    ProvideAppLanguage(language = state.language) {
        RedisTheme(darkTheme = state.darkMode) {
            RedisAppShell(
                state = state,
                onAction = viewModel::dispatch,
                modifier = modifier.fillMaxSize(),
                connectionState = connectionsState?.connectionState ?: ConnectionState.Disconnected,
                connectionsContent = {
                    ConnectionsWorkspace(
                        connections = connectionsViewModel,
                        browser = keyBrowserViewModel,
                        detail = keyDetailViewModel,
                        onImportText = { textFileImporter.importTextFile() },
                        onPickSshPrivateKeyPath = {
                            sshPrivateKeyPathPicker.pickFilePath(
                                title = AppI18n.t(StringKeys.ConnectionEditor.SshPickPrivateKeyTitle),
                            )
                        },
                    )
                },
            )
        }
    }
}
