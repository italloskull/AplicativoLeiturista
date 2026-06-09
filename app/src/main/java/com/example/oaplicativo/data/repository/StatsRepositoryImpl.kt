package com.example.oaplicativo.data.repository

import android.content.Context
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.domain.repository.StatsRepository
import com.example.oaplicativo.ui.screens.visitas.VisitasStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StatsRepositoryImpl private constructor(context: Context) : StatsRepository {
    private val db = LocalDatabase.getInstance(context)
    private val _stats = MutableStateFlow(VisitasStats())
    override val stats: StateFlow<VisitasStats> = _stats.asStateFlow()

    override suspend fun refreshStats() {
        val today = db.getTodayStats()
        val totalHoje = today["Total"] ?: 0
        
        // Sincroniza o recorde pessoal antes de emitir
        db.updateRecordIfHigher(totalHoje)
        val record = db.getPersonalRecord()
        
        val percentBoa = if (totalHoje > 0) (today["Boa"] ?: 0).toFloat() / totalHoje else 0f
        val percentRegular = if (totalHoje > 0) (today["Regular"] ?: 0).toFloat() / totalHoje else 0f
        val percentRuim = if (totalHoje > 0) (today["Ruim"] ?: 0).toFloat() / totalHoje else 0f

        _stats.value = VisitasStats(
            hojeTotal = totalHoje,
            recordePessoal = record,
            percentualBoa = percentBoa,
            percentualRegular = percentRegular,
            percentualRuim = percentRuim,
            totalHistorico = record // Usando o recorde como base de histórico simplificado
        )
    }

    companion object {
        @Volatile
        private var instance: StatsRepositoryImpl? = null
        fun getInstance(context: Context): StatsRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: StatsRepositoryImpl(context.applicationContext).also { instance = it }
            }
        }
    }
}
