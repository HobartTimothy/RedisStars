package org.roberthu.rs.platform

/**
 * Desktop file save dialog bridge. Implementations live in `app/desktopApp`.
 */
fun interface FileSavePicker {
    /**
     * Opens a platform save dialog and returns the absolute path, or null if cancelled.
     */
    fun pickSavePath(title: String, defaultFileName: String): String?
}
