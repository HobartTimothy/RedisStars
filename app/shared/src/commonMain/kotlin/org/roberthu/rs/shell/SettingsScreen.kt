package org.roberthu.rs.shell

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    autoConnect: Boolean,
    onAutoConnectChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    appVersion: String = "1.0.0-beta",
    licenseName: String = "Apache License 2.0",
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        item(key = "section-software") {
            Text(
                text = "软件设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item(key = "pref-dark-mode") {
            ListItem(
                headlineContent = { Text("深色模式") },
                supportingContent = { Text("Dark Mode") },
                trailingContent = {
                    Switch(
                        checked = darkMode,
                        onCheckedChange = onDarkModeChange,
                    )
                },
            )
        }
        item(key = "pref-auto-connect") {
            ListItem(
                headlineContent = { Text("启动时自动连接") },
                supportingContent = { Text("Auto-connect on startup") },
                trailingContent = {
                    Switch(
                        checked = autoConnect,
                        onCheckedChange = onAutoConnectChange,
                    )
                },
            )
        }
        item(key = "divider-about") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }
        item(key = "section-about") {
            Text(
                text = "关于",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item(key = "about-version") {
            ListItem(
                headlineContent = { Text("版本") },
                supportingContent = { Text(appVersion) },
            )
        }
        item(key = "about-license") {
            ListItem(
                headlineContent = { Text("开源协议") },
                supportingContent = { Text(licenseName) },
            )
        }
        item(key = "about-product") {
            ListItem(
                headlineContent = { Text("产品") },
                supportingContent = { Text("RedisStars — Compose Multiplatform Redis 客户端") },
            )
        }
    }
}
