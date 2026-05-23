package com.photosoap.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class DailyChallenge(
    val type: ChallengeType,
    val target: Int,
    val progress: Int = 0,
    val date: Long = Clock.System.now().toEpochMilliseconds(),
) {
    enum class ChallengeType { Review, Delete, Streak }

    val isComplete: Boolean get() = progress >= target

    val progressFraction: Float get() =
        if (target > 0) (progress.toFloat() / target).coerceIn(0f, 1f) else 0f

    companion object {
        fun generateForToday(): DailyChallenge {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val dayOfYear = today.dayOfYear
            val type = when (dayOfYear % 3) {
                0 -> ChallengeType.Review
                1 -> ChallengeType.Delete
                else -> ChallengeType.Streak
            }
            val target = when (type) {
                ChallengeType.Review -> 20 + (dayOfYear % 31)
                ChallengeType.Delete -> 10 + (dayOfYear % 16)
                ChallengeType.Streak -> 10 + (dayOfYear % 16)
            }
            return DailyChallenge(type = type, target = target)
        }
    }
}
