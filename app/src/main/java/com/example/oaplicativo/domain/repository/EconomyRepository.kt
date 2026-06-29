package com.example.oaplicativo.domain.repository

import com.example.oaplicativo.model.EconomyUpdate
import kotlinx.coroutines.flow.StateFlow

interface EconomyRepository {
    val items: StateFlow<List<EconomyUpdate>>
    suspend fun fetchEconomyUpdates(cidadeId: String? = null, isAdmin: Boolean = false)
    suspend fun saveEconomyUpdate(item: EconomyUpdate)
    suspend fun saveEconomyUpdates(items: List<EconomyUpdate>)
    fun updateLocalEconomyUpdates(items: List<EconomyUpdate>)
    fun clearCache()
    suspend fun getItemById(id: String): EconomyUpdate?
}
