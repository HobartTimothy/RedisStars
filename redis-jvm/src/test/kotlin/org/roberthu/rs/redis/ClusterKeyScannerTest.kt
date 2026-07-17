package org.roberthu.rs.redis

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
    fun mergeDropsTopologyRemovedMasterCursor() {
        val merged = ClusterKeyScanner.merge(
            previousCursors = mapOf("master-a" to "15", "removed-master" to "23"),
            results = listOf(
                success("master-a", listOf(key("a")), null),
                ClusterNodeScanResult.Failure(
                    "removed-master",
                    ClusterKeyScanner.TOPOLOGY_REMOVED_REASON,
                ),
            ),
        )

        assertNull(merged.nextCursorToken)
        assertEquals(
            listOf("removed-master: ${ClusterKeyScanner.TOPOLOGY_REMOVED_REASON}"),
            merged.partialFailures,
        )
    }

    @Test
    fun mergeCombinesSixMastersWithIndependentCursors() {
        val masterIds = (1..6).map { "master-$it" }
        val previousCursors = masterIds.associateWith { "0" }
        val results = masterIds.mapIndexed { index, nodeId ->
            success(nodeId, listOf(key("key-$nodeId")), "cursor-$index")
        }

        val merged = ClusterKeyScanner.merge(previousCursors, results)

        assertEquals(masterIds.map { "key-$it" }, merged.keys.map { it.key })
        assertEquals(
            masterIds.mapIndexed { index, nodeId -> nodeId to "cursor-$index" }.toMap(),
            ClusterKeyScanner.decodeToken(assertNotNull(merged.nextCursorToken)),
        )
    }

    @Test
    fun mergeCombinesThreeMastersWithPartialFailure() {
        val merged = ClusterKeyScanner.merge(
            previousCursors = mapOf("master-1" to "3", "master-2" to "7", "master-3" to "11"),
            results = listOf(
                success("master-1", listOf(key("k1")), "4"),
                ClusterNodeScanResult.Failure("master-2", "timeout: timed out"),
                success("master-3", listOf(key("k3")), null),
            ),
        )

        assertEquals(listOf("k1", "k3"), merged.keys.map { it.key })
        assertEquals(
            mapOf("master-1" to "4", "master-2" to "7"),
            ClusterKeyScanner.decodeToken(assertNotNull(merged.nextCursorToken)),
        )
        assertEquals(listOf("master-2: timeout: timed out"), merged.partialFailures)
    }

    @Test
    fun reconcileCursorsDropsStaleNodesAndAddsNewMasters() {
        val token = ClusterKeyScanner.encodeToken(
            mapOf(
                "master-a" to "12",
                "removed-master" to "99",
            ),
        )

        val reconciled = ClusterKeyScanner.reconcileCursors(
            cursorToken = token,
            masterNodeIds = listOf("master-b", "master-a"),
        )

        assertEquals(
            linkedMapOf("master-a" to "12", "master-b" to "0"),
            reconciled,
        )
    }

    @Test
    fun scanMastersParallelMergesThreeMasters() = runBlocking {
        val results = ClusterKeyScanner.scanMastersParallel(
            previousCursors = mapOf("master-1" to "0", "master-2" to "0", "master-3" to "0"),
            maxParallelMasters = 2,
        ) { nodeId, cursor ->
            delay(5)
            success(nodeId, listOf(key(nodeId)), if (cursor == "0") "next" else null)
        }

        val merged = ClusterKeyScanner.merge(
            mapOf("master-1" to "0", "master-2" to "0", "master-3" to "0"),
            results,
        )

        assertEquals(listOf("master-1", "master-2", "master-3"), merged.keys.map { it.key })
        assertEquals(
            mapOf("master-1" to "next", "master-2" to "next", "master-3" to "next"),
            ClusterKeyScanner.decodeToken(assertNotNull(merged.nextCursorToken)),
        )
    }

    @Test
    fun scanMastersParallelDoesNotFailEntireScanOnSingleNodeFailure() = runBlocking {
        val results = ClusterKeyScanner.scanMastersParallel(
            previousCursors = (1..6).associate { "master-$it" to "0" },
            maxParallelMasters = 3,
        ) { nodeId, _ ->
            if (nodeId == "master-3") {
                ClusterNodeScanResult.Failure(nodeId, "connection reset")
            } else {
                success(nodeId, listOf(key(nodeId)), null)
            }
        }

        val merged = ClusterKeyScanner.merge(
            (1..6).associate { "master-$it" to "0" },
            results,
        )

        assertEquals(
            listOf("master-1", "master-2", "master-4", "master-5", "master-6"),
            merged.keys.map { it.key },
        )
        assertEquals(
            mapOf("master-3" to "0"),
            ClusterKeyScanner.decodeToken(assertNotNull(merged.nextCursorToken)),
        )
        assertEquals(listOf("master-3: connection reset"), merged.partialFailures)
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
