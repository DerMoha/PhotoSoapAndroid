package com.photosoap.data.local.mapper

import com.photosoap.data.local.db.entity.UserStatsEntity
import com.photosoap.domain.model.StatsDisplay
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsMappingTest {

    @Test
    fun `entity maps to display model correctly`() {
        val entity = UserStatsEntity(
            totalReviewed = 150,
            totalDeleted = 30,
            totalKept = 120,
            storageFreed = 1024L * 1024 * 100,
            sessionReviewCount = 50,
            currentStreak = 25,
            bestStreak = 75,
            dayStreak = 7,
            todayReviewCount = 20,
            bestDayReviewCount = 40,
        )

        val display = entity.toDisplayModel()

        assertEquals(150, display.totalReviewed)
        assertEquals(30, display.totalDeleted)
        assertEquals(120, display.totalKept)
        assertEquals(1024L * 1024 * 100, display.storageFreed)
        assertEquals(50, display.sessionReviewCount)
        assertEquals(25, display.currentStreak)
        assertEquals(75, display.bestStreak)
        assertEquals(7, display.dayStreak)
        assertEquals(20, display.todayReviewCount)
        assertEquals(40, display.bestDayReviewCount)
    }

    @Test
    fun `default entity maps to zero display`() {
        val entity = UserStatsEntity()
        val display = entity.toDisplayModel()

        assertEquals(0, display.totalReviewed)
        assertEquals(0, display.totalDeleted)
        assertEquals(0, display.totalKept)
        assertEquals(0L, display.storageFreed)
    }
}
