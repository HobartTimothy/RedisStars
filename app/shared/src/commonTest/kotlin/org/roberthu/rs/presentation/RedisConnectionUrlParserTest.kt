package org.roberthu.rs.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RedisConnectionUrlParserTest {
    @Test
    fun redis_defaultPort() {
        val result = RedisConnectionUrlParser.parse("redis://localhost")

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("localhost", parsed.host)
        assertEquals(6379, parsed.port)
        assertEquals(0, parsed.database)
        assertFalse(parsed.tlsEnabled)
        assertNull(parsed.username)
        assertNull(parsed.password)
    }

    @Test
    fun rediss_enablesTls() {
        val result = RedisConnectionUrlParser.parse("rediss://example.com:6380")

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertTrue(parsed.tlsEnabled)
        assertEquals("example.com", parsed.host)
        assertEquals(6380, parsed.port)
    }

    @Test
    fun usernameAndPassword() {
        val result = RedisConnectionUrlParser.parse("redis://username:password@localhost:6379/0")

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("username", parsed.username)
        assertEquals("password", parsed.password)
        assertEquals("localhost", parsed.host)
        assertEquals(6379, parsed.port)
        assertEquals(0, parsed.database)
    }

    @Test
    fun passwordOnly() {
        val result = RedisConnectionUrlParser.parse("redis://:password@localhost:6379/0")

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertNull(parsed.username)
        assertEquals("password", parsed.password)
    }

    @Test
    fun databaseFromPath() {
        val result = RedisConnectionUrlParser.parse("redis://localhost:6379/2")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().database)
    }

    @Test
    fun rejectsIllegalScheme() {
        val result = RedisConnectionUrlParser.parse("http://localhost:6379")

        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("scheme", ignoreCase = true))
    }

    @Test
    fun rejectsIllegalPort() {
        val result = RedisConnectionUrlParser.parse("redis://localhost:99999")

        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("port", ignoreCase = true))
    }

    @Test
    fun rejectsMissingHost() {
        val result = RedisConnectionUrlParser.parse("redis://:6379")

        assertTrue(result.isFailure)
        assertTrue(result.error!!.contains("host", ignoreCase = true))
    }

    @Test
    fun failureDoesNotReturnPartialResult() {
        val result = RedisConnectionUrlParser.parse("redis://")

        assertTrue(result.isFailure)
        assertNull(result.valueOrNull)
    }

    @Test
    fun fullRedissExample() {
        val result = RedisConnectionUrlParser.parse(
            "rediss://username:password@example.com:6380/2",
        )

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("username", parsed.username)
        assertEquals("password", parsed.password)
        assertEquals("example.com", parsed.host)
        assertEquals(6380, parsed.port)
        assertEquals(2, parsed.database)
        assertTrue(parsed.tlsEnabled)
    }
}
