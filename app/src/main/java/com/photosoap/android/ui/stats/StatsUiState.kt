package com.photosoap.android.ui.stats

import com.photosoap.android.domain.model.UserStats

data class StatsUiState(
    val stats: UserStats? = null,
    val isLoading: Boolean = true,
    val keepDeleteRatio: Float = 0f,
)
