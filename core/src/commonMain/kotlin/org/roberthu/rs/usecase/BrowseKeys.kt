package org.roberthu.rs.usecase

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import org.roberthu.rs.domain.RedisDatabaseSummary
import org.roberthu.rs.domain.RedisError
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.port.KeyBrowserPort

class BrowseKeys(
    private val keyBrowser: KeyBrowserPort,
) {
    private var activeScanJob: Job? = null
    private var generation: Long = 0L

    suspend fun scan(query: ScanQuery): Result<ScanPage> {
        val job = currentCoroutineContext()[Job]
        activeScanJob = job
        return try {
            keyBrowser.scan(query)
        } finally {
            if (activeScanJob === job) {
                activeScanJob = null
            }
        }
    }

    fun cancel() {
        generation += 1
        activeScanJob?.cancel()
        activeScanJob = null
        keyBrowser.cancelScan()
    }

    /** Monotonic id used by presentation to ignore stale scan results. */
    fun nextRequestId(): Long {
        generation += 1
        return generation
    }

    fun currentRequestId(): Long = generation

    suspend fun listDatabases(): Result<List<RedisDatabaseSummary>> =
        keyBrowser.listDatabases()

    suspend fun selectDatabase(index: Int): Result<Unit> {
        if (index < 0) {
            return Result.failure(RedisError.Validation("数据库编号不能为负数"))
        }
        cancel()
        return keyBrowser.selectDatabase(index)
    }
}

object KeyScanMerger {
    fun mergeDedupPage(
        batches: List<List<RedisKeySummary>>,
        offset: Int,
        limit: Int,
    ): List<RedisKeySummary> {
        require(offset >= 0) { "offset must not be negative" }
        require(limit >= 0) { "limit must not be negative" }

        return batches
            .asSequence()
            .flatten()
            .distinctBy(RedisKeySummary::key)
            .drop(offset)
            .take(limit)
            .toList()
    }
}
