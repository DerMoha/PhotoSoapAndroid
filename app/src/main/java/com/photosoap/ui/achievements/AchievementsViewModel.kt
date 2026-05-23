package com.photosoap.ui.achievements

import androidx.lifecycle.ViewModel
import com.photosoap.domain.model.Achievement
import com.photosoap.domain.repository.AchievementRepository
import com.photosoap.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private fun loadAchievements() {
        // Simple synchronous load for now
        val unlocked = mutableSetOf<String>()
        _achievements.value = Achievement.all.map { a ->
            a.copy(isUnlocked = unlocked.contains(a.id))
        }
    }
}
