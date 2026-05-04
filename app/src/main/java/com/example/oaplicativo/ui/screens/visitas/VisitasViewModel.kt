package com.example.oaplicativo.ui.screens.visitas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.local.LocalDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VisitasStats(
    val hojeTotal: Int = 0,
    val recordePessoal: Int = 0,
    val percentualBoa: Float = 0f,
    val percentualRegular: Float = 0f,
    val percentualRuim: Float = 0f,
    val totalHistorico: Int = 0 // Isso virá do Supabase futuramente
)

class VisitasViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LocalDatabase(application)
    
    private val _stats = MutableStateFlow(VisitasStats())
    val stats: StateFlow<VisitasStats> = _stats

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val todayMap = db.getTodayStats()
            val record = db.getPersonalRecord()
            
            val total = todayMap["Total"] ?: 0
            
            // Calcula o recorde (Apenas cadastros úteis: Boa + Regular)
            val usefulToday = (todayMap["Boa"] ?: 0) + (todayMap["Regular"] ?: 0)
            db.updateRecordIfHigher(usefulToday)
            
            val pBoa = if (total > 0) (todayMap["Boa"]?.toFloat() ?: 0f) / total else 0f
            val pReg = if (total > 0) (todayMap["Regular"]?.toFloat() ?: 0f) / total else 0f
            val pRui = if (total > 0) (todayMap["Ruim"]?.toFloat() ?: 0f) / total else 0f

            _stats.value = VisitasStats(
                hojeTotal = total,
                recordePessoal = maxOf(record, usefulToday),
                percentualBoa = pBoa,
                percentualRegular = pReg,
                percentualRuim = pRui
            )
        }
    }
}
