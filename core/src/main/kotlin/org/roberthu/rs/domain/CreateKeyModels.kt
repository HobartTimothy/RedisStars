package org.roberthu.rs.domain

data class RedisDatabaseSummary(
    val index: Int,
    val keyCount: Long,
    val selectable: Boolean = true,
)

/**
 * Payload for creating a new Redis key. Prefer sealed variants over a single nullable bag.
 */
sealed interface RedisKeyPayload {
    data class StringPayload(val value: String) : RedisKeyPayload

    data class HashPayload(val fields: List<Pair<String, String>>) : RedisKeyPayload

    data class ListPayload(val values: List<String>) : RedisKeyPayload

    data class SetPayload(val members: List<String>) : RedisKeyPayload

    data class ZSetPayload(val entries: List<Pair<Double, String>>) : RedisKeyPayload

    data class StreamPayload(
        val id: String = "*",
        val fields: List<Pair<String, String>>,
    ) : RedisKeyPayload

    data class JsonPayload(val json: String) : RedisKeyPayload
}

data class CreateRedisKeyRequest(
    val database: Int,
    val key: String,
    val type: RedisKeyType,
    /** `null` or `-1` means persist (no expiry). Positive = EX seconds. */
    val ttlSeconds: Long?,
    val payload: RedisKeyPayload,
)
