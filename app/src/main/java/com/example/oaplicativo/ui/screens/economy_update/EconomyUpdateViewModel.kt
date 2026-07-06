package com.example.oaplicativo.ui.screens.economy_update

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.EconomyRepositoryImpl
import com.example.oaplicativo.domain.repository.EconomyRepository
import com.example.oaplicativo.model.EconomyUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.derivedStateOf
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

    private val _authorizedCities = MutableStateFlow<List<com.example.oaplicativo.model.Cidade>>(emptyList())
    val authorizedCities: StateFlow<List<com.example.oaplicativo.model.Cidade>> = _authorizedCities.asStateFlow()

    var selectedCidadeForRegistry by mutableStateOf<com.example.oaplicativo.model.Cidade?>(null)

    private val _selectedCityFilter = MutableStateFlow<com.example.oaplicativo.model.Cidade?>(null)
    val selectedCityFilter: StateFlow<com.example.oaplicativo.model.Cidade?> = _selectedCityFilter.asStateFlow()


    init {
        fetchEconomyUpdates()
        loadAuthorizedCities()
    }

    private fun loadAuthorizedCities() {
        viewModelScope.launch {
            val cities = AuthRepositoryImpl.getInstance().getUserCities()
            _authorizedCities.value = cities
            if (cities.size == 1) {
                selectedCidadeForRegistry = cities.first()
            }
        }
    }

    fun selectCityFilter(cidade: com.example.oaplicativo.model.Cidade?) {
        _selectedCityFilter.value = cidade
    }

    fun calculateEconomyQuality(item: EconomyUpdate): String {
        var score = 0f
        if (!item.hdNumber.isNullOrBlank()) score += 40f
        if (!item.buildingName.isNullOrBlank()) score += 10f
        if (!item.constructionCompany.isNullOrBlank()) score += 10f
        if (item.economiesCount != null && item.economiesCount > 0) score += 10f
        if (item.floorsCount != null && item.floorsCount > 0) score += 10f
        if (item.latitude != null) score += 4f
        if (!item.cidade.isNullOrBlank()) score += 4f
        if (!item.grupoSugerido.isNullOrBlank()) score += 4f
        if (!item.rotaSugerida.isNullOrBlank()) score += 4f
        if (!item.electricityMeterNumber.isNullOrBlank()) score += 4f
        
        return when {
            score >= 70f -> "Boa"
            score >= 40f -> "Regular"
            else -> "Ruim"
        }
    }

    fun fetchEconomyUpdates() {
        viewModelScope.launch {
            val user = AuthRepositoryImpl.getInstance().currentUserProfile.value
            repository.fetchEconomyUpdates(
                cidadeId = user?.cidadeId,
                isAdmin = user?.cargo?.lowercase() == "desenvolvedor"
            )
        }
    }

    fun saveEconomyUpdate(item: EconomyUpdate) {
        viewModelScope.launch {
            _state.value = EconomyUpdateState.Loading
            try {
                val db = LocalDatabase.getInstance(getApplication())
                val authRepo = AuthRepositoryImpl.getInstance()
                val user = authRepo.currentUserProfile.value

                val brDate = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                val finalId = item.id ?: java.util.UUID.randomUUID().toString()
                val sanitizedUserId = if (user?.id?.length == 36) user.id else null

                val selectedCity = selectedCidadeForRegistry
                if (selectedCity == null) {
                    _state.value = EconomyUpdateState.Error("Selecione o município do registro.")
                    return@launch
                }

                // SÊNIOR GEO-FIX: Calcula o grupo e rota baseados na cidade selecionada no formulário
                val suggestedGroup = GeoFencingHelper.findSuggestedGroup(selectedCity.nome, item.latitude, item.longitude)
                val suggestedRoute = GeoFencingHelper.findSuggestedRoute(selectedCity.nome, item.latitude, item.longitude)

                val finalItem = item.copy(
                    id = finalId,
                    leituristaId = sanitizedUserId,
                    cidade = selectedCity.nome,
                    cidadeId = selectedCity.id,
                    grupoSugerido = suggestedGroup,
                    rotaSugerida = suggestedRoute,
                    addedBy = user?.fullName ?: user?.username ?: "Equipe de Campo",
                    date = brDate,
                )

                val buildingQuality = calculateEconomyQuality(finalItem)
                Log.d("debugs", "🟢 [GE_SAVE] Gravando no banco. Cidade: ${selectedCity.nome} | Grupo: $suggestedGroup")

                db.saveEconomyUpdateOffline(finalItem.copy(isSynced = true)) 
                fetchEconomyUpdates()

                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "immediate_sync_economy_${System.currentTimeMillis()}",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )

                _state.value = EconomyUpdateState.Success
            } catch (e: Exception) {
                Log.e("EconomyVM", "❌ FALHA AO SALVAR", e)
                _state.value = EconomyUpdateState.Error(e.message ?: "Erro ao salvar")
            }
        }
    }

    fun getItemById(id: String): EconomyUpdate? {
        return items.value.find { it.id == id }
    }

    fun resetState() {
        _state.value = EconomyUpdateState.Idle
    }

    fun refreshData() {
        fetchEconomyUpdates()
    }

    fun forceSyncAll() {
        viewModelScope.launch {
            try {
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()

                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "manual_sync_all_${System.currentTimeMillis()}",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
                fetchEconomyUpdates()
            } catch (e: Exception) {
                Log.e("EconomyVM", "Falha ao disparar sincronização manual", e)
            }
        }
    }
}
