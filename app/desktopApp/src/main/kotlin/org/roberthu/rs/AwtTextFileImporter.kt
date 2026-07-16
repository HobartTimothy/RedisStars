package org.roberthu.rs

import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path
import kotlin.io.path.readText
import org.roberthu.rs.platform.TextFileImporter

fun awtTextFileImporter(): TextFileImporter = TextFileImporter {

    val dialog = FileDialog(null as Frame?, "导入数据", FileDialog.LOAD)

    dialog.isMultipleMode = false
    dialog.filenameFilter = FilenameFilter { _, name ->
        name.endsWith(".txt", ignoreCase = true) ||
            name.endsWith(".json", ignoreCase = true) ||
            name.endsWith(".csv", ignoreCase = true) ||
            !name.contains('.')
    }

    dialog.isVisible = true
    val directory = dialog.directory ?: return@TextFileImporter null
    val file = dialog.file ?: return@TextFileImporter null

    runCatching {
        Path(directory, file).readText(StandardCharsets.UTF_8)
    }.getOrNull()
}
