package com.photosoap.android.domain.model

data class DailyChallenge(
    val target: Int,
    val type: ChallengeType,
) {
    enum class ChallengeType(val label: String) {
        REVIEW("review"),
        DELETE("delete"),
        STREAK("streak"),
    }

    companion object {
        fun generate(): DailyChallenge {
            val dayOfYear = java.time.LocalDate.now().dayOfYear
            val seed = dayOfYear * 73 + 11
            return when (kotlin.math.abs(seed % 3)) {
                0 -> DailyChallenge(
                    target = minOf(20 + kotlin.math.abs(seed % 31), 50),
                    type = ChallengeType.REVIEW,
                )
                1 -> DailyChallenge(
                    target = minOf(10 + kotlin.math.abs(seed % 16), 25),
                    type = ChallengeType.DELETE,
                )
                else -> DailyChallenge(
                    target = minOf(10 + kotlin.math.abs(seed % 16), 25),
                    type = ChallengeType.STREAK,
                )
            }
        }
    }
}
