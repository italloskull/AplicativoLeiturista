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
    private var localPendingItems = emptyList<EconomyUpdate>()
    private var remoteItems = emptyList<EconomyUpdate>()

    // SÊNIOR FIX: Use WeakReference ou passe o contexto apenas quando necessarily para evitar Memory Leak.
    // Como este é um Singleton, guardar o Context diretamente é perigoso.
    private var applicationContext: android.content.Context? = null

    fun initialize(context: android.content.Context) {
        if (this.applicationContext == null) {
            this.applicationContext = context.applicationContext
            scope.launch { fetchEconomyUpdates(null, true) }
        }
    }

    override suspend fun fetchEconomyUpdates(cidadeId: String?, isAdmin: Boolean) {
        mutex.withLock {
            try {
                // 1. Carregamento Local
                val localPending = try {
                    val db = applicationContext?.let { LocalDatabase.getInstance(it) } ?: throw Exception("Context not ready")
                    db.getPendingEconomyUpdates(cidadeId, isAdmin).map { it.second.copy(isSynced = false) }
                } catch (e: Exception) {
                    Log.e("debugs", "❌ [GE] Erro banco local: ${e.message}")
                    emptyList()
                }

                // 2. Busca do servidor
                val remoteList = try {
                    val userCityName = getFriendlyCityName(cidadeId)
                    val result = client.postgrest["grandes_empreendimentos"]
                        .select {
                            if (!isAdmin && userCityName != null) {
                                filter { eq("cidade", userCityName) }
                            }
                            order("criado_em", order = Order.DESCENDING)
                            limit(100)
                        }.decodeList<EconomyUpdate>()
                    result
                } catch (e: Exception) {
                    val msg = e.message ?: ""
                    val shortError = when {
                        msg.contains("Unable to resolve host") || msg.contains("address associated") -> "Sem Internet (DNS)"
                        msg.contains("timeout") -> "Tempo Esgotado (Lento)"
                        msg.contains("401") || msg.contains("403") -> "Acesso Negado (Token/Key)"
                        else -> "Falha de Conexão"
                    }
                    Log.w("debugs", "⚠️ [GE] Rede: $shortError")
                    emptyList()
                }
                
                // 3. Mescla e Emissão
                remoteItems = remoteList
                localPendingItems = localPending
                combineAndEmit()
            } catch (e: Exception) {
                Log.e("debugs", "❌ [GE] Falha ao processar lista: ${e.message}")
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
        saveEconomyUpdates(listOf(item))
    }

    override suspend fun saveEconomyUpdates(items: List<EconomyUpdate>) {
        if (items.isEmpty()) return
        try {
            Log.d("debugs", "🚀 [GE] Sincronizando com Supabase...")
            // SÊNIOR FIX: Mudança para UPSERT com resolução de conflito por ID.
            // Isso elimina o erro de "duplicate key" e garante que o dado seja atualizado se já existir.
            client.postgrest["grandes_empreendimentos"].upsert(items) {
                onConflict = "id"
            }
            Log.d("debugs", "✅ [GE] Sincronizado com sucesso.")
            fetchEconomyUpdates(null, true)
        } catch (e: Exception) {
            Log.e("debugs", "❌ [GE] Erro no Supabase: ${e.message}")
            throw e
        }
    }

    override fun updateLocalEconomyUpdates(items: List<EconomyUpdate>) {
        localPendingItems = items.map { it.copy(isSynced = false) }
        combineAndEmit()
    }

    override fun clearCache() {
        remoteItems = emptyList()
        localPendingItems = emptyList()
        _items.value = emptyList()
        Log.d("debugs", "🧹 [GE] Cache de memória limpo.")
    }

    private fun combineAndEmit() {
        val combined = (localPendingItems + remoteItems).distinctBy { it.id }
        _items.value = combined
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
