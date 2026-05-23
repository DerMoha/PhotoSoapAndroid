package com.photosoap.android.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserStatsTest {

    @Test
    fun `copy updates fields correctly`() {
        val original = UserStats()
        val updated = original.copy(
            totalReviewed = original.totalReviewed + 1,
            totalDeleted = original.totalDeleted + 1,
            currentStreak = original.currentStreak + 1,
            bestStreak = maxOf(original.bestStreak, original.currentStreak + 1),
            storageFreed = original.storageFreed + 1024000,
        )

        assertEquals(1, updated.totalReviewed)
        assertEquals(1, updated.totalDeleted)
        assertEquals(0, updated.totalKept)
        assertEquals(1, updated.currentStreak)
        assertEquals(1, updated.bestStreak)
        assertEquals(1024000L, updated.storageFreed)
    }

    @Test
    fun `default values are zero`() {
        val stats = UserStats()
        assertEquals(0, stats.totalReviewed)
        assertEquals(0, stats.totalDeleted)
        assertEquals(0, stats.totalKept)
        assertEquals(0L, stats.storageFreed)
        assertEquals(0, stats.sessionReviewCount)
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.bestStreak)
        assertEquals(0, stats.dayStreak)
        assertEquals(0, stats.todayReviewCount)
        assertEquals(0, stats.bestDayReviewCount)
    }
}
