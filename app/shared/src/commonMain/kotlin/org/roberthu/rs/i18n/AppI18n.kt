package org.roberthu.rs.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.concurrent.Volatile
import org.roberthu.rs.domain.AppLanguage

/**
 * Process-wide current UI language, kept outside Compose so non-Composable code (ViewModels,
 * validators, use cases) can also call [AppI18n.t] without needing a `@Composable` context.
 */
object AppI18n {
    @Volatile
    var language: AppLanguage = AppLanguage.EnUS
        private set

    /** Updates the active language and applies platform-level locale settings (e.g. JVM default Locale). */
    fun update(language: AppLanguage) {
        this.language = language
        applyPlatformLocale(language)
    }

    fun t(key: String, vararg args: Any): String = StringCatalog.t(language, key, *args)
}

/** Composition-local mirror of [AppI18n.language] so Compose UI recomposes on language change. */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.EnUS }

/**
 * Installs [language] as both [AppI18n.language] and [LocalAppLanguage] for [content]. Wrapping
 * `content` in `key(language)` forces a full recomposition on language switch, so screens built
 * from remembered/derived state (e.g. `remember { ... }`) still pick up the new strings.
 */
@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    AppI18n.update(language)
    CompositionLocalProvider(LocalAppLanguage provides language) {
        key(language) {
            content()
        }
    }
}

/** Composable string lookup — prefer this over [AppI18n.t] inside `@Composable` UI code. */
@Composable
fun t(key: String, vararg args: Any): String {
    val language = LocalAppLanguage.current
    return StringCatalog.t(language, key, *args)
}

/** Platform hook for locale-dependent JVM/OS APIs (number/date formatting, etc.). */
expect fun applyPlatformLocale(language: AppLanguage)
