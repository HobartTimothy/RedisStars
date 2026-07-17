package org.roberthu.rs.presentation

/**
 * Immutable build-time metadata provided by the desktop composition root.
 *
 * The `app/shared` module must not hard-code version strings or license text.
 * The actual values are supplied by `DesktopCompositionRoot` so they stay
 * consistent with the Gradle build configuration.
 *
 * Note: the project is licensed under the MIT License.
 */
data class BuildInfo(
    val version: String,
    val licenseName: String,
)
