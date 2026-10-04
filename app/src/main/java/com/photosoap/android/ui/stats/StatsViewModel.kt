package com.photosoap.android.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosoap.android.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var persistedStats: com.photosoap.android.domain.model.UserStats? = null

    init {
        observeStats()
    }

    private fun observeStats() {
        viewModelScope.launch {
            statsRepository.observeStats().collect { stats ->
                val ratio = if (stats != null && (stats.totalKept + stats.totalDeleted) > 0) {
                    stats.totalKept.toFloat() / (stats.totalKept + stats.totalDeleted).toFloat()
                } else 0.5f

                persistedStats = stats
                _uiState.update {
                    it.copy(
                        stats = statsForToday(stats),
                        isLoading = false,
                        keepDeleteRatio = ratio,
                    )
                }
            }
        }
    }
    fun refreshDailyValues() {
        _uiState.update { it.copy(stats = statsForToday(persistedStats)) }
    }

    private fun statsForToday(stats: com.photosoap.android.domain.model.UserStats?): com.photosoap.android.domain.model.UserStats? {
        val today = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return stats?.let { if (it.todayDate == today) it else it.copy(todayReviewCount = 0) }
    }

}
