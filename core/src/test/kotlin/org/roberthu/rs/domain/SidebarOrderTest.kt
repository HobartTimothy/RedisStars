package org.roberthu.rs.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SidebarOrderTest {
    @Test
    fun computeInsertSortOrder_firstItemUsesStep() {
        val result = SidebarOrder.computeInsertSortOrder(emptyList(), 0)
        assertEquals(1024L, result.sortOrder)
        assertFalse(result.needsNormalization)
    }

    @Test
    fun computeInsertSortOrder_middleInsertUsesAverage() {
        val result = SidebarOrder.computeInsertSortOrder(listOf(1024L, 3072L), 1)
        assertEquals(2048L, result.sortOrder)
        assertFalse(result.needsNormalization)
    }

    @Test
    fun computeInsertSortOrder_appendUsesPreviousPlusStep() {
        val result = SidebarOrder.computeInsertSortOrder(listOf(1024L, 2048L), 2)
        assertEquals(3072L, result.sortOrder)
    }

    @Test
    fun computeInsertSortOrder_prependUsesNextMinusStep() {
        val result = SidebarOrder.computeInsertSortOrder(listOf(2048L, 3072L), 0)
        assertEquals(1024L, result.sortOrder)
    }

    @Test
    fun normalize_reassignsSpacedValues() {
        assertEquals(listOf(1024L, 2048L, 3072L), SidebarOrder.normalize(listOf(1L, 2L, 3L)))
    }

    @Test
    fun nextSortOrder_appendsAfterMax() {
        assertEquals(4096L, SidebarOrder.nextSortOrder(listOf(1024L, 2048L, 3072L)))
    }
}
