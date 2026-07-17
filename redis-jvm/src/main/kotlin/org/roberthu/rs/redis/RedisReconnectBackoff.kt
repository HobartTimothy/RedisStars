package org.roberthu.rs.redis

import kotlin.math.min
import kotlin.random.Random

internal object RedisReconnectBackoff {
    fun delayMs(
        attempt: Int,
        baseMs: Long,
        capMs: Long,
        random: Random = Random.Default,
    ): Long {
        var delayMs = baseMs.coerceAtLeast(1)
        repeat((attempt - 1).coerceIn(0, 62)) {
            if (delayMs >= capMs || delayMs > Long.MAX_VALUE / 2) {
                return applyJitter(min(delayMs, capMs), capMs, random)
            }
            delayMs *= 2
        }
        return applyJitter(min(delayMs, capMs), capMs, random)
    }

    private fun applyJitter(delayMs: Long, capMs: Long, random: Random): Long {
        val floor = (delayMs / 2).coerceAtLeast(1)
        val span = (delayMs - floor).coerceAtLeast(0)
        val jittered = floor + if (span == 0L) 0L else random.nextLong(span + 1)
        return min(jittered, capMs)
    }
}
