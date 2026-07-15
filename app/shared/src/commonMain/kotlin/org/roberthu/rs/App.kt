package org.roberthu.rs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.roberthu.rs.shell.ConnectionUiState
import org.roberthu.rs.shell.RedisAppShell
import org.roberthu.rs.theme.RedisTheme

@Composable
@Preview
fun App(modifier: Modifier = Modifier) {
    var darkMode by remember { mutableStateOf(true) }

    RedisTheme(darkTheme = darkMode) {
        RedisAppShell(
            connectionState = ConnectionUiState(
                nodeName = "localhost:6379",
                connected = true,
            ),
            darkMode = darkMode,
            onDarkModeChange = { darkMode = it },
            modifier = modifier.fillMaxSize(),
        )
    }
}
