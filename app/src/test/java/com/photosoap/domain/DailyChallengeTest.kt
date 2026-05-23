package com.photosoap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChallengeTest {

    @Test
    fun `generates valid challenge for today`() {
        val challenge = DailyChallenge.generateForToday()
        assertTrue(challenge.target > 0)
        assertTrue(challenge.progress == 0)
    }

    @Test
    fun `progressFraction returns correct value`() {
        val challenge = DailyChallenge(
            type = DailyChallenge.ChallengeType.Review,
            target = 30,
            progress = 15,
        )
        assertEquals(0.5f, challenge.progressFraction)
    }

    @Test
    fun `progressFraction clamps to 1f`() {
        val challenge = DailyChallenge(
            type = DailyChallenge.ChallengeType.Review,
            target = 30,
            progress = 40,
        )
        assertEquals(1f, challenge.progressFraction)
    }

    @Test
    fun `isComplete is true when progress meets target`() {
        val challenge = DailyChallenge(
            type = DailyChallenge.ChallengeType.Delete,
            target = 20,
            progress = 20,
        )
        assertTrue(challenge.isComplete)
    }

    @Test
    fun `isComplete is false when below target`() {
        val challenge = DailyChallenge(
            type = DailyChallenge.ChallengeType.Streak,
            target = 15,
            progress = 10,
        )
        assertFalse(challenge.isComplete)
    }
}
