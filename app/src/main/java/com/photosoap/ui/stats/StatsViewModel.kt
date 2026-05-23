package com.photosoap.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosoap.data.local.mapper.toDisplayModel
import com.photosoap.domain.model.StatsDisplay
import com.photosoap.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
) : ViewModel() {

    private val _stats = MutableStateFlow(StatsDisplay())
    val stats: StateFlow<StatsDisplay> = _stats.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val entity = statsRepository.getStats()
            _stats.value = entity.toDisplayModel()
        }
    }
}
