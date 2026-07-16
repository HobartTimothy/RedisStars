package org.roberthu.rs

import java.awt.FileDialog
import java.awt.Frame
import java.io.FilenameFilter
import java.nio.file.Path
import org.roberthu.rs.i18n.AppI18n
import org.roberthu.rs.i18n.StringKeys
import org.roberthu.rs.platform.FilePathPicker

fun awtSshPrivateKeyPathPicker(): FilePathPicker = FilePathPicker { title ->
    val dialog = FileDialog(
        null as Frame?,
        title.ifBlank { AppI18n.t(StringKeys.ConnectionEditor.SshPickPrivateKeyTitle) },
        FileDialog.LOAD,
    )

    dialog.isMultipleMode = false
    dialog.filenameFilter = FilenameFilter { _, name ->
        name.endsWith(".pem", ignoreCase = true) ||
            name.endsWith(".key", ignoreCase = true) ||
            !name.contains('.') ||
            name.startsWith("id_", ignoreCase = true)
    }

    dialog.isVisible = true

    val directory = dialog.directory ?: return@FilePathPicker null
    val file = dialog.file ?: return@FilePathPicker null

    Path.of(directory, file).toAbsolutePath().normalize().toString()
}