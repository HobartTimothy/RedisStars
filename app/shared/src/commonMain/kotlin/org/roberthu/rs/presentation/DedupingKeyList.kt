package org.roberthu.rs.presentation

import org.roberthu.rs.domain.RedisKeySummary

/**
 * Ordered key list with O(1) membership checks for incremental SCAN page merges.
 * First occurrence of each key name wins; later duplicates are ignored.
 */
class DedupingKeyList {
    private val keysByName = LinkedHashMap<String, RedisKeySummary>()
    private var snapshot: List<RedisKeySummary> = emptyList()
    private var dirty = false

    val size: Int get() = keysByName.size

    fun clear() {
        keysByName.clear()
        snapshot = emptyList()
        dirty = false
    }

    fun replaceAll(keys: List<RedisKeySummary>): List<RedisKeySummary> {
        clear()
        return appendAll(keys)
    }

    fun appendAll(keys: List<RedisKeySummary>): List<RedisKeySummary> {
        var changed = false
        for (key in keys) {
            if (keysByName.putIfAbsent(key.key, key) == null) {
                changed = true
            }
        }
        if (changed) {
            dirty = true
        }
        return toList()
    }

    fun toList(): List<RedisKeySummary> {
        if (dirty) {
            snapshot = keysByName.values.toList()
            dirty = false
        }
        return snapshot
    }
}
