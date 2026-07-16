package org.roberthu.rs.platform

import java.util.Locale

actual fun systemLanguageTag(): String =
    runCatching { Locale.getDefault().toLanguageTag() }
        .getOrDefault("en-US")
