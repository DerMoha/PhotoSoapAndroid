package com.photosoap.android.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosoap.android.domain.model.Achievement
import com.photosoap.android.domain.model.UserStats
import com.photosoap.android.domain.repository.AchievementRepository
import com.photosoap.android.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementsUiState(
    val unlockedIds: Set<String> = emptySet(),
    val stats: UserStats? = null,
    val unlockedCount: Int = 0,
    val totalCount: Int = Achievement.ALL.size,
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                achievementRepository.observeUnlockedIds(),
                statsRepository.observeStats(),
            ) { ids, stats ->
                AchievementsUiState(
                    unlockedIds = ids.toSet(),
                    stats = stats,
                    unlockedCount = ids.size,
                    totalCount = Achievement.ALL.size,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
