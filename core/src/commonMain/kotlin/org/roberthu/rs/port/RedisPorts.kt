package org.roberthu.rs.port

import kotlinx.coroutines.flow.StateFlow
import org.roberthu.rs.domain.BinarySafeString
import org.roberthu.rs.domain.ConnectionProfile
import org.roberthu.rs.domain.HashScanPage
import org.roberthu.rs.domain.KeyMetadata
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.domain.SetScanPage
import org.roberthu.rs.domain.ZSetScanPage

interface RedisConnectionPort {
    suspend fun test(profile: ConnectionProfile): Result<Unit>
    suspend fun connect(profile: ConnectionProfile): Result<Unit>
    suspend fun disconnect()
    fun connectionState(): StateFlow<ConnectionState>
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val profileId: String, val displayName: String) : ConnectionState()
    data class Reconnecting(val attempt: Int) : ConnectionState()
    data class Failed(val error: RedisError) : ConnectionState()
}

interface KeyBrowserPort {
    suspend fun scan(query: ScanQuery): Result<ScanPage>
    fun cancelScan()
}

interface KeyCommandPort {
    suspend fun metadata(key: String): Result<KeyMetadata>
    suspend fun rename(from: String, to: String): Result<Unit>
    suspend fun delete(keys: List<String>): Result<Long>
    suspend fun setTtl(key: String, ttlSeconds: Long?): Result<Unit>
}

interface RedisDataPort {
    suspend fun getString(key: String, maxBytes: Int): Result<BinarySafeString>
    suspend fun setString(key: String, value: String): Result<Unit>

    suspend fun hscan(key: String, cursor: String?, count: Int): Result<HashScanPage>
    suspend fun hset(key: String, field: String, value: String): Result<Unit>
    suspend fun hdel(key: String, fields: List<String>): Result<Long>

    suspend fun lrange(key: String, start: Long, stop: Long): Result<List<BinarySafeString>>
    suspend fun lpush(key: String, values: List<String>): Result<Long>
    suspend fun rpush(key: String, values: List<String>): Result<Long>
    suspend fun lset(key: String, index: Long, value: String): Result<Unit>
    suspend fun lrem(key: String, count: Long, value: String): Result<Long>

    suspend fun sscan(key: String, cursor: String?, count: Int): Result<SetScanPage>
    suspend fun sadd(key: String, members: List<String>): Result<Long>
    suspend fun srem(key: String, members: List<String>): Result<Long>

    suspend fun zscan(key: String, cursor: String?, count: Int): Result<ZSetScanPage>
    suspend fun zadd(key: String, score: Double, member: String): Result<Long>
    suspend fun zrem(key: String, members: List<String>): Result<Long>
}
