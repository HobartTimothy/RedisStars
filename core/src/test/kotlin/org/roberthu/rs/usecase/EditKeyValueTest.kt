package org.roberthu.rs.usecase

import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.BinarySafeString
import org.roberthu.rs.domain.HashScanPage
import org.roberthu.rs.domain.SetScanPage
import org.roberthu.rs.domain.ZSetScanPage
import org.roberthu.rs.port.RedisDataPort
import kotlin.test.Test
import kotlin.test.assertEquals

class EditKeyValueTest {
    @Test
    fun delegatesValueEditsToDataPort() = runTest {
        val port = RecordingDataPort()
        val useCase = EditKeyValue(port)

        useCase.setString("string", "value").getOrThrow()
        useCase.hset("hash", "field", "value").getOrThrow()
        useCase.hdel("hash", listOf("old")).getOrThrow()
        useCase.lpush("list", listOf("first")).getOrThrow()
        useCase.rpush("list", listOf("last")).getOrThrow()
        useCase.lset("list", 1, "middle").getOrThrow()
        useCase.lrem("list", 0, "obsolete").getOrThrow()
        useCase.sadd("set", listOf("member")).getOrThrow()
        useCase.srem("set", listOf("old")).getOrThrow()
        useCase.zadd("zset", 1.5, "member").getOrThrow()
        useCase.zrem("zset", listOf("old")).getOrThrow()

        assertEquals(
            listOf(
                "setString:string:value",
                "hset:hash:field:value",
                "hdel:hash:old",
                "lpush:list:first",
                "rpush:list:last",
                "lset:list:1:middle",
                "lrem:list:0:obsolete",
                "sadd:set:member",
                "srem:set:old",
                "zadd:zset:1.5:member",
                "zrem:zset:old",
            ),
            port.calls,
        )
    }

    private class RecordingDataPort : RedisDataPort {
        val calls = mutableListOf<String>()

        override suspend fun getString(key: String, maxBytes: Int) =
            Result.success(BinarySafeString("", "", false))

        override suspend fun setString(key: String, value: String): Result<Unit> =
            success("setString:$key:$value")

        override suspend fun hscan(key: String, cursor: String?, count: Int) =
            Result.success(HashScanPage(emptyList(), null))

        override suspend fun hset(key: String, field: String, value: String): Result<Unit> =
            success("hset:$key:$field:$value")

        override suspend fun hdel(key: String, fields: List<String>): Result<Long> =
            success("hdel:$key:${fields.joinToString()}", fields.size.toLong())

        override suspend fun lrange(key: String, start: Long, stop: Long) =
            Result.success(emptyList<BinarySafeString>())

        override suspend fun lpush(key: String, values: List<String>): Result<Long> =
            success("lpush:$key:${values.joinToString()}", values.size.toLong())

        override suspend fun rpush(key: String, values: List<String>): Result<Long> =
            success("rpush:$key:${values.joinToString()}", values.size.toLong())

        override suspend fun lset(key: String, index: Long, value: String): Result<Unit> =
            success("lset:$key:$index:$value")

        override suspend fun lrem(key: String, count: Long, value: String): Result<Long> =
            success("lrem:$key:$count:$value", 1)

        override suspend fun sscan(key: String, cursor: String?, count: Int) =
            Result.success(SetScanPage(emptyList(), null))

        override suspend fun sadd(key: String, members: List<String>): Result<Long> =
            success("sadd:$key:${members.joinToString()}", members.size.toLong())

        override suspend fun srem(key: String, members: List<String>): Result<Long> =
            success("srem:$key:${members.joinToString()}", members.size.toLong())

        override suspend fun zscan(key: String, cursor: String?, count: Int) =
            Result.success(ZSetScanPage(emptyList(), null))

        override suspend fun zadd(key: String, score: Double, member: String): Result<Long> =
            success("zadd:$key:$score:$member", 1)

        override suspend fun zrem(key: String, members: List<String>): Result<Long> =
            success("zrem:$key:${members.joinToString()}", members.size.toLong())

        private fun success(call: String): Result<Unit> {
            calls += call
            return Result.success(Unit)
        }

        private fun <T> success(call: String, value: T): Result<T> {
            calls += call
            return Result.success(value)
        }
    }
}
