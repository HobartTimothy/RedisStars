package org.roberthu.rs.redis

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

class RedisReconnectBackoffTest {
    @Test
    fun delayMs_isCappedAndAppliesFullJitter() {
        val random = Random(seed = 7)
        val delays = (1..8).map { attempt ->
            RedisReconnectBackoff.delayMs(
                attempt = attempt,
                baseMs = 250,
                capMs = 2_000,
                random = random,
            )
        }

        delays.forEach { delay ->
            assertTrue(delay in 1..2_000)
        }
        assertTrue(delays.last() <= 2_000)
        // With full jitter, successive capped delays should not all be identical.
        assertTrue(delays.distinct().size > 1)
    }

    @Test
    fun delayMs_withoutJitterSpan_staysWithinHalfToFullBase() {
        val delay = RedisReconnectBackoff.delayMs(
            attempt = 1,
            baseMs = 100,
            capMs = 1_000,
            random = Random(0),
        )
        assertTrue(delay in 50..100)
    }
}
