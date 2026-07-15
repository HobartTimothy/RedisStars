package org.roberthu.rs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    val root = DesktopCompositionRoot()
    application {
        Window(
            onCloseRequest = {
                root.close()
                exitApplication()
            },
            title = "RedisStars",
            state = rememberWindowState(width = 1280.dp, height = 800.dp),
        ) {
            App(container = root.container, modifier = Modifier.fillMaxSize())
        }
    }
}