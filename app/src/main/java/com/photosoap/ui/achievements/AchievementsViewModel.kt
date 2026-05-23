package com.photosoap.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosoap.domain.model.Achievement
import com.photosoap.domain.repository.AchievementRepository
import com.photosoap.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    init {
        loadAchievements()
    }

    fun loadAchievements() {
        viewModelScope.launch {
            val stats = statsRepository.getStats()
            val dbAchievements = achievementRepository.getAchievements()
            val unlockedIds = dbAchievements.filter { it.isUnlocked }.map { it.id }.toSet()

            _achievements.value = Achievement.all.map { achievement ->
                val isUnlocked = unlockedIds.contains(achievement.id)
                val progress = when (achievement.id) {
                    "first_steps" -> stats.totalReviewed.coerceAtMost(50).toFloat() / 50f
                    "spring_cleaning" -> stats.totalDeleted.coerceAtMost(200).toFloat() / 200f
                    "memory_keeper" -> stats.totalKept.coerceAtMost(500).toFloat() / 500f
                    "streak_master" -> stats.bestStreak.coerceAtMost(100).toFloat() / 100f
                    "daily_devotee" -> stats.dayStreak.coerceAtMost(14).toFloat() / 14f
                    "storage_saver" -> (stats.storageFreed.toFloat() / (5L * 1024 * 1024 * 1024)).coerceAtMost(1f)
                    "century_club" -> stats.sessionReviewCount.coerceAtMost(500).toFloat() / 500f
                    "photo_pro" -> stats.totalReviewed.coerceAtMost(5000).toFloat() / 5000f
                    "decisive" -> stats.currentStreak.coerceAtMost(200).toFloat() / 200f
                    "cleanup_champion" -> stats.totalDeleted.coerceAtMost(2000).toFloat() / 2000f
                    else -> 0f
                }
                achievement.copy(isUnlocked = isUnlocked, progress = progress)
            }
        }
    }
}
