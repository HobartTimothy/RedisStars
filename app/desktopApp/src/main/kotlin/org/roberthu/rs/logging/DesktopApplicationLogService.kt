package org.roberthu.rs.logging

import kotlinx.coroutines.flow.Flow
import org.roberthu.rs.domain.ApplicationLogEntry
import org.roberthu.rs.port.ApplicationLogPort
import org.roberthu.rs.util.ApplicationLogLineParser
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

class DesktopApplicationLogService(
    private val logDirectory: Path,
    private val ringBuffer: ApplicationLogRingBuffer = ApplicationLogRegistry.ringBuffer,
) : ApplicationLogPort {
    private val currentLogFile: Path
        get() = logDirectory.resolve("redisstars.log")

    override suspend fun loadRecent(maxEntries: Int): Result<List<ApplicationLogEntry>> =
        runCatching {
            val fileEntries = ApplicationLogFileReader.readTailLines(currentLogFile, maxEntries)
            ApplicationLogRegistry.mergeWithFileTail(fileEntries).takeLast(maxEntries)
        }

    override fun watch(): Flow<ApplicationLogEntry> = ringBuffer.events

    override suspend fun export(entries: List<ApplicationLogEntry>, targetPath: String): Result<Unit> =
        runCatching {
            val content = entries.joinToString(separator = "\n") { ApplicationLogLineParser.formatForExport(it) }
            val target = Path.of(targetPath)
            Files.createDirectories(target.parent)
            target.writeText(content)
        }
}
