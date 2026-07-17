package org.roberthu.rs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Full-width error banner shown below the top bar.
 *
 * Designed for application-level errors (e.g. settings save failure) that should
 * be visible but dismissible. Use [RedisInlineSnackbar] for pane-level errors.
 *
 * @param message The error text to display. Pass `null` to hide the banner.
 * @param onDismiss Called when the user taps the dismiss action.
 * @param dismissLabel Localized text for the dismiss action button.
 */
@Composable
fun RedisErrorBanner(
    message: String?,
    onDismiss: () -> Unit,
    dismissLabel: String,
    modifier: Modifier = Modifier,
) {
    if (message == null) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("error_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text(dismissLabel)
        }
    }
}

/**
 * Snackbar host pre-styled for connection-pane error messages.
 *
 * Wraps [SnackbarHost] with consistent error container colours so every pane
 * does not need to repeat the same colour override.
 */
@Composable
fun RedisInlineSnackbar(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
