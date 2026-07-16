package org.roberthu.rs.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.roberthu.rs.domain.ApplicationLogLevel

@Composable
fun logLevelColor(level: ApplicationLogLevel): Color = when (level) {
    ApplicationLogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
    ApplicationLogLevel.INFO -> MaterialTheme.colorScheme.primary
    ApplicationLogLevel.WARN -> MaterialTheme.colorScheme.tertiary
    ApplicationLogLevel.ERROR -> MaterialTheme.colorScheme.error
}
