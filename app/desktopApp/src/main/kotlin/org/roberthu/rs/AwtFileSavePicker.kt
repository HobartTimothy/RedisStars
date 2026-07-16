package org.roberthu.rs

import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import org.roberthu.rs.platform.FileSavePicker

fun awtRuntimeLogSavePicker(): FileSavePicker = FileSavePicker { title, defaultFileName ->
    val dialog = FileDialog(
        null as Frame?,
        title,
        FileDialog.SAVE,
    )
    dialog.file = defaultFileName
    dialog.isVisible = true
    val directory = dialog.directory ?: return@FileSavePicker null
    val file = dialog.file ?: return@FileSavePicker null
    Path.of(directory, file).toAbsolutePath().normalize().toString()
}
