package org.roberthu.rs.domain

enum class RedisKeyType {
    String,
    Hash,
    List,
    Set,
    ZSet,
    Stream,
    Other,
    Unknown,
}

data class RedisKeySummary(
    val key: String,
    val type: RedisKeyType,
)

data class ScanPage(
    val keys: List<RedisKeySummary>,
    val nextCursorToken: String?,
    val partialFailures: List<String> = emptyList(),
)

data class ScanQuery(
    val database: Int = 0,
    val pattern: String = "*",
    val type: RedisKeyType? = null,
    val cursorToken: String? = null,
    val countHint: Int = 100,
)

data class KeyMetadata(
    val key: String,
    val type: RedisKeyType,
    val ttlSeconds: Long?,
    val memoryBytes: Long?,
    val encoding: String?,
)
