package org.roberthu.rs.shell.keys

import org.roberthu.rs.domain.RedisKeySummary

internal sealed interface KeyTreeNode {
    val sortKey: String

    data class Folder(
        val segment: String,
        val children: List<KeyTreeNode>,
    ) : KeyTreeNode {
        override val sortKey: String = segment
    }

    data class Leaf(val key: RedisKeySummary) : KeyTreeNode {
        override val sortKey: String = key.key
    }
}

internal fun buildKeyTree(
    keys: List<RedisKeySummary>,
    separator: String,
): List<KeyTreeNode> {
    if (separator.isEmpty()) {
        return keys.sortedBy { it.key }.map { KeyTreeNode.Leaf(it) }
    }

    val root = mutableMapOf<String, NodeBuilder>()
    for (key in keys) {
        val parts = key.key.split(separator)
        if (parts.size == 1) {
            root.getOrPut(parts[0]) { NodeBuilder() }.leaf = key
        } else {
            var current = root.getOrPut(parts[0]) { NodeBuilder() }
            for (index in 1 until parts.lastIndex) {
                current = current.children.getOrPut(parts[index]) { NodeBuilder() }
            }
            current.children.getOrPut(parts.last()) { NodeBuilder() }.leaf = key
        }
    }

    return root.entries
        .flatMap { (segment, builder) -> builder.toNodes(segment) }
        .sortedWith(treeNodeComparator)
}

private class NodeBuilder {
    var leaf: RedisKeySummary? = null
    val children = mutableMapOf<String, NodeBuilder>()
}

private fun NodeBuilder.toNodes(segment: String): List<KeyTreeNode> {
    val childNodes = children.entries
        .flatMap { (childSegment, childBuilder) -> childBuilder.toNodes(childSegment) }
        .sortedWith(treeNodeComparator)

    return if (children.isEmpty()) {
        listOfNotNull(leaf?.let { KeyTreeNode.Leaf(it) })
    } else {
        val folderChildren = buildList {
            leaf?.let { add(KeyTreeNode.Leaf(it)) }
            addAll(childNodes)
        }
        listOf(KeyTreeNode.Folder(segment = segment, children = folderChildren))
    }
}

private val treeNodeComparator = compareBy<KeyTreeNode> { it !is KeyTreeNode.Folder }
    .thenBy { it.sortKey }
