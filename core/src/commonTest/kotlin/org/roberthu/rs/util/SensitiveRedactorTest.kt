package org.roberthu.rs.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensitiveRedactorTest {
    @Test
    fun redactsRedisUriPassword() {
        val input = "redis://user:secret@127.0.0.1:6379"
        val redacted = SensitiveRedactor.redact(input)
        assertFalse(redacted.contains("secret"))
        assertTrue(redacted.contains("******@"))
    }

    @Test
    fun redactsAuthorizationHeader() {
        val input = "Authorization: Bearer abc123"
        assertEquals("Authorization: ***", SensitiveRedactor.redact(input))
    }

    @Test
    fun redactsTokenAssignments() {
        val input = "access_token=xyz"
        assertEquals("access_token=***", SensitiveRedactor.redact(input))
    }

    @Test
    fun redactsPasswordAssignments() {
        val input = "password=topsecret"
        assertEquals("password=***", SensitiveRedactor.redact(input))
    }
}
