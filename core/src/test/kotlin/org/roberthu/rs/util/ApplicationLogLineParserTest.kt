package org.roberthu.rs.util

import org.roberthu.rs.domain.ApplicationLogLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApplicationLogLineParserTest {
    @Test
    fun parsesStructuredLogLine() {
        val line = "2026-07-16 16:00:00.123 INFO  org.roberthu.rs.Main - Application started"
        val entry = ApplicationLogLineParser.parseLine(line)
        assertNotNull(entry)
        assertEquals("2026-07-16 16:00:00.123", entry.timestamp)
        assertEquals(ApplicationLogLevel.INFO, entry.level)
        assertEquals("org.roberthu.rs.Main", entry.target)
        assertEquals("Application started", entry.message)
    }

    @Test
    fun redactsSensitiveContentWhileParsing() {
        val line = "2026-07-16 16:00:00.123 WARN  redis - connect redis://user:secret@127.0.0.1:6379"
        val entry = ApplicationLogLineParser.parseLine(line)
        assertNotNull(entry)
        assertEquals(ApplicationLogLevel.WARN, entry.level)
        assertEquals(false, entry.message.contains("secret"))
    }

    @Test
    fun fallsBackForMalformedLine() {
        val entry = ApplicationLogLineParser.parseLine("not a structured log line")
        assertNotNull(entry)
        assertEquals("not a structured log line", entry.message)
    }
}
