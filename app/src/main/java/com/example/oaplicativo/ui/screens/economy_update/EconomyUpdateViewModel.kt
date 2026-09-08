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
import kotlinx.coroutines.Dispatchers
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
    object SaveSuccess : EconomyUpdateState() // SÊNIOR FIX: Estado específico para fechamento de tela
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

    // SÊNIOR SIGNATURE STATE (GE)
    var signatureBitmap by mutableStateOf<android.graphics.Bitmap?>(null)
    var signatureLines by mutableStateOf<List<com.example.oaplicativo.ui.components.LinePath>>(emptyList())
    var localAssinaturaPath by mutableStateOf<String?>(null)
    var assinaturaUrl by mutableStateOf<String?>(null)
    var assinaturaHash by mutableStateOf<String?>(null)
    var assinaturaPayload by mutableStateOf<String?>(null)
    var isDownloadingSignature by mutableStateOf(false)
    var remoteSignatureBitmap by mutableStateOf<android.graphics.Bitmap?>(null)

    fun onSignatureCaptured(bitmap: android.graphics.Bitmap, lines: List<com.example.oaplicativo.ui.components.LinePath> = emptyList()) {
        signatureLines = lines
        signatureBitmap = bitmap
    }

    fun generateLatestStampedSignature(buildingName: String, latitude: Double?, longitude: Double?): android.graphics.Bitmap? {
        if (signatureLines.isEmpty()) return signatureBitmap
        val buildingLabel = buildingName.ifBlank { "GE EMPREENDIMENTO" }
        val userProfile = AuthRepositoryImpl.getInstance().currentUserProfile.value
        val leituristaLabel = userProfile?.fullName ?: userProfile?.username ?: "Equipe de Campo"
        val gpsLabel = if (latitude != null && longitude != null) "$latitude, $longitude" else "GPS PENDENTE"
        val tsBrasilia = com.example.oaplicativo.util.DateUtils.getFullTimestampBrasilia()

        val rawPayload = "${buildingLabel.ifBlank { "NÃO INFORMADO" }}|${leituristaLabel.ifBlank { "EQUIPE DE CAMPO" }}|$tsBrasilia|${gpsLabel.ifBlank { "GPS NÃO CAPTURADO" }}"
        assinaturaPayload = rawPayload

        return com.example.oaplicativo.ui.components.createBitmapFromLinesWithStamp(
            lines = signatureLines,
            entrevistadoInfo = buildingLabel,
            leituristaInfo = leituristaLabel,
            gpsInfo = gpsLabel,
            timestampBrasilia = tsBrasilia
        )
    }

    fun clearSignature() {
        signatureLines = emptyList()
        signatureBitmap = null
        localAssinaturaPath = null
        assinaturaUrl = null
        assinaturaHash = null
        assinaturaPayload = null
        remoteSignatureBitmap = null
    }

    fun loadRemoteSignature() {
        val url = assinaturaUrl ?: return
        if (remoteSignatureBitmap != null || isDownloadingSignature) return
        viewModelScope.launch(Dispatchers.IO) {
            isDownloadingSignature = true
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val downloadedBitmap = android.graphics.BitmapFactory.decodeStream(input)
                remoteSignatureBitmap = downloadedBitmap
            } catch (e: Exception) {
                Log.e("EconomyVM", "Erro ao baixar assinatura remota de GE: ${e.message}")
            } finally {
                isDownloadingSignature = false
            }
        }
    }

    fun loadItemForEdit(itemId: String?) {
        if (itemId == null) return
        val item = getItemById(itemId) ?: return
        assinaturaUrl = item.assinaturaUrl
        assinaturaHash = item.assinaturaHash
        assinaturaPayload = item.assinaturaPayload
        localAssinaturaPath = item.localAssinaturaPath
        val path = localAssinaturaPath
        if (!path.isNullOrBlank()) {
            val file = java.io.File(path)
            if (file.exists()) {
                signatureBitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            }
        }
    }

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
        if (!item.hdNumber.isNullOrBlank()) score += 25f
        if (!item.buildingName.isNullOrBlank()) score += 10f
        if (!item.constructionCompany.isNullOrBlank()) score += 10f
        if (item.economiesCount != null && item.economiesCount > 0) score += 10f
        if (item.floorsCount != null && item.floorsCount > 0) score += 10f
        if (item.blockCount != null && item.blockCount > 0) score += 5f
        if (!item.assinaturaUrl.isNullOrBlank() || !item.localAssinaturaPath.isNullOrBlank() || signatureBitmap != null) score += 15f // SÊNIOR SCORE: Assinatura Digital vale 15 pontos!
        if (item.latitude != null) score += 4f
        if (!item.cidade.isNullOrBlank()) score += 4f
        if (!item.grupoSugerido.isNullOrBlank()) score += 4f
        if (!item.rotaSugerida.isNullOrBlank()) score += 4f
        if (!item.electricityMeterNumber.isNullOrBlank()) score += 4f
        if (!item.possuiPiscina.isNullOrBlank()) score += 2f
        if (!item.tilEsgoto.isNullOrBlank()) score += 2f
        if (!item.setor.isNullOrBlank()) score += 2f
        if (!item.quadra.isNullOrBlank()) score += 2f
        if (!item.lote.isNullOrBlank()) score += 2f
        if (!item.observacao.isNullOrBlank()) score += 3f
        
        return when {
            score >= 70f -> "Boa"
            score >= 40f -> "Regular"
            else -> "Ruim"
        }
    }

    fun fetchEconomyUpdates(forceFull: Boolean = false) {
        viewModelScope.launch {
            _state.value = EconomyUpdateState.Loading
            try {
                val user = AuthRepositoryImpl.getInstance().currentUserProfile.value
                repository.fetchEconomyUpdates(
                    cidadeId = user?.cidadeId,
                    isAdmin = user?.cargo?.lowercase() == "desenvolvedor",
                    forceFull = forceFull
                )
                _state.value = EconomyUpdateState.Success
            } catch (e: Exception) {
                _state.value = EconomyUpdateState.Error(e.message ?: "Erro ao carregar")
            }
        }
    }

    fun saveEconomyUpdate(item: EconomyUpdate) {
        viewModelScope.launch {
            _state.value = EconomyUpdateState.Loading
            try {
                val db = LocalDatabase.getInstance(getApplication())
                val authRepo = AuthRepositoryImpl.getInstance()
                val user = authRepo.currentUserProfile.value

                val brDate = com.example.oaplicativo.util.DateUtils.getTodayBrasilia()
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

                // SÊNIOR SIGNATURE FIX: Regenera a assinatura do GE e calcula o Hash SHA-256 do ARQUIVO PNG FÍSICO
                val finalStampedSig = generateLatestStampedSignature(item.buildingName ?: "", item.latitude, item.longitude)
                if (finalStampedSig != null && signatureLines.isNotEmpty()) {
                    try {
                        val sigDir = java.io.File(getApplication<Application>().filesDir, "signatures").apply { if (!exists()) mkdirs() }
                        val file = java.io.File(sigDir, "sig_ge_${java.util.UUID.randomUUID()}.png")
                        val pngBytes = com.example.oaplicativo.ui.components.bitmapToPngBytes(finalStampedSig)
                        file.writeBytes(pngBytes)

                        localAssinaturaPath = file.absolutePath
                        signatureBitmap = finalStampedSig
                        assinaturaHash = com.example.oaplicativo.ui.components.computePngBytesSha256(pngBytes)
                        Log.d("EconomyVM", "🔒 Hash SHA-256 do arquivo PNG físico de GE: $assinaturaHash")
                    } catch (e: Exception) {
                        Log.e("EconomyVM", "Erro ao salvar PNG final de GE: ${e.message}")
                    }
                }

                val finalItem = item.copy(
                    id = finalId,
                    leituristaId = sanitizedUserId,
                    cidade = selectedCity.nome,
                    cidadeId = selectedCity.id,
                    grupoSugerido = suggestedGroup,
                    rotaSugerida = suggestedRoute,
                    addedBy = user?.fullName ?: user?.username ?: "Equipe de Campo",
                    date = brDate,
                    assinaturaUrl = assinaturaUrl,
                    assinaturaHash = assinaturaHash,
                    assinaturaPayload = assinaturaPayload,
                    localAssinaturaPath = localAssinaturaPath
                )

                val buildingQuality = calculateEconomyQuality(finalItem)
                Log.d("debugs", "🟢 [GE_SAVE] Gravando no banco. Cidade: ${selectedCity.nome} | Grupo: $suggestedGroup")

                db.saveEconomyUpdateOffline(finalItem.copy(isSynced = true)) 
                
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "immediate_sync_economy_${System.currentTimeMillis()}",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )

                _state.value = EconomyUpdateState.SaveSuccess // SÊNIOR FIX: Dispara o fechamento apenas aqui
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
        fetchEconomyUpdates(forceFull = true)
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
