package org.roberthu.rs.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
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
}
