package org.roberthu.rs.theme

import androidx.compose.ui.graphics.Color
import org.roberthu.rs.domain.RedisKeyType

object RedisKeyTypeColors {
    val String = Color(0xFF3B82F6)
    val Hash = Color(0xFFF97316)
    val List = Color(0xFF22C55E)
    val Set = Color(0xFFA855F7)
    val ZSet = Color(0xFF14B8A6)
    val Stream = Color(0xFFEC4899)
    val Json = Color(0xFFEAB308)
    val Other = Color(0xFF6B7280)
    val Unknown = Color(0xFF9CA3AF)
}

fun RedisKeyType.color(): Color = when (this) {
    RedisKeyType.String -> RedisKeyTypeColors.String
    RedisKeyType.Hash -> RedisKeyTypeColors.Hash
    RedisKeyType.List -> RedisKeyTypeColors.List
    RedisKeyType.Set -> RedisKeyTypeColors.Set
    RedisKeyType.ZSet -> RedisKeyTypeColors.ZSet
    RedisKeyType.Stream -> RedisKeyTypeColors.Stream
    RedisKeyType.Json -> RedisKeyTypeColors.Json
    RedisKeyType.Other -> RedisKeyTypeColors.Other
    RedisKeyType.Unknown -> RedisKeyTypeColors.Unknown
}
