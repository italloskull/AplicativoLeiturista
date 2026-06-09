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
                
                val finalItem = item.copy(
                    cidadeId = user?.cidadeId,
                    leituristaId = user?.id,
                    addedBy = user?.fullName ?: user?.username ?: "Leiturista",
                    createdAt = utcNow,
                    date = brDate
                )

                // 1. SALVA OFFLINE PRIMEIRO (INDSTRUTÍVEL)
                Log.d("EconomyVM", "💾 Salvando registro offline: ${finalItem.hdNumber}")
                db.saveEconomyUpdateOffline(finalItem)
                
                // 2. ATUALIZA LISTA LOCAL IMEDIATAMENTE (SÊNIOR FIX)
                fetchEconomyUpdates()

                // 3. DISPARA SINCRONIZAÇÃO EM SEGUNDO PLANO
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                Log.d("EconomyVM", "🚀 Disparando SyncWorker via WorkManager...")
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "immediate_sync_economy_${System.currentTimeMillis()}", // Nome ÚNICO para forçar execução
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )

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
}
