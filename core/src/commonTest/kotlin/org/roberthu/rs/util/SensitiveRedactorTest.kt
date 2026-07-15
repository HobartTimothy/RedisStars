package org.roberthu.rs.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SensitiveRedactorTest {
    @Test
    fun stripsPasswordQueryAndAuthHeaders() {
        val raw = "redis://:s3cret@localhost:6379/0 Authorization: Bearer tok"

        val output = SensitiveRedactor.redact(raw)

        assertFalse(output.contains("s3cret"))
        assertFalse(output.contains("Bearer tok"))
        assertTrue(output.contains("***"))
    }

    @Test
    fun stripsNamedUriPassword() {
        val output = SensitiveRedactor.redact("redis://alice:secret@localhost:6379")

        assertFalse(output.contains("secret"))
        assertTrue(output.contains("redis://alice:***@localhost:6379"))
    }

    @Test
    fun stripsPasswordAssignment() {
        val output = SensitiveRedactor.redact("host=localhost password=hunter2 database=0")

        assertFalse(output.contains("hunter2"))
        assertTrue(output.contains("password=***"))
    }
}
