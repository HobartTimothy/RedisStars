package org.roberthu.rs.ui.theme

/**
 * Application-level constants that are not user-configurable and not part of the
 * Material theme (e.g. brand name, product-level strings that appear in UI but
 * live outside the i18n catalog).
 *
 * Kept here so they have a single source of truth rather than being scattered
 * across Composable files.
 */
object RedisAppConstants {
    /** Brand name displayed in the sidebar header and window title. */
    const val AppName = "RedisStars"

    /** Hover layer alpha for interactive surfaces (matches Material motion spec). */
    const val HoverAlpha = 0.08f

    /** Dragging ghost alpha for drag-and-drop items. */
    const val DraggingAlpha = 0.55f

    /** Selected highlight alpha for group/drop-target highlights. */
    const val HighlightAlpha = 0.45f
}
