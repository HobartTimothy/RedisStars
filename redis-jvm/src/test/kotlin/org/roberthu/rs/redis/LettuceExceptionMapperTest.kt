package org.roberthu.rs.redis

import org.roberthu.rs.domain.RedisError
import kotlin.test.Test
import kotlin.test.assertIs

class LettuceExceptionMapperTest {
    @Test
    fun mapsAuthenticationFailures() {
        assertIs<RedisError.AuthFailed>(
            LettuceExceptionMapper.map(IllegalStateException("WRONGPASS invalid username-password pair")),
        )
    }

    @Test
    fun mapsTimeouts() {
        assertIs<RedisError.Timeout>(
            LettuceExceptionMapper.map(IllegalStateException("Command timed out after 5 seconds")),
        )
    }

    @Test
    fun mapsClosedConnections() {
        assertIs<RedisError.ConnectionClosed>(
            LettuceExceptionMapper.map(IllegalStateException("Connection reset by peer")),
        )
    }

    @Test
    fun mapsOtherNetworkFailures() {
        assertIs<RedisError.Network>(
            LettuceExceptionMapper.map(IllegalStateException("Connection refused: localhost")),
        )
    }

    @Test
    fun mapsClusterRedirections() {
        assertIs<RedisError.MovedAsk>(
            LettuceExceptionMapper.map(IllegalStateException("MOVED 3999 127.0.0.1:6381")),
        )
        assertIs<RedisError.MovedAsk>(
            LettuceExceptionMapper.map(IllegalStateException("ASK 3999 127.0.0.1:6381")),
        )
    }

    @Test
    fun mapsCrossSlotRenameFailures() {
        assertIs<RedisError.CrossSlotRename>(
            LettuceExceptionMapper.map(
                IllegalStateException("CROSSSLOT Keys in request don't hash to the same slot"),
            ),
        )
    }

    @Test
    fun mapsReadOnlyFailures() {
        assertIs<RedisError.ReadOnly>(
            LettuceExceptionMapper.map(IllegalStateException("READONLY You can't write against a read only replica")),
        )
    }

    @Test
    fun examinesNestedCauses() {
        val nested = IllegalStateException(
            "Command failed",
            IllegalArgumentException("NOAUTH Authentication required"),
        )

        assertIs<RedisError.AuthFailed>(LettuceExceptionMapper.map(nested))
    }

    @Test
    fun mapsUnrecognizedFailuresWithoutLeakingTheirMessage() {
        assertIs<RedisError.Unknown>(
            LettuceExceptionMapper.map(IllegalStateException("redis://user:secret@example.test")),
        )
    }
}
