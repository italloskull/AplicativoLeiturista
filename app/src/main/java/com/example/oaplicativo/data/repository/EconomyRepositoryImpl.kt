package com.example.oaplicativo.data.repository

import android.content.Context
import android.util.Log
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.domain.repository.EconomyRepository
import com.example.oaplicativo.model.EconomyUpdate
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EconomyRepositoryImpl private constructor() : EconomyRepository {
    private val client = SupabaseClient.client
    private val _items = MutableStateFlow<List<EconomyUpdate>>(emptyList())
    override val items: StateFlow<List<EconomyUpdate>> = _items.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    private var localPendingItems: List<EconomyUpdate> = emptyList()
    private var remoteItems: List<EconomyUpdate> = emptyList()

    private var applicationContext: Context? = null
    private val localDb: LocalDatabase get() = LocalDatabase.getInstance(applicationContext!!)

    fun initialize(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
            // Carga inicial rápida
            scope.launch { fetchEconomyUpdates(null, true) }
        }
    }

    override suspend fun fetchEconomyUpdates(cidadeId: String?, isAdmin: Boolean) {
        mutex.withLock {
            try {
                val userCityName = getFriendlyCityName(cidadeId)
                Log.d("debugs", "🔍 [GE] Buscando na nuvem. Cidade: $userCityName | Admin: $isAdmin")

                // 1. Busca local de pendentes (SÊNIOR FIX: Usa o nome da cidade no SQLite)
                localPendingItems = try {
                    localDb.getPendingEconomyUpdates(userCityName, isAdmin).map { it.second.copy(isSynced = false) }
                } catch (e: Exception) {
                    Log.e("debugs", "❌ [GE] Erro banco local: ${e.message}")
                    emptyList()
                }

                // 2. Busca do servidor
                val remoteList = try {
                    client.postgrest["grandes_empreendimentos"]
                        .select {
                            if (!isAdmin && userCityName != null) {
                                filter { eq("cidade", userCityName) }
                            }
                            order("data", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                            limit(100)
                        }.decodeList<EconomyUpdate>()
                } catch (e: Exception) {
                    Log.e("debugs", "❌ [GE] Erro servidor: ${e.message}")
                    emptyList()
                }

                remoteItems = remoteList.map { it.copy(isSynced = true) }
                combineAndEmit()
            } catch (e: Exception) {
                Log.e("debugs", "❌ [GE] Falha crítica no fetch: ${e.message}")
            }
        }
    }

    private fun getFriendlyCityName(cidadeId: String?): String? {
        return when (cidadeId) {
            "c2be642b-2823-41b9-8f54-0b8c84db9a14" -> "Itapoá"
            "ff9166b8-63b1-4481-a26a-64778181fa08" -> "Guabiruba"
            "74df760a-0120-42b4-bb4d-03cfd92e79b0" -> "Gaivota"
            "93fee74f-6cbb-4638-868d-ef5c17b081a4" -> "Gravatal"
            "9ed90b8c-1b63-44b7-88cd-c2b9b6babcc7" -> "Sombrio"
            else -> null
        }
    }

    override suspend fun saveEconomyUpdate(item: EconomyUpdate) {
        localDb.saveEconomyUpdateOffline(item)
        fetchEconomyUpdates(null, true)
    }

    override suspend fun saveEconomyUpdates(items: List<EconomyUpdate>) {
        mutex.withLock {
            try {
                client.postgrest["grandes_empreendimentos"].upsert(items)
                Log.d("debugs", "✅ [GE] Sincronizado com sucesso.")
            } catch (e: Exception) {
                Log.e("debugs", "❌ [GE] Erro no Upsert: ${e.message}")
                throw e
            }
        }
    }

    override fun updateLocalEconomyUpdates(items: List<EconomyUpdate>) {
        localPendingItems = items
        combineAndEmit()
    }

    override fun clearCache() {
        remoteItems = emptyList()
        localPendingItems = emptyList()
        _items.value = emptyList()
        Log.d("debugs", "🧹 [GE] Cache de memória limpo.")
    }

    private fun combineAndEmit() {
        scope.launch(Dispatchers.Default) {
            mutex.withLock {
                val remoteIds = remoteItems.mapNotNull { it.id }.toSet()
                
                val combined = (localPendingItems.asSequence().map { 
                    if (remoteIds.contains(it.id)) it.copy(isSynced = true) else it
                } + remoteItems.asSequence())
                .distinctBy { it.id }
                .sortedByDescending { it.date }
                .toList()
                
                _items.value = combined
                Log.d("debugs", "📊 [GE] Lista reativa emitida: ${combined.size} itens globais.")
            }
        }
    }

    override suspend fun getItemById(id: String): EconomyUpdate? {
        return items.value.find { it.id == id }
    }

    companion object {
        @Volatile private var instance: EconomyRepositoryImpl? = null
        fun getInstance(): EconomyRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: EconomyRepositoryImpl().also { instance = it }
            }
        }
    }
}
