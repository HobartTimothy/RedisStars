package org.roberthu.rs.presentation

import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DedupingKeyListTest {
    @Test
    fun appendAll_keepsFirstOccurrenceOrderAndSkipsDuplicates() {
        val index = DedupingKeyList()
        index.appendAll(
            listOf(
                RedisKeySummary("a", RedisKeyType.String),
                RedisKeySummary("b", RedisKeyType.Hash),
            ),
        )
        index.appendAll(
            listOf(
                RedisKeySummary("a", RedisKeyType.List),
                RedisKeySummary("c", RedisKeyType.Set),
            ),
        )

        assertEquals(listOf("a", "b", "c"), index.toList().map { it.key })
        assertEquals(RedisKeyType.String, index.toList().first().type)
    }

    @Test
    fun replaceAll_resetsIndex() {
        val index = DedupingKeyList()
        index.appendAll(listOf(RedisKeySummary("old", RedisKeyType.String)))
        index.replaceAll(listOf(RedisKeySummary("new", RedisKeyType.Hash)))

        assertEquals(listOf("new"), index.toList().map { it.key })
    }

    @Test
    fun appendAll_handlesOneHundredPagesWithOverlaps() {
        val index = DedupingKeyList()
        val pageCount = 100
        val pageSize = 1_000
        val overlap = 50

        var expectedUnique = 0
        for (page in 0 until pageCount) {
            val start = page * (pageSize - overlap)
            val pageKeys = (0 until pageSize).map { offset ->
                RedisKeySummary("key-${start + offset}", RedisKeyType.String)
            }
            index.appendAll(pageKeys)
            expectedUnique = start + pageSize
        }

        assertEquals(expectedUnique, index.size)
        assertEquals(expectedUnique, index.toList().size)
        assertEquals("key-0", index.toList().first().key)
        assertEquals("key-${expectedUnique - 1}", index.toList().last().key)

        // Snapshot should be reused when no new unique keys arrive.
        val firstSnapshot = index.toList()
        index.appendAll(listOf(RedisKeySummary("key-0", RedisKeyType.Hash)))
        assertTrue(firstSnapshot === index.toList())
    }
}
