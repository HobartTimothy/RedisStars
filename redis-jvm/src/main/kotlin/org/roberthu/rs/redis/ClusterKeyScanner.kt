package org.roberthu.rs.redis

import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.models.partitions.RedisClusterNode
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
) {
    fun scan(query: ScanQuery): ScanPage {
        val masters = connection.partitions
            .filter { it.`is`(RedisClusterNode.NodeFlag.UPSTREAM) }
            .associateBy { it.nodeId }
        val previousCursors = query.cursorToken
            ?.let(::decodeToken)
            ?: masters.keys.sorted().associateWithTo(linkedMapOf()) { "0" }

        val results = previousCursors.map { (nodeId, cursor) ->
            val node = masters[nodeId]
            if (node == null) {
                ClusterNodeScanResult.Failure(nodeId, "master is no longer in the cluster topology")
            } else {
                try {
                    val commands = connection.getConnection(nodeId).sync()
                    ClusterNodeScanResult.Success(
                        nodeId,
                        scanPage(
                            LettuceScanCommands(commands),
                            query.copy(cursorToken = cursor),
                        ),
                    )
                } catch (failure: Throwable) {
                    val error = LettuceExceptionMapper.map(failure)
                    ClusterNodeScanResult.Failure(nodeId, "${error.code}: ${error.message}")
                }
            }
        }
        return merge(previousCursors, results)
    }

    companion object {
        private val json = Json {
            allowStructuredMapKeys = false
            ignoreUnknownKeys = false
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
                        previousCursors[result.nodeId]?.let { nextCursors[result.nodeId] = it }
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
