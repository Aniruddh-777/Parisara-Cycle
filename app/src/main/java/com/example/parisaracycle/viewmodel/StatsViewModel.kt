package com.example.parisaracycle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parisaracycle.data.model.EcoStats
import com.example.parisaracycle.data.repository.EcoStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatsUiState(
    val stats: EcoStats = EcoStats()
)

class StatsViewModel(
    private val ecoStatsRepository: EcoStatsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ecoStatsRepository.stats.collect { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            ecoStatsRepository.refresh()
        }
    }

    companion object {
        fun factory(repository: EcoStatsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    StatsViewModel(repository) as T
            }
    }
}
