package com.photosoap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SwipeDirectionTest {

    @Test
    fun `enum has two values`() {
        assertEquals(2, SwipeDirection.entries.size)
    }

    @Test
    fun `Keep direction exists`() {
        assertNotNull(SwipeDirection.Keep)
    }

    @Test
    fun `Delete direction exists`() {
        assertNotNull(SwipeDirection.Delete)
    }
}
