package org.roberthu.rs.platform

/**
 * Desktop file import bridge. Implementations live in `app/desktopApp`.
 * Preview/tests may return null.
 */
fun interface TextFileImporter {
    /** Opens a platform file picker and returns UTF-8 text, or null if cancelled. */
    fun importTextFile(): String?
}
