package org.roberthu.rs.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t

@Composable
fun MonitorPane(modifier: Modifier = Modifier) {
    PlaceholderDestination(
        title = t(StringKeys.Monitor.Title),
        subtitle = t(StringKeys.Monitor.Placeholder),
        modifier = modifier,
    )
}

@Composable
fun SlowLogPane(modifier: Modifier = Modifier) {
    PlaceholderDestination(
        title = t(StringKeys.SlowLog.Title),
        subtitle = t(StringKeys.SlowLog.Placeholder),
        modifier = modifier,
    )
}

@Composable
private fun PlaceholderDestination(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "$title\n$subtitle",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}
