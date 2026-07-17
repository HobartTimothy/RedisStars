package org.roberthu.rs.domain

data class BinarySafeString(
    val utf8: String?,
    val base64: String,
    val truncated: Boolean,
)

data class HashEntry(
    val field: BinarySafeString,
    val value: BinarySafeString,
)

data class HashScanPage(
    val entries: List<HashEntry>,
    val nextCursorToken: String?,
)

data class SetScanPage(
    val members: List<BinarySafeString>,
    val nextCursorToken: String?,
)

data class ZSetEntry(
    val member: BinarySafeString,
    val score: Double,
)

data class ZSetScanPage(
    val entries: List<ZSetEntry>,
    val nextCursorToken: String?,
)
