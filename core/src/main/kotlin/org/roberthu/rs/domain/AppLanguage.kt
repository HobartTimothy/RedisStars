package org.roberthu.rs.domain

/**
 * Application UI language. Prefer this type over raw language tags in business code.
 */
enum class AppLanguage(val tag: String) {
    ZhCN("zh-CN"),
    EnUS("en-US"),
    ;

    companion object {
        /** Accepts only canonical persisted tags (`zh-CN`, `en-US`). */
        fun fromTagOrNull(tag: String?): AppLanguage? {
            if (tag.isNullOrBlank()) return null
            val normalized = tag.trim()
            return entries.firstOrNull { it.tag.equals(normalized, ignoreCase = true) }
        }

        /**
         * System language rule: tags starting with `zh` → [ZhCN], otherwise [EnUS].
         */
        fun fromSystemLanguage(systemLanguage: String): AppLanguage {
            val normalized = systemLanguage.trim().lowercase()
            return if (normalized.startsWith("zh")) ZhCN else EnUS
        }

        /**
         * Prefer a valid stored preference; otherwise derive from the system language.
         * Invalid stored values fall back to the system rule (never crash).
         */
        fun resolve(storedTag: String?, systemLanguage: String): AppLanguage =
            fromTagOrNull(storedTag) ?: fromSystemLanguage(systemLanguage)
    }
}