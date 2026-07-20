package org.roberthu.rs.shell.keys

import org.roberthu.rs.domain.RedisKeyType

internal val filterableKeyTypes = listOf(
    RedisKeyType.String,
    RedisKeyType.Hash,
    RedisKeyType.List,
    RedisKeyType.Set,
    RedisKeyType.ZSet,
    RedisKeyType.Stream,
    RedisKeyType.Json,
)

internal fun RedisKeyType.filterBadgeLetter(): Char = when (this) {
    RedisKeyType.String -> 'S'
    RedisKeyType.Hash -> 'H'
    RedisKeyType.List -> 'L'
    RedisKeyType.Set -> 'E'
    RedisKeyType.ZSet -> 'Z'
    RedisKeyType.Stream -> 'X'
    RedisKeyType.Json -> 'J'
    RedisKeyType.Other,
    RedisKeyType.Unknown,
    -> '?'
}

internal const val ALL_TYPE_FILTER_BADGE_LETTER = 'A'
