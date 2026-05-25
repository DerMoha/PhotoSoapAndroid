package com.photosoap.android.domain.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AchievementTest {

    @Test
    fun `first steps unlocks at 50 reviews`() {
        val stats = UserStats(totalReviewed = 50)
        val achievement = Achievement.ALL.find { it.id == "first_steps" }!!
        assertTrue(achievement.isUnlocked(stats))
    }

    @Test
    fun `first steps does not unlock below 50`() {
        val stats = UserStats(totalReviewed = 49)
        val achievement = Achievement.ALL.find { it.id == "first_steps" }!!
        assertFalse(achievement.isUnlocked(stats))
    }

    @Test
    fun `spring cleaning unlocks at 200 deletions`() {
        val stats = UserStats(totalDeleted = 200)
        val achievement = Achievement.ALL.find { it.id == "spring_cleaning" }!!
        assertTrue(achievement.isUnlocked(stats))
    }

    @Test
    fun `streak master unlocks at 100 best streak`() {
        val stats = UserStats(bestStreak = 100)
        val achievement = Achievement.ALL.find { it.id == "streak_master" }!!
        assertTrue(achievement.isUnlocked(stats))
    }

    @Test
    fun `storage saver unlocks at 5GB`() {
        val stats = UserStats(storageFreed = 5_000_000_000L)
        val achievement = Achievement.ALL.find { it.id == "storage_saver" }!!
        assertTrue(achievement.isUnlocked(stats))
    }

    @Test
    fun `all achievements have unique ids`() {
        val ids = Achievement.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `progress computes correct fraction`() {
        val stats = UserStats(totalReviewed = 25)
        val achievement = Achievement.ALL.find { it.id == "first_steps" }!!
        assertEquals(0.5f, achievement.progress(stats))
    }

    @Test
    fun `isUnlocked at exactly boundary returns true`() {
        val stats = UserStats(totalDeleted = 200)
        val achievement = Achievement.ALL.find { it.id == "spring_cleaning" }!!
        assertTrue(achievement.isUnlocked(stats))
    }

    @Test
    fun `isUnlocked at boundary minus one returns false`() {
        val stats = UserStats(bestStreak = 99)
        val achievement = Achievement.ALL.find { it.id == "streak_master" }!!
        assertFalse(achievement.isUnlocked(stats))
    }

    @Test
    fun `daily challenge generates valid targets`() {
        val challenge = DailyChallenge.generate()
        assertTrue(challenge.target in 10..50)
    }

    @Test
    fun `daily challenge type matches target range`() {
        repeat(20) {
            val challenge = DailyChallenge.generate()
            when (challenge.type) {
                DailyChallenge.ChallengeType.REVIEW -> assertTrue(challenge.target in 20..50, "review target ${challenge.target} out of range")
                DailyChallenge.ChallengeType.DELETE -> assertTrue(challenge.target in 10..25, "delete target ${challenge.target} out of range")
                DailyChallenge.ChallengeType.STREAK -> assertTrue(challenge.target in 10..25, "streak target ${challenge.target} out of range")
            }
        }
    }
}
