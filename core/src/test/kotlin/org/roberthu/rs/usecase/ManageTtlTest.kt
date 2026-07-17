package org.roberthu.rs.usecase

import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.KeyMetadata
import org.roberthu.rs.port.KeyCommandPort
import kotlin.test.Test
import kotlin.test.assertEquals

class ManageTtlTest {
    @Test
    fun delegatesExpireAndPersistToKeyCommandPort() = runTest {
        val port = RecordingKeyCommandPort()
        val useCase = ManageTtl(port)

        useCase.expire("session", 60).getOrThrow()
        useCase.persist("session").getOrThrow()

        assertEquals(listOf("session" to 60L, "session" to null), port.ttlCalls)
    }

    private class RecordingKeyCommandPort : KeyCommandPort {
        val ttlCalls = mutableListOf<Pair<String, Long?>>()

        override suspend fun metadata(key: String): Result<KeyMetadata> =
            error("not used")

        override suspend fun rename(from: String, to: String): Result<Unit> =
            error("not used")

        override suspend fun delete(keys: List<String>): Result<Long> =
            error("not used")

        override suspend fun setTtl(key: String, ttlSeconds: Long?): Result<Unit> {
            ttlCalls += key to ttlSeconds
            return Result.success(Unit)
        }

        override suspend fun exists(key: String, database: Int) = Result.success(false)

        override suspend fun createKey(request: org.roberthu.rs.domain.CreateRedisKeyRequest) =
            Result.success(Unit)

        override suspend fun isRedisJsonAvailable() = Result.success(false)
    }
}
