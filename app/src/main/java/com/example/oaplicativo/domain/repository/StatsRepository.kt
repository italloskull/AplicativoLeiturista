package com.example.oaplicativo.domain.repository

import com.example.oaplicativo.ui.screens.visitas.VisitasStats
import kotlinx.coroutines.flow.StateFlow

interface StatsRepository {
    val stats: StateFlow<VisitasStats>
    suspend fun refreshStats(cidadeId: String? = null, isAdmin: Boolean = false)
}
