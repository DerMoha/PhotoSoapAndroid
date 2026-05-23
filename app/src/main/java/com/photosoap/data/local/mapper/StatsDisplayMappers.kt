package com.photosoap.data.local.mapper

import com.photosoap.data.local.db.entity.UserStatsEntity
import com.photosoap.domain.model.StatsDisplay

fun UserStatsEntity.toDisplayModel(): StatsDisplay = StatsDisplay(
    totalReviewed = totalReviewed,
    totalDeleted = totalDeleted,
    totalKept = totalKept,
    storageFreed = storageFreed,
    sessionReviewCount = sessionReviewCount,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    dayStreak = dayStreak,
    todayReviewCount = todayReviewCount,
    bestDayReviewCount = bestDayReviewCount,
)
