package org.roberthu.rs.domain

/**
 * Spaced integer sort values for sidebar ordering.
 * Root-level groups and ungrouped connections share one sort space; connections inside a group
 * use a separate sort space scoped by [ConnectionProfile.groupId].
 */
object SidebarOrder {
    const val STEP: Long = 1024L

    fun normalize(orders: List<Long>): List<Long> =
        orders.mapIndexed { index, _ -> (index + 1).toLong() * STEP }

    fun computeInsertSortOrder(
        existingOrders: List<Long>,
        insertIndex: Int,
    ): SortOrderInsertResult {
        val sorted = existingOrders
        when {
            sorted.isEmpty() -> return SortOrderInsertResult(STEP, needsNormalization = false)
            insertIndex <= 0 -> {
                val next = sorted.first()
                val value = next - STEP
                return SortOrderInsertResult(
                    sortOrder = value,
                    needsNormalization = value <= 0L || value >= next,
                )
            }
            insertIndex >= sorted.size -> {
                return SortOrderInsertResult(
                    sortOrder = sorted.last() + STEP,
                    needsNormalization = false,
                )
            }
            else -> {
                val previous = sorted[insertIndex - 1]
                val next = sorted[insertIndex]
                if (next - previous <= 1L) {
                    val normalized = normalize(sorted)
                    val normalizedPrevious = normalized[insertIndex - 1]
                    val normalizedNext = normalized[insertIndex]
                    return SortOrderInsertResult(
                        sortOrder = (normalizedPrevious + normalizedNext) / 2,
                        needsNormalization = true,
                        normalizedOrders = normalized,
                    )
                }
                return SortOrderInsertResult(
                    sortOrder = (previous + next) / 2,
                    needsNormalization = false,
                )
            }
        }
    }

    fun nextSortOrder(existingOrders: Collection<Long>): Long =
        (existingOrders.maxOrNull() ?: 0L) + STEP

    fun compareBySortOrderThenNameThenId(
        sortOrder: (String) -> Long,
        name: (String) -> String,
        id: (String) -> String,
    ): Comparator<String> = Comparator { left, right ->
        compareValuesBy(left, right, sortOrder, name, id)
    }
}

data class SortOrderInsertResult(
    val sortOrder: Long,
    val needsNormalization: Boolean,
    val normalizedOrders: List<Long>? = null,
)
