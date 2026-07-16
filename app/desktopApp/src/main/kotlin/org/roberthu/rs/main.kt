package org.roberthu.rs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    val root = DesktopCompositionRoot()
    application {
        val appIcon = remember {
            runCatching {
                val stream = checkNotNull(
                    ClassLoader.getSystemClassLoader().getResourceAsStream("logo.png")
                        ?: object {}.javaClass.getResourceAsStream("/logo.png"),
                ) { "logo.png not found on classpath" }
                stream.use { BitmapPainter(loadImageBitmap(it)) }
            }.getOrNull()
        }
        Window(
            onCloseRequest = {
                root.close()
                exitApplication()
            },
            title = "RedisStars",
            icon = appIcon,
            state = rememberWindowState(width = 1280.dp, height = 800.dp),
        ) {
            App(
                container = root.container,
                modifier = Modifier.fillMaxSize(),
                textFileImporter = awtTextFileImporter(),
                sshPrivateKeyPathPicker = awtSshPrivateKeyPathPicker(),
            )
        }
    }
}
