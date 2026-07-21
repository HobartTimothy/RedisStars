package org.roberthu.rs.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Desktop-compact typography. Access body styles via [MaterialTheme.typography];
 * data-specific styles via [RedisTheme.typography].
 */
@Immutable
data class RedisTypographyTokens(
    val paneTitle: TextStyle,
    val sectionTitle: TextStyle,
    val data: TextStyle,
    val log: TextStyle,
    val keyName: TextStyle,
)

fun createRedisMaterialTypography(): Typography {
    val default = Typography()
    return Typography(
        titleLarge = default.titleLarge.copy(
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = default.titleMedium.copy(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleSmall = default.titleSmall.copy(
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        ),
        bodyLarge = default.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodyMedium = default.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = default.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = default.labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = default.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = default.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
    )
}

fun createRedisTypographyTokens(): RedisTypographyTokens {
    val mono = FontFamily.Monospace
    return RedisTypographyTokens(
        paneTitle = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        sectionTitle = TextStyle(
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        ),
        data = TextStyle(
            fontFamily = mono,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        log = TextStyle(
            fontFamily = mono,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        ),
        keyName = TextStyle(
            fontFamily = mono,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
    )
}

internal val LocalRedisTypography = staticCompositionLocalOf { createRedisTypographyTokens() }
