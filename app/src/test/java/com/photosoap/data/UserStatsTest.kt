package com.photosoap.data.local.db.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class UserStatsTest {

    @Test
    fun `default entity has zero values`() {
        val stats = UserStatsEntity()
        assertEquals(0, stats.totalReviewed)
        assertEquals(0, stats.totalDeleted)
        assertEquals(0, stats.totalKept)
        assertEquals(0L, stats.storageFreed)
        assertEquals(0, stats.sessionReviewCount)
    }

    @Test
    fun `copy preserves all fields`() {
        val stats = UserStatsEntity(
            totalReviewed = 100,
            totalDeleted = 20,
            storageFreed = 1024L,
        )
        val copied = stats.copy(totalReviewed = 101)
        assertEquals(101, copied.totalReviewed)
        assertEquals(20, copied.totalDeleted)
        assertEquals(1024L, copied.storageFreed)
    }
}
