package org.roberthu.rs.usecase

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.roberthu.rs.domain.ScanPage
import org.roberthu.rs.domain.ScanQuery
import org.roberthu.rs.port.KeyBrowserPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrowseKeysTest {
    @Test
    fun passesCursorAndOtherQueryFieldsToPortUnchanged() = runTest {
        val port = FakeKeyBrowserPort()
        val useCase = BrowseKeys(port)
        val query = ScanQuery(
            database = 3,
            pattern = "user:*",
            cursorToken = "42",
            countHint = 25,
        )

        val result = useCase.scan(query)

        assertTrue(result.isSuccess)
        assertEquals(listOf(query), port.queries)
    }

    @Test
    fun cancelStopsActiveScanAndNotifiesPort() = runTest {
        val port = FakeKeyBrowserPort(suspendScan = true)
        val useCase = BrowseKeys(port)
        val scan = launch { useCase.scan(ScanQuery()) }
        port.started.await()

        useCase.cancel()
        scan.join()

        assertTrue(scan.isCancelled)
        assertEquals(1, port.cancelCalls)
    }

    private class FakeKeyBrowserPort(
        private val suspendScan: Boolean = false,
    ) : KeyBrowserPort {
        val queries = mutableListOf<ScanQuery>()
        val started = CompletableDeferred<Unit>()
        var cancelCalls = 0

        override suspend fun scan(query: ScanQuery): Result<ScanPage> {
            queries += query
            started.complete(Unit)
            if (suspendScan) awaitCancellation()
            return Result.success(ScanPage(emptyList(), null))
        }

        override fun cancelScan() {
            cancelCalls += 1
        }

        override suspend fun listDatabases() =
            Result.success(listOf(org.roberthu.rs.domain.RedisDatabaseSummary(0, 0)))

        override suspend fun selectDatabase(index: Int) = Result.success(Unit)
    }
}
