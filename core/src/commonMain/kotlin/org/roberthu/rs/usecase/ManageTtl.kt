package org.roberthu.rs.usecase

import org.roberthu.rs.port.KeyCommandPort

class ManageTtl(
    private val keyCommandPort: KeyCommandPort,
) {
    suspend fun expire(key: String, ttlSeconds: Long) =
        keyCommandPort.setTtl(key, ttlSeconds)

    suspend fun persist(key: String) = keyCommandPort.setTtl(key, null)
}
