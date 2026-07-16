package org.roberthu.rs.platform

import org.roberthu.rs.domain.AppLanguage

/** Best-effort system language tag (e.g. `zh-CN`, `en-US`). Never throws. */
expect fun systemLanguageTag(): String

fun resolveInitialAppLanguage(storedTag: String?): AppLanguage =
    AppLanguage.resolve(storedTag = storedTag, systemLanguage = systemLanguageTag())
