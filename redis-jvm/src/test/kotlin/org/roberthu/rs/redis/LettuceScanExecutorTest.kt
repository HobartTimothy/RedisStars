package org.roberthu.rs.redis

import io.lettuce.core.RedisCommandExecutionException
import org.junit.Test
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanQuery
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LettuceScanExecutorTest {
    @Test
    fun retriesWithoutServerTypeAndFiltersOnlyReturnedPage() {
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
    fun usesCountHintAndScanOnly() {
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

    private class RecordingScanCommands(
        private val pages: ArrayDeque<Result<ScanCommandPage>>,
        private val types: Map<String, String> = emptyMap(),
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
            return types.getValue(key)
        }
    }
}
