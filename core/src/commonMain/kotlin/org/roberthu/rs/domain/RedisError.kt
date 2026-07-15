package org.roberthu.rs.domain

sealed class RedisError(
    open val code: String,
    override val message: String,
) : Exception(message) {
    data class AuthFailed(
        override val message: String = "Authentication failed",
    ) : RedisError("AUTH_FAILED", message)

    data class Timeout(
        override val message: String = "Redis operation timed out",
    ) : RedisError("TIMEOUT", message)

    data class Network(
        override val message: String = "Redis network error",
    ) : RedisError("NETWORK", message)

    data class ReadOnly(
        override val message: String = "Redis node is read-only",
    ) : RedisError("READ_ONLY", message)

    data class MovedAsk(
        override val message: String = "Redis cluster redirection",
    ) : RedisError("MOVED_ASK", message)

    data class ConnectionClosed(
        override val message: String = "Redis connection is closed",
    ) : RedisError("CONNECTION_CLOSED", message)

    data class CrossSlotRename(
        override val message: String = "Cannot rename keys across cluster slots",
    ) : RedisError("CROSS_SLOT_RENAME", message)

    data class PartialClusterFailure(
        override val message: String = "One or more Redis cluster nodes failed",
    ) : RedisError("PARTIAL_CLUSTER_FAILURE", message)

    data class ValueTooLarge(
        override val message: String = "Redis value exceeds the configured limit",
    ) : RedisError("VALUE_TOO_LARGE", message)

    data class NotSupported(
        override val message: String = "Redis operation is not supported",
    ) : RedisError("NOT_SUPPORTED", message)

    data class Validation(
        override val message: String,
    ) : RedisError("VALIDATION", message)

    data class Unknown(
        override val message: String = "Unknown Redis error",
    ) : RedisError("UNKNOWN", message)
}
