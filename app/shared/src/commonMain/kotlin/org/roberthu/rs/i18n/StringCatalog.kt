package org.roberthu.rs.i18n

import org.roberthu.rs.domain.AppLanguage

/**
 * Runtime source of truth for translated UI strings. The `composeResources/values/strings.xml`
 * resource bundle is not read for runtime language switching — it only exists as a static
 * fallback/preview resource. All language-aware UI code should call [t] (or the `t()` Composable
 * in `AppI18n.kt`) instead of branching on [AppLanguage] directly.
 */
object StringCatalog {
    /**
     * Controls whether [missingKey] prints a warning. Tests can flip this to keep output clean
     * while still exercising the missing-key fallback path.
     */
    var warnOnMissingKey: Boolean = true

    /**
     * Resolves [key] for [language], falling back to [AppLanguage.EnUS] and finally to the raw
     * key itself. Never throws — a missing key degrades to the key string plus a debug warning.
     */
    fun t(language: AppLanguage, key: String, vararg args: Any): String {
        val template = lookup(language, key)
            ?: lookup(AppLanguage.EnUS, key)
            ?: key.also { missingKey(key) }
        return if (args.isEmpty()) template else format(template, args)
    }

    /** True if [key] is defined in either language catalog. */
    fun hasKey(key: String): Boolean =
        StringsEnUs.strings.containsKey(key) || StringsZhCn.strings.containsKey(key)

    private fun lookup(language: AppLanguage, key: String): String? = when (language) {
        AppLanguage.ZhCN -> StringsZhCn.strings[key]
        AppLanguage.EnUS -> StringsEnUs.strings[key]
    }

    /**
     * Minimal `java.util.Formatter`-compatible subset: positional (`%1$s`, `%2$d`) and sequential
     * (`%s`, `%d`) specifiers, plus the `%%` literal escape. Implemented by hand (rather than
     * `java.util.Formatter`/`String.format`) so the catalog stays usable from Kotlin/Multiplatform
     * `commonMain` without a JVM-only dependency.
     */
    private fun format(template: String, args: Array<out Any>): String {
        var autoIndex = 0
        return FORMAT_SPECIFIER.replace(template) { match ->
            val conversion = match.groupValues[2]
            if (conversion == "%") {
                "%"
            } else {
                val explicitPosition = match.groupValues[1]
                val argIndex = if (explicitPosition.isNotEmpty()) {
                    explicitPosition.toInt() - 1
                } else {
                    autoIndex++
                }
                val arg = args.getOrNull(argIndex)
                renderArg(arg, conversion)
            }
        }
    }

    private fun renderArg(arg: Any?, conversion: String): String = when (conversion.lowercase()) {
        "d" -> when (arg) {
            is Long, is Int, is Short, is Byte -> arg.toString()
            is Number -> arg.toLong().toString()
            null -> "0"
            else -> arg.toString()
        }
        "f" -> when (arg) {
            is Number -> arg.toDouble().toString()
            null -> "0.0"
            else -> arg.toString()
        }
        else -> arg?.toString() ?: "null"
    }

    private fun missingKey(key: String) {
        if (warnOnMissingKey) {
            println("[StringCatalog] Missing translation for key: $key")
        }
    }

    /** Group 1: optional `N$` position (digits only). Group 2: conversion character. */
    private val FORMAT_SPECIFIER = Regex("""%(?:(\d+)\$)?[-+#0, ]*\d*(?:\.\d+)?([a-zA-Z%])""")
}
