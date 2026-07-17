package org.roberthu.rs.redis

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.models.partitions.RedisClusterNode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import java.util.Base64

internal sealed interface ClusterNodeScanResult {
    val nodeId: String

    data class Success(
        override val nodeId: String,
        val page: ScanPage,
    ) : ClusterNodeScanResult

    data class Failure(
        override val nodeId: String,
        val reason: String,
    ) : ClusterNodeScanResult
}

internal class ClusterKeyScanner(
    private val connection: StatefulRedisClusterConnection<String, String>,
    private val maxParallelMasters: Int = DEFAULT_MAX_PARALLEL_MASTERS,
) {
    suspend fun scan(query: ScanQuery): ScanPage {
        val masters = connection.partitions
            .filter { it.`is`(RedisClusterNode.NodeFlag.UPSTREAM) }
            .associateBy { it.nodeId }
        val previousCursors = reconcileCursors(query.cursorToken, masters.keys)

        val results = scanMastersParallel(previousCursors, maxParallelMasters) { nodeId, cursor ->
            val node = masters[nodeId]
            if (node == null) {
                ClusterNodeScanResult.Failure(nodeId, TOPOLOGY_REMOVED_REASON)
            } else {
                try {
                    val nodeConnection = connection.getConnection(nodeId)
                    val commands = nodeConnection.sync()
                    ClusterNodeScanResult.Success(
                        nodeId,
                        scanPage(
                            LettuceScanCommands.fromNodeConnection(nodeConnection, commands),
                            query.copy(cursorToken = cursor),
                        ),
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Throwable) {
                    val error = LettuceExceptionMapper.map(failure)
                    ClusterNodeScanResult.Failure(nodeId, "${error.code}: ${error.message}")
                }
            }
        }
        return merge(previousCursors, results)
    }

    companion object {
        internal const val DEFAULT_MAX_PARALLEL_MASTERS = 8
        internal const val TOPOLOGY_REMOVED_REASON = "master is no longer in the cluster topology"

        private val json = Json {
            allowStructuredMapKeys = false
            ignoreUnknownKeys = false
        }

        fun reconcileCursors(
            cursorToken: String?,
            masterNodeIds: Collection<String>,
        ): LinkedHashMap<String, String> {
            val decoded = cursorToken?.let(::decodeToken)
            return masterNodeIds.sorted().associateWithTo(linkedMapOf()) { nodeId ->
                decoded?.get(nodeId) ?: "0"
            }
        }

        suspend fun scanMastersParallel(
            previousCursors: Map<String, String>,
            maxParallelMasters: Int,
            scanNode: suspend (nodeId: String, cursor: String) -> ClusterNodeScanResult,
        ): List<ClusterNodeScanResult> = coroutineScope {
            val semaphore = Semaphore(maxParallelMasters.coerceAtLeast(1))
            previousCursors.map { (nodeId, cursor) ->
                async {
                    semaphore.withPermit {
                        scanNode(nodeId, cursor)
                    }
                }
            }.awaitAll()
        }

        fun merge(
            previousCursors: Map<String, String>,
            results: List<ClusterNodeScanResult>,
        ): ScanPage {
            val keysByName = linkedMapOf<String, RedisKeySummary>()
            val nextCursors = linkedMapOf<String, String>()
            val partialFailures = mutableListOf<String>()

            results.forEach { result ->
                when (result) {
                    is ClusterNodeScanResult.Success -> {
                        result.page.keys.forEach { key -> keysByName.putIfAbsent(key.key, key) }
                        result.page.nextCursorToken?.let { nextCursors[result.nodeId] = it }
                        partialFailures += result.page.partialFailures.map {
                            "${result.nodeId}: $it"
                        }
                    }

                    is ClusterNodeScanResult.Failure -> {
                        if (result.reason != TOPOLOGY_REMOVED_REASON) {
                            previousCursors[result.nodeId]?.let { nextCursors[result.nodeId] = it }
                        }
                        partialFailures += "${result.nodeId}: ${result.reason}"
                    }
                }
            }

            return ScanPage(
                keys = keysByName.values.toList(),
                nextCursorToken = nextCursors.takeIf { it.isNotEmpty() }?.let(::encodeToken),
                partialFailures = partialFailures,
            )
        }

        fun encodeToken(cursors: Map<String, String>): String {
            val jsonBytes = json.encodeToString(cursors).encodeToByteArray()
            return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes)
        }

        fun decodeToken(token: String): Map<String, String> =
            try {
                val jsonText = Base64.getUrlDecoder().decode(token).decodeToString()
                json.decodeFromString<Map<String, String>>(jsonText)
            } catch (_: Throwable) {
                throw RedisError.Validation("Invalid cluster cursor token")
            }
    }
}
