package org.roberthu.rs.util

import org.roberthu.rs.domain.ApplicationLogEntry
import org.roberthu.rs.domain.ApplicationLogLevel

object ApplicationLogLineParser {
    private val linePattern = Regex(
        """^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+(\w+)\s+(\S+)\s+-\s+(.*)$""",
    )
    private var idCounter = 0L

    fun parseLine(
        line: String,
        idFactory: () -> String = { "file-${++idCounter}" },
    ): ApplicationLogEntry? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        val match = linePattern.matchEntire(trimmed) ?: return fallbackTextEntry(trimmed, idFactory)
        val level = ApplicationLogLevel.fromLogbackName(match.groupValues[2]) ?: return fallbackTextEntry(trimmed, idFactory)
        val message = SensitiveRedactor.redact(match.groupValues[4])
        val details = splitDetails(message)
        return ApplicationLogEntry(
            id = idFactory(),
            timestamp = match.groupValues[1],
            level = level,
            target = match.groupValues[3],
            message = details.first,
            details = details.second,
        )
    }

    fun formatForExport(entry: ApplicationLogEntry): String {
        val target = entry.target?.let { " $it" }.orEmpty()
        val body = buildString {
            append(entry.message)
            entry.details?.let {
                if (it.isNotBlank()) {
                    append('\n')
                    append(it)
                }
            }
        }
        return "${entry.timestamp} ${entry.level.name.padEnd(5)}$target - $body"
    }

    private fun fallbackTextEntry(line: String, idFactory: () -> String): ApplicationLogEntry =
        ApplicationLogEntry(
            id = idFactory(),
            timestamp = "",
            level = ApplicationLogLevel.INFO,
            target = null,
            message = SensitiveRedactor.redact(line),
        )

    private fun splitDetails(message: String): Pair<String, String?> {
        val newline = message.indexOf('\n')
        return if (newline < 0) {
            message to null
        } else {
            message.substring(0, newline) to message.substring(newline + 1)
        }
    }
}
