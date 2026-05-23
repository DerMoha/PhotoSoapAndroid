package com.photosoap.data.local.mapper

import com.photosoap.data.local.db.entity.UserStatsEntity

fun UserStatsEntity.toDomain(): Map<String, Any> = mapOf(
    "totalReviewed" to totalReviewed,
    "totalDeleted" to totalDeleted,
    "totalKept" to totalKept,
    "storageFreed" to storageFreed,
    "sessionReviewCount" to sessionReviewCount,
    "currentStreak" to currentStreak,
    "bestStreak" to bestStreak,
    "dayStreak" to dayStreak,
    "lastReviewDate" to (lastReviewDate ?: 0L),
    "todayReviewCount" to todayReviewCount,
    "todayDate" to (todayDate ?: 0L),
    "bestDayReviewCount" to bestDayReviewCount,
    "dailyChallengeProgress" to dailyChallengeProgress,
    "dailyChallengeTarget" to dailyChallengeTarget,
    "dailyChallengeType" to dailyChallengeType,
    "dailyChallengeDate" to (dailyChallengeDate ?: 0L),
)
