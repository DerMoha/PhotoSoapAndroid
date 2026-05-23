package com.photosoap.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementTest {

    @Test
    fun `all returns 10 achievements`() {
        assertEquals(10, Achievement.all.size)
    }

    @Test
    fun `achievement ids are unique`() {
        val ids = Achievement.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `default achievement is not unlocked`() {
        Achievement.all.forEach { achievement ->
            assertFalse(achievement.isUnlocked)
        }
    }

    @Test
    fun `achievement copy with unlocked true works`() {
        val achievement = Achievement.all.first().copy(isUnlocked = true)
        assertTrue(achievement.isUnlocked)
    }
}
