package org.roberthu.rs.platform

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual fun runtimeLogExportFileName(): String {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
    return "redisstars-runtime-logs-$timestamp.log"
}
