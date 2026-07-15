package org.roberthu.rs.usecase

import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.port.KeyBrowserPort

class BrowseKeys(
    private val keyBrowser: KeyBrowserPort,
) {
    private var activeScanJob: Job? = null

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
        activeScanJob?.cancel()
        activeScanJob = null
        keyBrowser.cancelScan()
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
