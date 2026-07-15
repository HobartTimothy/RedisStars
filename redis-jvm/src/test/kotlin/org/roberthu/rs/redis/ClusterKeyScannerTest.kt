package org.roberthu.rs.redis

import org.junit.Test
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import org.roberthu.rs.domain.ScanPage
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClusterKeyScannerTest {
    @Test
    fun mergeDeduplicatesKeysAndTracksEachUnfinishedMasterCursor() {
        val merged = ClusterKeyScanner.merge(
            previousCursors = mapOf("master-a" to "0", "master-b" to "0"),
            results = listOf(
                success("master-a", listOf(key("shared"), key("a")), "17"),
                success("master-b", listOf(key("shared"), key("b")), "29"),
            ),
        )

        assertEquals(listOf("shared", "a", "b"), merged.keys.map { it.key })
        assertEquals(
            mapOf("master-a" to "17", "master-b" to "29"),
            ClusterKeyScanner.decodeToken(assertNotNull(merged.nextCursorToken)),
        )
        assertTrue(merged.partialFailures.isEmpty())
    }

    @Test
    fun mergeOmitsFinishedMastersAndEndsWhenAllMastersFinish() {
        val first = ClusterKeyScanner.merge(
            previousCursors = mapOf("master-a" to "4", "master-b" to "8"),
            results = listOf(
                success("master-a", listOf(key("a")), null),
                success("master-b", listOf(key("b")), "11"),
            ),
        )

        assertEquals(
            mapOf("master-b" to "11"),
            ClusterKeyScanner.decodeToken(assertNotNull(first.nextCursorToken)),
        )

        val last = ClusterKeyScanner.merge(
            previousCursors = mapOf("master-b" to "11"),
            results = listOf(success("master-b", listOf(key("c")), null)),
        )

        assertNull(last.nextCursorToken)
    }

    @Test
    fun mergeRetainsFailedMasterCursorAndReportsPartialFailure() {
        val merged = ClusterKeyScanner.merge(
            previousCursors = mapOf("master-a" to "15", "master-b" to "23"),
            results = listOf(
                success("master-a", listOf(key("a")), null),
                ClusterNodeScanResult.Failure("master-b", "node unavailable"),
            ),
        )

        assertEquals(
            mapOf("master-b" to "23"),
            ClusterKeyScanner.decodeToken(assertNotNull(merged.nextCursorToken)),
        )
        assertEquals(listOf("master-b: node unavailable"), merged.partialFailures)
    }

    @Test
    fun tokenRoundTripsNodeIdsAndCursorsAsOpaqueBase64Json() {
        val cursors = linkedMapOf("node \"one\"" to "12", "節點-2" to "99")

        val token = ClusterKeyScanner.encodeToken(cursors)

        assertEquals(cursors, ClusterKeyScanner.decodeToken(token))
        assertTrue(!token.contains("node"))
    }

    private fun success(
        nodeId: String,
        keys: List<RedisKeySummary>,
        nextCursor: String?,
    ) = ClusterNodeScanResult.Success(
        nodeId = nodeId,
        page = ScanPage(keys, nextCursor),
    )

    private fun key(name: String) = RedisKeySummary(name, RedisKeyType.String)
}
