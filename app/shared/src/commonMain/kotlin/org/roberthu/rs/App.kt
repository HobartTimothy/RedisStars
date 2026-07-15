package org.roberthu.rs

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.roberthu.rs.shell.ConnectionUiState
import org.roberthu.rs.shell.RedisAppShell
import org.roberthu.rs.theme.RedisTheme

@Composable
@Preview
fun App() {
    RedisTheme {
        RedisAppShell(
            connectionState = ConnectionUiState(
                nodeName = "localhost:6379",
                connected = true,
            ),
        )
    }
}