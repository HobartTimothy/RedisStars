package org.roberthu.rs.shell.keys

import org.roberthu.rs.domain.RedisKeySummary
import org.roberthu.rs.domain.RedisKeyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KeyTreeBuilderTest {
    @Test
    fun buildKeyTree_groupsBySeparator() {
        val keys = listOf(
            RedisKeySummary("user:1", RedisKeyType.String),
            RedisKeySummary("user:2", RedisKeyType.Hash),
            RedisKeySummary("session:abc", RedisKeyType.Set),
        )

        val tree = buildKeyTree(keys, ":")

        assertEquals(2, tree.size)
        val userFolder = tree.filterIsInstance<KeyTreeNode.Folder>().single { it.segment == "user" }
        assertEquals(2, userFolder.children.filterIsInstance<KeyTreeNode.Leaf>().size)
        val sessionFolder = tree.filterIsInstance<KeyTreeNode.Folder>().single { it.segment == "session" }
        assertEquals("session:abc", (sessionFolder.children.single() as KeyTreeNode.Leaf).key.key)
    }

    @Test
    fun buildKeyTree_flatWhenSeparatorEmpty() {
        val keys = listOf(
            RedisKeySummary("b", RedisKeyType.String),
            RedisKeySummary("a", RedisKeyType.String),
        )

        val tree = buildKeyTree(keys, "")

        assertTrue(tree.all { it is KeyTreeNode.Leaf })
        assertEquals(listOf("a", "b"), tree.map { (it as KeyTreeNode.Leaf).key.key })
    }
}
