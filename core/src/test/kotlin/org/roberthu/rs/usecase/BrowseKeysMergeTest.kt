package org.roberthu.rs.usecase

import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import kotlin.test.Test
import kotlin.test.assertEquals

class BrowseKeysMergeTest {
    @Test
    fun deduplicatesKeysAcrossBatchesWhilePreservingFirstOccurrenceOrder() {
        val firstUsers = key("users", RedisKeyType.Set)
        val duplicateUsers = key("users", RedisKeyType.Hash)

        val merged = KeyScanMerger.mergeDedupPage(
            batches = listOf(
                listOf(key("alpha"), firstUsers),
                listOf(duplicateUsers, key("omega")),
            ),
            offset = 0,
            limit = 10,
        )

        assertEquals(listOf(key("alpha"), firstUsers, key("omega")), merged)
    }

    @Test
    fun appliesOffsetAndLimitAfterDeduplication() {
        val merged = KeyScanMerger.mergeDedupPage(
            batches = listOf(
                listOf(key("a"), key("b"), key("b")),
                listOf(key("c"), key("d"), key("e")),
            ),
            offset = 2,
            limit = 2,
        )

        assertEquals(listOf(key("c"), key("d")), merged)
    }

    @Test
    fun zeroLimitReturnsEmptyPage() {
        val merged = KeyScanMerger.mergeDedupPage(
            batches = listOf(listOf(key("a"))),
            offset = 0,
            limit = 0,
        )

        assertEquals(emptyList(), merged)
    }

    @Test
    fun offsetBeyondUniqueKeysReturnsEmptyPage() {
        val merged = KeyScanMerger.mergeDedupPage(
            batches = listOf(listOf(key("a"), key("a"))),
            offset = 1,
            limit = 10,
        )

        assertEquals(emptyList(), merged)
    }

    private fun key(
        name: String,
        type: RedisKeyType = RedisKeyType.String,
    ) = RedisKeySummary(name, type)
}
