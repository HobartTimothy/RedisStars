package org.roberthu.rs.domain

object RedisValueCodec {
    fun encode(value: String): ByteArray = value.encodeToByteArray()

    fun decode(value: ByteArray): String = value.decodeToString()
}
