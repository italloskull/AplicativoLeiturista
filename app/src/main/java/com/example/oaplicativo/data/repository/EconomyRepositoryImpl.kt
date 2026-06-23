package com.example.oaplicativo.data.repository

import android.util.Log
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.local.LocalDatabase
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

    private var context: android.content.Context? = null

    fun initialize(context: android.content.Context) {
        if (this.context == null) {
            this.context = context.applicationContext
            scope.launch {
                fetchEconomyUpdates()
            }
        }
    }

    override suspend fun fetchEconomyUpdates() {
        mutex.withLock {
            try {
                // 1. Busca do servidor (registros oficiais)
                val remoteList = try {
                    client.postgrest["atualizacao_economias"]
                        .select {
                            order("criado_em", order = Order.DESCENDING)
                            limit(100)
                        }.decodeList<EconomyUpdate>()
                } catch (e: Exception) {
                    Log.w("EconomyRepo", "Offline: Não foi possível buscar do Supabase.")
                    emptyList()
                }
                
                // 2. SÊNIOR FIX: Injeção segura via context inicializado para evitar Crash por morte de Activity
                val localPending = try {
                    val db = context?.let { LocalDatabase.getInstance(it) } ?: throw Exception("Context not ready")
                    db.getPendingEconomyUpdates().map { it.second.copy(isSynced = false) }
                } catch (e: Exception) {
                    Log.e("EconomyRepo", "Erro ao acessar banco local: ${e.message}")
                    emptyList()
                }

                // 3. Mescla as duas listas (IDs locais têm prioridade na visualização para mostrar a nuvem vermelha)
                val combined = (localPending + remoteList).distinctBy { it.id }
                
                _items.value = combined
            } catch (e: Exception) {
                Log.e("EconomyRepo", "Erro ao processar lista de economias: ${e.message}")
            }
        }
    }

    fun updateLocalEconomyUpdates(list: List<EconomyUpdate>) {
        _items.value = list
    }

    override suspend fun saveEconomyUpdate(item: EconomyUpdate) {
        saveEconomyUpdates(listOf(item))
    }

    override suspend fun saveEconomyUpdates(items: List<EconomyUpdate>) {
        if (items.isEmpty()) return
        try {
            // SÊNIOR DEBUG: Log do payload exato para conferência
            items.forEach { 
                Log.d("EconomyRepo", "📦 Payload HD: ${it.hdNumber}, Edifício: ${it.buildingName}, ID: ${it.id}") 
            }

            // SÊNIOR FIX: Garantia absoluta de UPSERT por ID
            client.postgrest["atualizacao_economias"].upsert(items) {
                onConflict = "id"
            }
            Log.d("EconomyRepo", "✅ Sucesso Supabase: Lote de ${items.size} enviado.")
            fetchEconomyUpdates()
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Erro desconhecido no Supabase"
            Log.e("EconomyRepo", "❌ FALHA NO SUPABASE: $errorMsg")
            // SÊNIOR QA: Verificamos se o erro é de 'Tabela não encontrada' ou 'RLS'
            if (errorMsg.contains("404") || errorMsg.contains("not found")) {
                Log.e("EconomyRepo", "🚨 ATENÇÃO: Verifique se a tabela 'atualizacao_economias' existe no Supabase!")
            }
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
