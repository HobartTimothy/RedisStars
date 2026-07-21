package org.roberthu.rs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import org.roberthu.rs.shell.icons.AppIcons
import org.roberthu.rs.ui.theme.RedisTheme

@Composable
fun RedisEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    testTag: String = "empty_state",
    icon: @Composable (() -> Unit)? = null,
) {
    val colors = RedisTheme.colors
    val spacing = RedisTheme.spacing
    val dim = RedisTheme.dimensions
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.xl)
            .testTag(testTag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            icon()
        } else {
            Icon(
                imageVector = AppIcons.Folder,
                contentDescription = null,
                modifier = Modifier.size(dim.emptyStateIconSize),
                tint = colors.iconSecondary.copy(alpha = 0.45f),
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm),
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.xs),
            )
        }
    }
}

@Composable
fun RedisLoadingState(
    modifier: Modifier = Modifier,
    label: String? = null,
    testTag: String = "loading_state",
) {
    val colors = RedisTheme.colors
    val dim = RedisTheme.dimensions
    Box(
        modifier = modifier.fillMaxSize().testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RedisTheme.spacing.sm),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(dim.iconSizeLg))
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
fun RedisErrorState(
    message: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    testTag: String = "error_state",
) {
    val colors = RedisTheme.colors
    val spacing = RedisTheme.spacing
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier.fillMaxWidth().testTag(testTag),
    ) {
        Column(Modifier.padding(spacing.sm)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = spacing.xs),
                )
            }
        }
    }
}

@Composable
fun RedisWarningBanner(
    message: String,
    modifier: Modifier = Modifier,
    testTag: String = "warning_banner",
) {
    val colors = RedisTheme.colors
    val spacing = RedisTheme.spacing
    val shapes = RedisTheme.shapes
    Surface(
        color = colors.warning.copy(alpha = 0.12f),
        shape = shapes.small,
        modifier = modifier.fillMaxWidth().testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier.size(RedisTheme.dimensions.iconSizeSm),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textPrimary,
            )
        }
    }
}

@Composable
fun RedisNotConnectedState(
    message: String,
    modifier: Modifier = Modifier,
    testTag: String = "not_connected_state",
) {
    val colors = RedisTheme.colors
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
        modifier = modifier.testTag(testTag),
    )
}
