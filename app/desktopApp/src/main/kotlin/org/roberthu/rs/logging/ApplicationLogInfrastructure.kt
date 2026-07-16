package org.roberthu.rs.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.roberthu.rs.domain.ApplicationLogEntry
import org.roberthu.rs.domain.ApplicationLogLevel
import org.roberthu.rs.util.ApplicationLogLineParser
import org.roberthu.rs.util.SensitiveRedactor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

class ApplicationLogRingBuffer(
    private val maxEntries: Int = 10_000,
) {
    private val lock = Any()
    private val entries = ArrayDeque<ApplicationLogEntry>(maxEntries)
    private val idCounter = AtomicLong(0)
    private val _events = MutableSharedFlow<ApplicationLogEntry>(extraBufferCapacity = 256)
    val events: SharedFlow<ApplicationLogEntry> = _events.asSharedFlow()

    fun append(event: ILoggingEvent) {
        val entry = event.toEntry()
        synchronized(lock) {
            if (entries.size >= maxEntries) {
                entries.removeFirst()
            }
            entries.addLast(entry)
        }
        _events.tryEmit(entry)
    }

    fun snapshot(limit: Int = maxEntries): List<ApplicationLogEntry> = synchronized(lock) {
        if (limit >= entries.size) {
            entries.toList()
        } else {
            entries.takeLast(limit)
        }
    }

    private fun ILoggingEvent.toEntry(): ApplicationLogEntry {
        val timestamp = TIMESTAMP_FORMATTER.format(
            Instant.ofEpochMilli(timeStamp).atZone(ZoneId.systemDefault()),
        )
        val message = SensitiveRedactor.redact(formattedMessage ?: "")
        val details = throwableProxy?.let { proxy ->
            buildString {
                append(proxy.className)
                proxy.message?.let { append(": ").append(SensitiveRedactor.redact(it)) }
                proxy.stackTraceElementProxyArray?.forEach { frame ->
                    append('\n')
                    append("    at ")
                    append(frame.stackTraceElement)
                }
            }.takeIf { it.isNotBlank() }
        }
        return ApplicationLogEntry(
            id = "live-${idCounter.incrementAndGet()}",
            timestamp = timestamp,
            level = ApplicationLogLevel.fromLogbackName(level.levelStr) ?: ApplicationLogLevel.INFO,
            target = loggerName,
            message = message,
            details = details,
        )
    }

    companion object {
        private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}

class ApplicationLogAppender : AppenderBase<ILoggingEvent>() {
    override fun append(eventObject: ILoggingEvent) {
        ApplicationLogRegistry.ringBuffer.append(eventObject)
    }
}

object ApplicationLogRegistry {
    val ringBuffer = ApplicationLogRingBuffer()

    fun mergeWithFileTail(fileEntries: List<ApplicationLogEntry>): List<ApplicationLogEntry> {
        val live = ringBuffer.snapshot()
        if (fileEntries.isEmpty()) return live
        if (live.isEmpty()) return fileEntries
        val seen = linkedSetOf<String>()
        val merged = ArrayList<ApplicationLogEntry>(fileEntries.size + live.size)
        fileEntries.forEach { entry ->
            val key = dedupeKey(entry)
            if (seen.add(key)) merged.add(entry)
        }
        live.forEach { entry ->
            val key = dedupeKey(entry)
            if (seen.add(key)) merged.add(entry)
        }
        return merged
    }

    private fun dedupeKey(entry: ApplicationLogEntry): String =
        "${entry.timestamp}|${entry.level}|${entry.target}|${entry.message}|${entry.details}"
}

object ApplicationLogFileReader {
    fun readTailLines(file: java.nio.file.Path, maxLines: Int, maxBytes: Long = 2L * 1024 * 1024): List<ApplicationLogEntry> {
        if (!java.nio.file.Files.exists(file)) return emptyList()
        return runCatching {
            val bytes = java.nio.file.Files.size(file)
            val readBytes = minOf(bytes, maxBytes)
            val buffer = ByteArray(readBytes.toInt())
            java.nio.file.Files.newInputStream(file).use { input ->
                if (bytes > readBytes) {
                    input.skip(bytes - readBytes)
                }
                val read = input.read(buffer)
                val text = String(buffer, 0, read, Charsets.UTF_8)
                val lines = text.lineSequence().dropWhile { it.isBlank() }.toList()
                val tail = if (lines.size > maxLines) lines.takeLast(maxLines) else lines
                tail.mapNotNull { ApplicationLogLineParser.parseLine(it) }
            }
        }.getOrElse { emptyList() }
    }
}
