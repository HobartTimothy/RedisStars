package org.roberthu.rs.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.i18n.t

enum class ShellDestination(
    val icon: ImageVector,
) {
    Connections(icon = Icons.Filled.Cable),
    Monitor(icon = Icons.Filled.Speed),
    RuntimeLogs(icon = Icons.Filled.Article),
    Settings(icon = Icons.Default.Settings),
    ;

    companion object {
        /** Primary destinations shown at the top of the rail / start of the bar. */
        val primaryDestinations: List<ShellDestination> =
            listOf(Connections, Monitor, RuntimeLogs)
    }
}

@Composable
fun ShellDestination.label(): String = when (this) {
    ShellDestination.Connections -> t(StringKeys.Nav.Connections)
    ShellDestination.Monitor -> t(StringKeys.Nav.Monitor)
    ShellDestination.RuntimeLogs -> t(StringKeys.Nav.RuntimeLogs)
    ShellDestination.Settings -> t(StringKeys.Nav.Settings)
}
