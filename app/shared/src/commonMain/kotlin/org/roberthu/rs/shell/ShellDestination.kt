package org.roberthu.rs.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.ui.graphics.vector.ImageVector

enum class ShellDestination(
    val label: String,
    val icon: ImageVector,
) {
    Connections(label = "连接管理", icon = Icons.Filled.Cable),
    Monitor(label = "实时监控", icon = Icons.Filled.Speed),
    SlowLog(label = "慢日志", icon = Icons.Filled.Timelapse),
    Settings(label = "设置", icon = Icons.Default.Settings),
    ;

    companion object {
        /** Primary destinations shown at the top of the rail / start of the bar. */
        val primaryDestinations: List<ShellDestination> =
            listOf(Connections, Monitor, SlowLog)
    }
}
