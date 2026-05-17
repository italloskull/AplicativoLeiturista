package com.example.oaplicativo.data.repository

import android.util.Log
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.domain.repository.EconomyRepository
import com.example.oaplicativo.model.EconomyUpdate
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()

    init {
        scope.launch {
            fetchEconomyUpdates()
        }
    }

    override suspend fun fetchEconomyUpdates() {
        mutex.withLock {
            try {
                val list = client.postgrest["atualizacao_economias"]
                    .select {
                        order("criado_em", order = Order.DESCENDING)
                        limit(100)
                    }.decodeList<EconomyUpdate>()
                _items.value = list
            } catch (e: Exception) {
                Log.e("EconomyRepo", "Erro ao buscar economias: ${e.message}")
            }
        }
    }

    override suspend fun saveEconomyUpdate(item: EconomyUpdate) {
        try {
            client.postgrest["atualizacao_economias"].upsert(item) {
                onConflict = "numero_hd"
            }
            fetchEconomyUpdates()
        } catch (e: Exception) {
            Log.e("EconomyRepo", "Erro ao salvar economia: ${e.message}")
            throw e
        }
    }

    override suspend fun getItemById(id: String): EconomyUpdate? {
        return _items.value.find { it.id == id }
    }

    companion object {
        @Volatile
        private var instance: EconomyRepositoryImpl? = null
        fun getInstance(): EconomyRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: EconomyRepositoryImpl().also { instance = it }
            }
        }
    }
}
