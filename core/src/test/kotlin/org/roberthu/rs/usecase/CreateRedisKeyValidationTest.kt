package org.roberthu.rs.usecase

import org.roberthu.rs.domain.CreateRedisKeyRequest
import org.roberthu.rs.domain.DeploymentMode
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.roberthu.rs.domain.RedisKeyPayload

class CreateRedisKeyValidationTest {
    private val useCase = CreateRedisKey(FakeKeyCommandPort())

    @Test
    fun rejectsBlankKey() {
        val result = useCase.validate(stringRequest(key = "  "))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("键名"))
    }

    @Test
    fun acceptsTtlMinusOneAndPositive() {
        assertTrue(useCase.validate(stringRequest(ttl = -1)).isSuccess)
        assertTrue(useCase.validate(stringRequest(ttl = 60)).isSuccess)
        assertTrue(useCase.validate(stringRequest(ttl = null)).isSuccess)
    }

    @Test
    fun rejectsTtlZeroAndLessThanMinusOne() {
        assertTrue(useCase.validate(stringRequest(ttl = 0)).isFailure)
        assertTrue(useCase.validate(stringRequest(ttl = -2)).isFailure)
    }

    @Test
    fun rejectsDuplicateHashFields() {
        val result = useCase.validate(
            CreateRedisKeyRequest(
                database = 0,
                key = "h",
                type = RedisKeyType.Hash,
                ttlSeconds = null,
                payload = RedisKeyPayload.HashPayload(listOf("a" to "1", "a" to "2")),
            ),
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("重复"))
    }

    @Test
    fun rejectsDuplicateSetMembers() {
        val result = useCase.validate(
            CreateRedisKeyRequest(
                database = 0,
                key = "s",
                type = RedisKeyType.Set,
                ttlSeconds = null,
                payload = RedisKeyPayload.SetPayload(listOf("a", "a")),
            ),
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsInvalidJsonShape() {
        val result = useCase.validate(
            CreateRedisKeyRequest(
                database = 0,
                key = "j",
                type = RedisKeyType.Json,
                ttlSeconds = null,
                payload = RedisKeyPayload.JsonPayload("not-json"),
            ),
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsPayloadTypeMismatch() {
        val result = useCase.validate(
            CreateRedisKeyRequest(
                database = 0,
                key = "x",
                type = RedisKeyType.String,
                ttlSeconds = null,
                payload = RedisKeyPayload.ListPayload(listOf("a")),
            ),
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun clusterOnlyAllowsDb0() {
        val result = useCase.validate(stringRequest(database = 1), DeploymentMode.Cluster)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("db0"))
    }

    private fun stringRequest(
        key: String = "k",
        database: Int = 0,
        ttl: Long? = null,
    ) = CreateRedisKeyRequest(
        database = database,
        key = key,
        type = RedisKeyType.String,
        ttlSeconds = ttl,
        payload = RedisKeyPayload.StringPayload("v"),
    )

    private class FakeKeyCommandPort : org.roberthu.rs.port.KeyCommandPort {
        override suspend fun metadata(key: String) = error("unused")
        override suspend fun rename(from: String, to: String) = error("unused")
        override suspend fun delete(keys: List<String>) = error("unused")
        override suspend fun setTtl(key: String, ttlSeconds: Long?) = error("unused")
        override suspend fun exists(key: String, database: Int) = Result.success(false)
        override suspend fun createKey(request: CreateRedisKeyRequest) = Result.success(Unit)
        override suspend fun isRedisJsonAvailable() = Result.success(false)
    }
}
