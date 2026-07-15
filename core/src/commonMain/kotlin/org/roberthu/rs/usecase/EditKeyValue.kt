package org.roberthu.rs.usecase

import org.roberthu.rs.port.RedisDataPort

class EditKeyValue(
    private val dataPort: RedisDataPort,
) {
    suspend fun setString(key: String, value: String) = dataPort.setString(key, value)

    suspend fun hset(key: String, field: String, value: String) =
        dataPort.hset(key, field, value)

    suspend fun hdel(key: String, fields: List<String>) = dataPort.hdel(key, fields)

    suspend fun lpush(key: String, values: List<String>) = dataPort.lpush(key, values)

    suspend fun rpush(key: String, values: List<String>) = dataPort.rpush(key, values)

    suspend fun lset(key: String, index: Long, value: String) =
        dataPort.lset(key, index, value)

    suspend fun lrem(key: String, count: Long, value: String) =
        dataPort.lrem(key, count, value)

    suspend fun sadd(key: String, members: List<String>) = dataPort.sadd(key, members)

    suspend fun srem(key: String, members: List<String>) = dataPort.srem(key, members)

    suspend fun zadd(key: String, score: Double, member: String) =
        dataPort.zadd(key, score, member)

    suspend fun zrem(key: String, members: List<String>) = dataPort.zrem(key, members)
}
