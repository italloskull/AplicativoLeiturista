package com.example.oaplicativo.domain.repository

import com.example.oaplicativo.model.EconomyUpdate
import kotlinx.coroutines.flow.StateFlow

interface EconomyRepository {
    val items: StateFlow<List<EconomyUpdate>>
    suspend fun fetchEconomyUpdates()
    suspend fun saveEconomyUpdate(item: EconomyUpdate)
    suspend fun getItemById(id: String): EconomyUpdate?
}
