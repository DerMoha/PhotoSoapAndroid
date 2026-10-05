package com.photosoap.android.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReviewProgressTest {
    @Test fun `empty periods are not complete`() {
        assertFalse(ReviewProgress().isComplete)
        assertEquals(0f, ReviewProgress().fraction)
    }
    @Test fun `unfinished periods never display one hundred percent`() {
        assertEquals(0.99f, ReviewProgress(999, 1000).displayFraction)
        assertFalse(ReviewProgress(999, 1000).isComplete)
        assertEquals(1f, ReviewProgress(1000, 1000).displayFraction)
        assertTrue(ReviewProgress(1000, 1000).isComplete)
    }
}
