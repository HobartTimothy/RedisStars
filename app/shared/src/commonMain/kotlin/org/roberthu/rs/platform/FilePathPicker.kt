package org.roberthu.rs.platform

/**
 * Desktop file-path picker bridge. Implementations live in `app/desktopApp`.
 * Preview/tests may return null.
 */
fun interface FilePathPicker {
    /**
     * Opens a platform file picker and returns the absolute path, or null if cancelled.
     */
    fun pickFilePath(title: String): String?
}
