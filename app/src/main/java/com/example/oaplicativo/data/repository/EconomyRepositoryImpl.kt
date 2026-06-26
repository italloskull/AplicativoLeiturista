package com.example.oaplicativo.data.repository

import android.util.Log
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.domain.repository.EconomyRepository
import com.example.oaplicativo.model.EconomyUpdate
import io.github.jan.supabase.auth.auth
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
                // 1. SÊNIOR FIX: Carregamento LOCAL obrigatório (A base da verdade)
                val localPending = try {
                    val db = context?.let { LocalDatabase.getInstance(it) } ?: throw Exception("Context not ready")
                    val list = db.getPendingEconomyUpdates().map { it.second.copy(isSynced = false) }
                    Log.d("EconomyRepo", "🏠 [LOCAL] Carregados ${list.size} registros pendentes do celular.")
                    list
                } catch (e: Exception) {
                    Log.e("EconomyRepo", "❌ Erro ao acessar banco local: ${e.message}")
                    emptyList()
                }

                // Emite os dados locais imediatamente para a UI não ficar em branco caso a rede demore
                if (localPending.isNotEmpty()) {
                    _items.value = localPending
                }

                // 2. Busca do servidor (registros oficiais) - Tenta em segundo plano
                val remoteList = try {
                    Log.d("EconomyRepo", "🌐 [NETWORK] Buscando Grandes Empreendimentos...")
                    val result = client.postgrest["grandes_empreendimentos"]
                        .select {
                            order("criado_em", order = Order.DESCENDING)
                            limit(100)
                        }.decodeList<EconomyUpdate>()
                    Log.d("EconomyRepo", "✅ [NETWORK] Busca concluída: ${result.size} itens do servidor.")
                    result
                } catch (e: Exception) {
                    Log.w("EconomyRepo", "⚠️ [NETWORK] Modo Offline ativo: ${e.message}")
                    emptyList<EconomyUpdate>()
                }
                
                // 3. Mescla Final: IDs locais têm prioridade absoluta (Nuvem Vermelha)
                val combined = (localPending + remoteList).distinctBy { it.id }
                _items.value = combined
                Log.d("EconomyRepo", "📊 [FINAL] Lista atualizada com ${combined.size} itens (Local + Nuvem).")
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
        try {
            // SÊNIOR DEBUG: Log do payload exato para conferência
            items.forEach { 
                Log.d("EconomyRepo", "📦 [TENTATIVA GE] HD: ${it.hdNumber} | Edifício: ${it.buildingName} | ID: ${it.id}") 
            }

            Log.d("EconomyRepo", "🚀 [SUPABASE] Enviando INSERT para grandes_empreendimentos...")
            
            // SÊNIOR FIX: Apontando para a nova tabela oficial 'grandes_empreendimentos'
            client.postgrest["grandes_empreendimentos"].insert(items)
            
            Log.d("EconomyRepo", "✅ [SUPABASE] Sucesso: Grandes Empreendimentos aceitos.")
            fetchEconomyUpdates()
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Erro desconhecido"
            Log.e("EconomyRepo", "❌ [SUPABASE] FALHA CRÍTICA NO ENVIO: $errorMsg")
            
            // SÊNIOR DIAGNOSTIC: Verifica se o erro é de permissão (RLS)
            if (errorMsg.contains("403") || errorMsg.contains("permission", ignoreCase = true)) {
                Log.e("EconomyRepo", "🚨 ALERTA: Verifique a política de RLS (INSERT) no Supabase!")
            }
            throw e
        }
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
