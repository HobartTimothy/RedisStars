package org.roberthu.rs.redis

import io.lettuce.core.RedisCommandExecutionException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanQuery
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LettuceScanExecutorTest {
    @Test
    fun retriesWithoutServerTypeAndFiltersOnlyReturnedPage() = runBlocking {
        val commands = RecordingScanCommands(
            pages = ArrayDeque(
                listOf(
                    Result.failure(
                        RedisCommandExecutionException("ERR syntax error"),
                    ),
                    Result.success(ScanCommandPage(listOf("string:key", "hash:key"), "19", false)),
                ),
            ),
            types = mapOf(
                "string:key" to "string",
                "hash:key" to "hash",
            ),
        )

        val page = scanPage(
            commands,
            ScanQuery(type = RedisKeyType.Hash, countHint = 25),
        )

        assertEquals(2, commands.scanCalls.size)
        assertEquals("hash", commands.scanCalls.first().type)
        assertEquals(null, commands.scanCalls.last().type)
        assertEquals(25L, commands.scanCalls.last().count)
        assertEquals(listOf("string:key", "hash:key"), commands.typeCalls)
        assertEquals(listOf("hash:key"), page.keys.map { it.key })
        assertEquals("19", page.nextCursorToken)
    }

    @Test
    fun usesCountHintAndScanOnly() = runBlocking {
        val commands = RecordingScanCommands(
            pages = ArrayDeque(
                listOf(Result.success(ScanCommandPage(emptyList(), "0", true))),
            ),
        )

        scanPage(commands, ScanQuery(pattern = "user:*", countHint = 37))

        assertEquals(1, commands.scanCalls.size)
        assertEquals(37L, commands.scanCalls.single().count)
        assertTrue(commands.commandNames.all { it == "scan" || it == "type" })
    }

    @Test
    fun resolveKeyTypesPreservesOrderAndReportsPartialFailures() = runBlocking {
        val commands = RecordingScanCommands(
            pages = ArrayDeque(),
            types = mapOf(
                "key-a" to "string",
                "key-c" to "hash",
            ),
            failingKeys = setOf("key-b"),
        )

        val resolved = resolveKeyTypes(
            commands,
            listOf("key-a", "key-b", "key-c"),
            concurrency = 2,
        )

        assertEquals(listOf("key-a", "key-b", "key-c"), resolved.map { it.first })
        assertEquals("string", resolved[0].second.getOrThrow())
        assertTrue(resolved[1].second.isFailure)
        assertEquals("hash", resolved[2].second.getOrThrow())
    }

    @Test
    fun resolveKeyTypesUsesBoundedConcurrency() = runBlocking {
        val commands = ConcurrentRecordingScanCommands(
            keys = listOf("k1", "k2", "k3", "k4", "k5"),
            typeDelayMs = 20,
        )

        val resolved = resolveKeyTypes(commands, commands.keys, concurrency = 2)

        assertEquals(commands.keys, resolved.map { it.first })
        assertTrue(commands.maxInFlight <= 2)
    }

    private class RecordingScanCommands(
        private val pages: ArrayDeque<Result<ScanCommandPage>>,
        private val types: Map<String, String> = emptyMap(),
        private val failingKeys: Set<String> = emptySet(),
    ) : ScanCommands {
        val scanCalls = mutableListOf<ScanCommandRequest>()
        val typeCalls = mutableListOf<String>()
        val commandNames = mutableListOf<String>()

        override fun scan(request: ScanCommandRequest): ScanCommandPage {
            commandNames += "scan"
            scanCalls += request
            return pages.removeFirst().getOrThrow()
        }

        override fun type(key: String): String {
            commandNames += "type"
            typeCalls += key
            if (key in failingKeys) {
                throw RedisCommandExecutionException("ERR type failed for $key")
            }
            return types.getValue(key)
        }
    }

    private class ConcurrentRecordingScanCommands(
        val keys: List<String>,
        private val typeDelayMs: Long,
    ) : ScanCommands {
        var maxInFlight = 0
        private var inFlight = 0

        override fun scan(request: ScanCommandRequest): ScanCommandPage =
            ScanCommandPage(emptyList(), "0", true)

        override fun type(key: String): String {
            synchronized(this) {
                inFlight += 1
                maxInFlight = maxOf(maxInFlight, inFlight)
            }
            try {
                Thread.sleep(typeDelayMs)
                return "string"
            } finally {
                synchronized(this) {
                    inFlight -= 1
                }
            }
        }
    }
}
