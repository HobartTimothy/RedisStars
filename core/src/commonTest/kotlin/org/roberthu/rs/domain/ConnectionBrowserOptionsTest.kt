package org.roberthu.rs.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseIndexListParserTest {
    @Test
    fun parse_blank_returnsEmptyList() {
        assertEquals(emptyList(), DatabaseIndexListParser.parse("").getOrThrow())
        assertEquals(emptyList(), DatabaseIndexListParser.parse("   ").getOrThrow())
    }

    @Test
    fun parse_singleIndices_returnsSortedDistinctValues() {
        assertEquals(listOf(0, 1, 3), DatabaseIndexListParser.parse("3,1,0,1").getOrThrow())
    }

    @Test
    fun parse_rangesAreExpanded() {
        assertEquals(listOf(0, 1, 3, 4, 5), DatabaseIndexListParser.parse("0,1,3-5").getOrThrow())
    }

    @Test
    fun parse_toleratesWhitespaceAroundTokens() {
        assertEquals(listOf(0, 1, 2, 3), DatabaseIndexListParser.parse(" 0 , 1 , 2-3 ").getOrThrow())
    }

    @Test
    fun parse_invalidToken_fails() {
        assertTrue(DatabaseIndexListParser.parse("abc").isFailure)
        assertTrue(DatabaseIndexListParser.parse("1,,2").isFailure)
        assertTrue(DatabaseIndexListParser.parse("-1").isFailure)
    }

    @Test
    fun parse_reversedRange_fails() {
        assertTrue(DatabaseIndexListParser.parse("5-3").isFailure)
    }

    @Test
    fun parse_malformedRange_fails() {
        assertTrue(DatabaseIndexListParser.parse("1-2-3").isFailure)
    }

    @Test
    fun format_emptyList_returnsEmptyString() {
        assertEquals("", DatabaseIndexListParser.format(emptyList()))
    }

    @Test
    fun format_compressesConsecutiveRuns() {
        assertEquals("0-1,3-5", DatabaseIndexListParser.format(listOf(5, 4, 3, 1, 0, 1)))
    }

    @Test
    fun format_singleValuesStaySeparate() {
        assertEquals("0,2,4", DatabaseIndexListParser.format(listOf(0, 2, 4)))
    }

    @Test
    fun parseThenFormat_roundTrips() {
        val parsed = DatabaseIndexListParser.parse("0,1,3-5").getOrThrow()
        assertEquals("0-1,3-5", DatabaseIndexListParser.format(parsed))
    }
}

class ConnectionBrowserOptionsTest {
    @Test
    fun normalized_fillsBlankPatternAndSeparator() {
        val options = ConnectionBrowserOptions(keyPattern = "  ", keySeparator = "").normalized()

        assertEquals("*", options.keyPattern)
        assertEquals(":", options.keySeparator)
    }

    @Test
    fun normalized_coercesBatchSizeIntoBounds() {
        assertEquals(1, ConnectionBrowserOptions(keyLoadBatchSize = -5).normalized().keyLoadBatchSize)
        assertEquals(100_000, ConnectionBrowserOptions(keyLoadBatchSize = 999_999).normalized().keyLoadBatchSize)
        assertEquals(250, ConnectionBrowserOptions(keyLoadBatchSize = 250).normalized().keyLoadBatchSize)
    }

    @Test
    fun normalized_clearsFilterValuesWhenShowAll() {
        val options = ConnectionBrowserOptions(
            databaseFilterMode = DatabaseFilterMode.ShowAll,
            databaseFilterValues = listOf(1, 2, 3),
        ).normalized()

        assertEquals(emptyList(), options.databaseFilterValues)
    }

    @Test
    fun normalized_sortsAndDedupesFilterValuesWhenSpecified() {
        val options = ConnectionBrowserOptions(
            databaseFilterMode = DatabaseFilterMode.ShowSpecified,
            databaseFilterValues = listOf(3, 1, 1, 2),
        ).normalized()

        assertEquals(listOf(1, 2, 3), options.databaseFilterValues)
    }

    @Test
    fun filterDatabases_showAll_returnsEverything() {
        val all = listOf(
            RedisDatabaseSummary(index = 0, keyCount = 10),
            RedisDatabaseSummary(index = 1, keyCount = 20),
        )
        val options = ConnectionBrowserOptions(databaseFilterMode = DatabaseFilterMode.ShowAll)

        assertEquals(all, options.filterDatabases(all))
    }

    @Test
    fun filterDatabases_showSpecified_keepsOnlyListed() {
        val all = listOf(
            RedisDatabaseSummary(index = 0, keyCount = 10),
            RedisDatabaseSummary(index = 1, keyCount = 20),
            RedisDatabaseSummary(index = 2, keyCount = 30),
        )
        val options = ConnectionBrowserOptions(
            databaseFilterMode = DatabaseFilterMode.ShowSpecified,
            databaseFilterValues = listOf(0, 2),
        )

        assertEquals(listOf(all[0], all[2]), options.filterDatabases(all))
    }

    @Test
    fun filterDatabases_hideSpecified_removesListed() {
        val all = listOf(
            RedisDatabaseSummary(index = 0, keyCount = 10),
            RedisDatabaseSummary(index = 1, keyCount = 20),
            RedisDatabaseSummary(index = 2, keyCount = 30),
        )
        val options = ConnectionBrowserOptions(
            databaseFilterMode = DatabaseFilterMode.HideSpecified,
            databaseFilterValues = listOf(1),
        )

        assertEquals(listOf(all[0], all[2]), options.filterDatabases(all))
    }
}
