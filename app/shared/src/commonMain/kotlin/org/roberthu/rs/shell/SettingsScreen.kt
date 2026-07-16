package org.roberthu.rs.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.roberthu.rs.domain.AppLanguage
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    autoConnect: Boolean,
    onAutoConnectChange: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
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
                text = t(StringKeys.Settings.Title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item(key = "pref-language") {
            ListItem(
                headlineContent = { Text(t(StringKeys.Settings.Language)) },
                supportingContent = {
                    Text(t(StringKeys.Settings.LanguageDesc))
                },
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("settings_language_chips"),
                    ) {
                        FilterChip(
                            selected = language == AppLanguage.ZhCN,
                            onClick = { onLanguageChange(AppLanguage.ZhCN) },
                            label = { Text(t(StringKeys.Settings.LanguageOptionZh)) },
                            modifier = Modifier.testTag("settings_language_zh"),
                        )
                        FilterChip(
                            selected = language == AppLanguage.EnUS,
                            onClick = { onLanguageChange(AppLanguage.EnUS) },
                            label = { Text(t(StringKeys.Settings.LanguageOptionEn)) },
                            modifier = Modifier.testTag("settings_language_en"),
                        )
                    }
                },
            )
        }
        item(key = "pref-dark-mode") {
            ListItem(
                headlineContent = { Text(t(StringKeys.Settings.DarkMode)) },
                supportingContent = { Text(t(StringKeys.Settings.DarkModeDesc)) },
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
                headlineContent = { Text(t(StringKeys.Settings.AutoConnect)) },
                supportingContent = { Text(t(StringKeys.Settings.AutoConnectDesc)) },
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
                text = t(StringKeys.Settings.AboutTitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item(key = "about-version") {
            ListItem(
                headlineContent = { Text(t(StringKeys.Settings.Version)) },
                supportingContent = { Text(appVersion) },
            )
        }
        item(key = "about-license") {
            ListItem(
                headlineContent = { Text(t(StringKeys.Settings.License)) },
                supportingContent = { Text(licenseName) },
            )
        }
        item(key = "about-product") {
            ListItem(
                headlineContent = { Text(t(StringKeys.Settings.Product)) },
                supportingContent = { Text(t(StringKeys.Settings.ProductDesc)) },
            )
        }
    }
}
