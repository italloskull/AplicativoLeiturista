package com.example.oaplicativo.ui.screens.economy_update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.EconomyRepositoryImpl
import com.example.oaplicativo.domain.repository.EconomyRepository
import com.example.oaplicativo.model.EconomyUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.work.*
import android.util.Log
import com.example.oaplicativo.data.sync.SyncWorker
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.example.oaplicativo.util.GeoFencingHelper

sealed class EconomyUpdateState {
    object Idle : EconomyUpdateState()
    object Loading : EconomyUpdateState()
    object Success : EconomyUpdateState()
    data class Error(val message: String) : EconomyUpdateState()
}

class EconomyUpdateViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: EconomyRepository = EconomyRepositoryImpl.getInstance()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<EconomyUpdateState>(EconomyUpdateState.Idle)
    val state: StateFlow<EconomyUpdateState> = _state.asStateFlow()

    val items: StateFlow<List<EconomyUpdate>> = repository.items

    init {
        // SÊNIOR PERF: O init agora é apenas para carga inicial rápida.
        // O fetch real é feito reativamente pela UI.
        viewModelScope.launch {
            repository.fetchEconomyUpdates()
        }
    }

    fun fetchEconomyUpdates() {
        viewModelScope.launch {
            // SÊNIOR PERF: Buscamos dados do repositório (que agora mescla Local + Supabase)
            repository.fetchEconomyUpdates()
        }
    }

    fun saveEconomyUpdate(item: EconomyUpdate) {
        viewModelScope.launch {
            _state.value = EconomyUpdateState.Loading
            try {
                val db = LocalDatabase.getInstance(getApplication())
                val authRepo = AuthRepositoryImpl.getInstance()
                val user = authRepo.currentUserProfile.value

                // Prepara dados de auditoria
                val utcNow = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val brDate = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                
                // SÊNIOR FIX: ID deve ser UUID para garantir que registros sem HD não colidam no Supabase
                val finalId = item.id ?: java.util.UUID.randomUUID().toString()
                
                // SÊNIOR FIX: Tradução do nome amigável da cidade para o Supabase
                val friendlyCityName = when (user?.cidadeId) {
                    "c2be642b-2823-41b9-8f54-0b8c84db9a14" -> "Itapoá"
                    "ff9166b8-63b1-4481-a26a-64778181fa08" -> "Guabiruba"
                    "74df763a-0120-42b4-bb4d-03cfd92e79b0" -> "Gaivota"
                    "93fee74f-6cbb-4638-868d-ef5c17b081a4" -> "Gravatal"
                    "9ed90b8c-1b63-44b7-88cd-e2b9bd6babcc" -> "Sombrio"
                    else -> "Cidade Não Definida"
                }

                // SÊNIOR FIX: Sanitização de UUIDs para evitar Erro 400 (Bad Request) no Postgres
                val sanitizedUserId = if (user?.id?.length == 36) user.id else null

                val finalItem = item.copy(
                    id = finalId,
                    leituristaId = sanitizedUserId,
                    cidade = friendlyCityName,
                    grupoSugerido = GeoFencingHelper.findSuggestedGroup(friendlyCityName, item.latitude, item.longitude),
                    rotaSugerida = GeoFencingHelper.findSuggestedRoute(friendlyCityName, item.latitude, item.longitude),
                    addedBy = user?.fullName ?: user?.username ?: "Leiturista",
                    createdAt = utcNow,
                    date = brDate
                )

                // 1. SALVA OFFLINE PRIMEIRO (INDSTRUTÍVEL)
                Log.d("EconomyVM", "🟢 [SAVE_START] Preparando envio para SQLite: ${finalItem.buildingName}")
                db.saveEconomyUpdateOffline(finalItem)
                
                // 2. ATUALIZA LISTA LOCAL IMEDIATAMENTE (SÊNIOR FIX)
                Log.d("EconomyVM", "🔄 [PASSO 2] Atualizando lista local...")
                fetchEconomyUpdates()

                // 3. DISPARA SINCRONIZAÇÃO EM SEGUNDO PLANO
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                Log.d("EconomyVM", "🚀 [PASSO 3] Disparando SyncWorker via WorkManager...")
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "immediate_sync_economy_${System.currentTimeMillis()}", // Nome ÚNICO para forçar execução
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )

                Log.d("EconomyVM", "✅ [FIM] Processo de salvamento finalizado com sucesso.")
                _state.value = EconomyUpdateState.Success
            } catch (e: Exception) {
                Log.e("EconomyVM", "❌ FALHA AO SALVAR OFFLINE", e)
                _state.value = EconomyUpdateState.Error(e.message ?: "Erro ao salvar offline")
            }
        }
    }

    fun getItemById(id: String): EconomyUpdate? {
        return items.value.find { it.id == id }
    }

    fun resetState() {
        _state.value = EconomyUpdateState.Idle
    }

    /**
     * SÊNIOR COMMAND: Força o Robô de Sincronização a processar tudo agora.
     */
    fun forceSyncAll() {
        viewModelScope.launch {
            try {
                Log.i("EconomyVM", "🚀 Comando manual: Forçando sincronização global...")
                
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()

                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "manual_sync_all_${System.currentTimeMillis()}",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
                
                // Feedback visual de carregamento rápido
                fetchEconomyUpdates()
            } catch (e: Exception) {
                Log.e("EconomyVM", "Falha ao disparar sincronização manual", e)
            }
        }
    }
}
