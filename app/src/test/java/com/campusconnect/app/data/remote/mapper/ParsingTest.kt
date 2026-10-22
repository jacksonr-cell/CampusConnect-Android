package com.campusconnect.app.data.remote.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParsingTest {
    @Test
    fun `parses ISO timestamp to epoch milliseconds`() {
        assertEquals(1_735_689_600_000L, "2025-01-01T00:00:00Z".toEpochMillisOrNull())
    }

    @Test
    fun `invalid or missing timestamp returns null`() {
        assertNull("not-a-date".toEpochMillisOrNull())
        assertNull(null.toEpochMillisOrNull())
    }

    @Test
    fun `parses decimal strings and uses fallback for invalid values`() {
        assertEquals(8_500.25, "8500.25".toSafeDouble(), 0.0)
        assertEquals(0.0, "invalid".toSafeDouble(), 0.0)
        assertEquals(42.0, "invalid".toSafeDouble(42.0), 0.0)
    }

    @Test
    fun `nullable decimal parser preserves invalidity`() {
        assertEquals(12.5, "12.5".toSafeDoubleOrNull()!!, 0.0)
        assertNull("invalid".toSafeDoubleOrNull())
        assertNull(null.toSafeDoubleOrNull())
    }
}
