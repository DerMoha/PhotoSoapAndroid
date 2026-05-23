package com.photosoap.data.local.mapper

import com.photosoap.data.local.db.entity.UserStatsEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsDomainMappersTest {

    @Test
    fun `entity toDomain maps all fields`() {
        val entity = UserStatsEntity(
            totalReviewed = 100,
            totalDeleted = 50,
            totalKept = 50,
            storageFreed = 1024L * 1024,
            sessionReviewCount = 75,
            currentStreak = 12,
            bestStreak = 88,
            dayStreak = 5,
            lastReviewDate = 1700000000L,
            todayReviewCount = 15,
            todayDate = 1700000000L,
            bestDayReviewCount = 30,
            dailyChallengeProgress = 10,
            dailyChallengeTarget = 25,
            dailyChallengeType = "review",
            dailyChallengeDate = 1700000000L,
        )

        val map = entity.toDomain()

        assertEquals(100, map["totalReviewed"])
        assertEquals(50, map["totalDeleted"])
        assertEquals(50, map["totalKept"])
        assertEquals(1024L * 1024, map["storageFreed"])
    }
}
