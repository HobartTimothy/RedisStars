package org.roberthu.rs.i18n

import java.util.Locale
import org.roberthu.rs.domain.AppLanguage

/**
 * Keeps the JVM default [Locale] in sync with the app language so platform-level formatting
 * (numbers, dates, [java.util.Formatter], etc.) matches the selected UI language.
 */
actual fun applyPlatformLocale(language: AppLanguage) {
    Locale.setDefault(Locale.forLanguageTag(language.tag))
}
