package com.example.oaplicativo.ui.screens.visitas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.repository.StatsRepositoryImpl
import com.example.oaplicativo.domain.repository.StatsRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VisitasStats(
    val hojeTotal: Int = 0,
    val recordePessoal: Int = 0,
    val percentualBoa: Float = 0f,
    val percentualRegular: Float = 0f,
    val percentualRuim: Float = 0f,
    val totalHistorico: Int = 0
)

class VisitasViewModel(
    application: Application,
    private val repository: StatsRepository = StatsRepositoryImpl.getInstance(application)
) : com.example.oaplicativo.util.BaseViewModel<VisitasStats>() {
    
    val stats: StateFlow<VisitasStats> = repository.stats

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            emitLoading()
            try {
                repository.refreshStats()
                emitSuccess(repository.stats.value)
            } catch (e: Exception) {
                emitError("Erro ao carregar estatísticas", e)
            }
        }
    }
}
