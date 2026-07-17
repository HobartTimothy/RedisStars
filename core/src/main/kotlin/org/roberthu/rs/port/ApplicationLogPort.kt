package org.roberthu.rs.port

import kotlinx.coroutines.flow.Flow
import org.roberthu.rs.domain.ApplicationLogEntry

interface ApplicationLogPort {
    suspend fun loadRecent(maxEntries: Int): Result<List<ApplicationLogEntry>>

    fun watch(): Flow<ApplicationLogEntry>

    suspend fun export(entries: List<ApplicationLogEntry>, targetPath: String): Result<Unit>
}
