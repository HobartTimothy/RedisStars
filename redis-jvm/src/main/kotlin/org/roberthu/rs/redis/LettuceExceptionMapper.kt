package org.roberthu.rs.redis

import org.roberthu.rs.domain.RedisError

object LettuceExceptionMapper {
    fun map(throwable: Throwable): RedisError {
        val causes = generateSequence(throwable) { it.cause }.toList()
        val fingerprint = causes.joinToString(" ") {
            "${it::class.qualifiedName.orEmpty()} ${it.message.orEmpty()}"
        }.uppercase()

        return when {
            causes.any { it is RedisError } -> causes.filterIsInstance<RedisError>().first()
            fingerprint.containsAny("WRONGPASS", "NOAUTH", "AUTHENTICATION", "AUTH FAILED") ->
                RedisError.AuthFailed()
            fingerprint.containsAny("TIMEOUT", "TIMED OUT") ->
                RedisError.Timeout()
            fingerprint.contains("CROSSSLOT") ->
                RedisError.CrossSlotRename()
            fingerprint.containsClusterRedirection() ->
                RedisError.MovedAsk()
            fingerprint.contains("READONLY") || fingerprint.contains("READ ONLY") ->
                RedisError.ReadOnly()
            fingerprint.containsAny(
                "CONNECTION RESET",
                "CONNECTION CLOSED",
                "CLOSED CHANNEL",
                "BROKEN PIPE",
                "END OF STREAM",
            ) -> RedisError.ConnectionClosed()
            fingerprint.containsAny(
                "CONNECTEXCEPTION",
                "CONNECTION REFUSED",
                "UNRESOLVEDADDRESS",
                "UNKNOWNHOST",
                "NETWORK",
                "NO ROUTE TO HOST",
            ) -> RedisError.Network()
            else -> RedisError.Unknown()
        }
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any(::contains)

    private fun String.containsClusterRedirection(): Boolean =
        lineSequence().any { line ->
            val normalized = line.trimStart()
            normalized.startsWith("MOVED ") ||
                normalized.startsWith("ASK ") ||
                normalized.contains(" MOVED ") ||
                normalized.contains(" ASK ")
        }
}
