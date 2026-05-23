package com.photosoap.android.data.repository

import com.photosoap.android.domain.model.UserStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatsMappingTest {

    @Test
    fun `toDomain maps all fields`() {
        val entity = com.photosoap.android.data.local.db.entity.UserStatsEntity(
            totalReviewed = 100,
            totalDeleted = 30,
            totalKept = 70,
            storageFreed = 1_000_000_000L,
            sessionReviewCount = 50,
            currentStreak = 5,
            bestStreak = 15,
            dayStreak = 3,
            todayReviewCount = 20,
            bestDayReviewCount = 40,
            dailyChallengeProgress = 10,
            dailyChallengeTarget = 20,
            dailyChallengeType = "review",
        )

        val domain = entity.toDomain()
        assertEquals(100, domain.totalReviewed)
        assertEquals(30, domain.totalDeleted)
        assertEquals(70, domain.totalKept)
        assertEquals(1_000_000_000L, domain.storageFreed)
        assertEquals(50, domain.sessionReviewCount)
        assertEquals(5, domain.currentStreak)
        assertEquals(15, domain.bestStreak)
        assertEquals(3, domain.dayStreak)
        assertEquals(20, domain.todayReviewCount)
        assertEquals(40, domain.bestDayReviewCount)
    }

    @Test
    fun `toEntity roundtrips correctly`() {
        val domain = UserStats(
            totalReviewed = 42,
            totalDeleted = 7,
            totalKept = 35,
            storageFreed = 500_000_000L,
            sessionReviewCount = 10,
            currentStreak = 3,
            bestStreak = 12,
            dayStreak = 2,
            todayReviewCount = 5,
            bestDayReviewCount = 25,
            dailyChallengeProgress = 3,
            dailyChallengeTarget = 20,
            dailyChallengeType = "review",
        )

        val entity = domain.toEntity()
        val roundtripped = entity.toDomain()

        assertEquals(domain, roundtripped)
    }
}
