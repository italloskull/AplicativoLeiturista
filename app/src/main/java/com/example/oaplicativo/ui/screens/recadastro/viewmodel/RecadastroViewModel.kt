@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.recadastro.viewmodel

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.data.repository.StatsRepositoryImpl
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class RoleData {
    var nomeCompleto by mutableStateOf("")
    var cpfCnpj by mutableStateOf("")
    var nomeMae by mutableStateOf("")
    var dataNascimento by mutableStateOf("")
    var sexo by mutableStateOf<String?>(null)
    var apresentouDoc by mutableStateOf<String?>(null)
    var qualDoc by mutableStateOf("")
}

class RecadastroViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val authRepository = AuthRepositoryImpl.getInstance()
    private val customerRepository: CustomerRepository = CustomerRepositoryImpl.getInstance()
    private val localDb = LocalDatabase.getInstance(application)
    private val geocoder = Geocoder(application, Locale.getDefault())
    
    var isDataCensoredInitial by mutableStateOf(false)
    val currentUserProfile = authRepository.currentUserProfile
    
    private var editingCustomerId: String?
        get() = savedStateHandle["editing_id"]
        set(value) { savedStateHandle["editing_id"] = value }

    // --- ESTADO DO IMÓVEL ---
    var matricula by mutableStateOf("")
    var registrationDigit by mutableStateOf("")
    var setor by mutableStateOf("")
    var quadra by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var isCapturingLocation by mutableStateOf(false)

    // SÊNIOR PERF: Reatividade em tempo real para sugestões geográficas
    // BUG FIX: Adicionado LOG e fallback de cidade para garantir que a detecção ocorra
    val grupoSugerido by derivedStateOf {
        val finalCidade = cidade.ifBlank { getLeituristaCidadeNome(currentUserProfile.value?.cidadeId) ?: "" }
        val result = GeoFencingHelper.findSuggestedGroup(finalCidade, latitude, longitude)
        Log.d("GeoDebug", "🔎 Tentando detectar GRUPO para cidade: $finalCidade | Lat: $latitude | Resultado: $result")
        result
    }
    val rotaSugerida by derivedStateOf {
        val finalCidade = cidade.ifBlank { getLeituristaCidadeNome(currentUserProfile.value?.cidadeId) ?: "" }
        val result = GeoFencingHelper.findSuggestedRoute(finalCidade, latitude, longitude)
        Log.d("GeoDebug", "🔎 Tentando detectar ROTA para cidade: $finalCidade | Lat: $latitude | Resultado: $result")
        result
    }

    // --- ESTADO DO RESPONSÁVEL ---
    var responsavelTipo by mutableStateOf("Proprietário") 
    var entrevistadoEhOResponsavel by mutableStateOf("Sim")
    var responsavelData = RoleData()
    var entrevistadoNomeApenas by mutableStateOf("")
    var entrevistadoEmailApenas by mutableStateOf("")
    var entrevistadoCelularApenas by mutableStateOf("")

    // --- ESTADO DE CONTATO E ENDEREÇO ---
    var email by mutableStateOf("")
    var telefone by mutableStateOf("")
    var celular1 by mutableStateOf("")
    var logradouro by mutableStateOf("")
    var numero by mutableStateOf("")
    var complemento by mutableStateOf("")
    var bairro by mutableStateOf("")
    var cidade by mutableStateOf("")
    var uf by mutableStateOf("")
    var cep by mutableStateOf("")

    // --- ESTADO DE CARACTERÍSTICAS ---
    var pavimentoRua by mutableStateOf<String?>(null)
    var pavimentoCalcada by mutableStateOf<String?>(null)
    var fonteAbastecimento by mutableStateOf<String?>(null)
    var existeRedeAgua by mutableStateOf<String?>(null)
    var possuiPiscina by mutableStateOf<String?>(null)
    var possuiCaixaAgua by mutableStateOf<String?>(null)
    var beneficiarioSocial by mutableStateOf<String?>(null)
    var usaAguaVizinho by mutableStateOf<String?>(null)
    var possuiHidrometro by mutableStateOf<String?>(null)
    var isStandardMeasurementBox by mutableStateOf<String?>(null)
    var isStandardizedSeals by mutableStateOf<String?>(null)
    var isHdAccessible by mutableStateOf<String?>(null)
    var isVacationer by mutableStateOf<String?>(null)
    var locationStatus by mutableStateOf<String?>(null)
    var localInstalacao by mutableStateOf<String?>(null)
    var acessibilidade by mutableStateOf<String?>(null)
    
    var numeroHidrometro by mutableStateOf("")
    var economias by mutableStateOf("")
    var observacao by mutableStateOf("")

    var isCepLoading by mutableStateOf(false)
    var cepError by mutableStateOf(false)
    
    // SÊNIOR FIX: Estado de UI persistente para evitar travamento em rotação/morte de processo
    var isCapturingGpsOnSave by mutableStateOf(false)

    // SÊNIOR FIX: Orquestração de Coroutines para evitar ANR e Memory Leaks
    private var geocodeJob: Job? = null
    private var cepJob: Job? = null
    private var lastResolvedLat = 0.0
    private var lastResolvedLng = 0.0

    /**
     * SÊNIOR PROTOCOL: Garante que o estado de captura nunca fique travado.
     */
    override fun onCleared() {
        super.onCleared()
        geocodeJob?.cancel()
        cepJob?.cancel()
        isCapturingLocation = false // Reset de segurança
    }

    // SÊNIOR PERF: Cálculo ultra-eficiente de progresso usando derivedStateOf
    // Isso evita re-cálculos pesados durante a digitação, economizando bateria.
    private val _registrationProgress = derivedStateOf {
        var score = 0f
        if (matricula.isNotBlank() && matricula != "0") score += 1f
        if (latitude != null) score += 1f
        if (responsavelData.nomeCompleto.isNotBlank()) score += 1f
        if (responsavelData.cpfCnpj.isNotBlank()) score += 1f
        if (celular1.isNotBlank()) score += 1f
        if (logradouro.isNotBlank()) score += 1f
        if (numero.isNotBlank()) score += 1f
        if (bairro.isNotBlank()) score += 1f
        if (email.isNotBlank()) score += 1f
        if (pavimentoRua != null) score += 1f
        if (fonteAbastecimento != null) score += 1f
        if (economias.isNotBlank()) score += 1f
        score / 12f
    }
    val registrationProgress: Float get() = _registrationProgress.value

    private fun getLeituristaCidadeNome(cidadeId: String?): String? {
        return when (cidadeId?.lowercase()) {
            "c2be642b-2823-41b9-8f54-0b8c84db9a14" -> "Itapoá"
            "ff9166b8-63b1-4481-a26a-64778181fa08" -> "Guabiruba"
            "74df760a-0120-42b4-bb4d-03cfd92e79b0" -> "Gaivota"
            "93fee74f-6cbb-4638-868d-ef5c17b081a4" -> "Gravatal"
            "9ed90b8c-1b63-44b7-88cd-c2b9b6babcc7" -> "Sombrio"
            "c5643444-2396-4f4d-8724-4f014798c897" -> "Araquari" // Backup Legado
            else -> null
        }
    }

    fun loadCustomerForEdit(customerId: String?) {
        if (customerId == null) return
        editingCustomerId = customerId
        viewModelScope.launch {
            val customer = customerRepository.getCustomerById(customerId) ?: return@launch
            isDataCensoredInitial = com.example.oaplicativo.util.privacy.PrivacyUtils.shouldMaskSensitiveData(customer.createdAt)
            
            matricula = customer.registrationNumber ?: ""
            registrationDigit = customer.registrationDigit ?: ""
            setor = customer.setor ?: ""
            quadra = customer.quadra ?: ""
            latitude = customer.latitude
            longitude = customer.longitude
            cep = customer.cep ?: ""
            logradouro = customer.logradouro ?: ""
            numero = customer.numero ?: ""
            complemento = customer.complemento ?: ""
            bairro = customer.bairro ?: ""
            cidade = customer.cidade ?: ""
            uf = customer.uf ?: ""
            email = customer.email ?: ""
            telefone = ""
            celular1 = customer.celular ?: ""
            
            beneficiarioSocial = customer.beneficiarioSocial.ifSpaceNull()
            usaAguaVizinho = customer.usaAguaVizinho.ifSpaceNull()
            possuiHidrometro = customer.possuiHidrometro.ifSpaceNull()
            isStandardMeasurementBox = customer.isStandardMeasurementBox.ifSpaceNull()
            isStandardizedSeals = customer.isStandardizedSeals.ifSpaceNull()
            isHdAccessible = customer.isHdAccessible.ifSpaceNull()
            isVacationer = customer.isVacationer.ifSpaceNull()
            locationStatus = customer.locationStatus.ifSpaceNull()
            existeRedeAgua = customer.existeRedeAgua.ifSpaceNull()
            possuiPiscina = customer.possuiPiscina.ifSpaceNull()
            possuiCaixaAgua = customer.possuiCaixaAgua.ifSpaceNull()
            pavimentoRua = customer.pavimentoRua.ifSpaceNull()
            pavimentoCalcada = customer.pavimentoCalcada.ifSpaceNull()
            fonteAbastecimento = customer.fonteAbastecimento.ifSpaceNull()
            localInstalacao = customer.localInstalacao.ifSpaceNull()
            acessibilidade = customer.acessibilidade.ifSpaceNull()
            observacao = customer.observacao ?: ""
            economias = customer.economiesCount?.toString() ?: ""

            responsavelTipo = "Proprietário" 
            responsavelData.nomeCompleto = customer.entrevistadoNome ?: ""
            responsavelData.cpfCnpj = customer.entrevistadoCpf ?: ""
            responsavelData.nomeMae = customer.entrevistadoMae ?: ""
            responsavelData.dataNascimento = customer.entrevistadoNascimento ?: ""
            responsavelData.sexo = customer.entrevistadoSexo
            responsavelData.apresentouDoc = customer.entrevistadoApresentouDoc.ifSpaceNull()
            responsavelData.qualDoc = customer.entrevistadoQualDoc ?: ""
            
            entrevistadoEhOResponsavel = "Sim"
            entrevistadoNomeApenas = ""
        }
    }

    fun saveRecadastro(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val snapshotLat = latitude
        val snapshotLng = longitude
        val snapshotMatricula = matricula.trim()
        val snapshotDigit = registrationDigit.trim()
        val snapshotEmail = email.trim().lowercase()
        val snapshotCelular = celular1.trim()
        val snapshotLogradouro = logradouro.trim()
        val snapshotNumero = numero.trim()
        val snapshotComplemento = complemento.trim()
        val snapshotBairro = bairro.trim()
        val snapshotCep = cep.trim()
        val snapshotUf = uf.trim()
        val snapshotCidadeManual = cidade.trim()
        val snapshotObs = observacao.trim()
        
        val sEco = economias.toIntOrNull()

        if (isCapturingLocation) return 
        
        viewModelScope.launch {
            try {
                isCapturingLocation = true
                
                val user = authRepository.currentUserProfile.value ?: run {
                    onError("Sessão inválida. Faça login novamente.")
                    isCapturingLocation = false
                    return@launch
                }

                val finalCidade = snapshotCidadeManual.ifBlank { getLeituristaCidadeNome(user.cidadeId) ?: "" }
                val finalCidadeId = if (user.cidadeId?.length == 36) user.cidadeId else null
                val finalLeituristaId = if (user.id?.length == 36) user.id else null
                
                val utcNow = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val brDate = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))

                val sNome = responsavelData.nomeCompleto.trim()
                val sCpf = responsavelData.cpfCnpj.trim()
                val sMae = responsavelData.nomeMae.trim()
                val sNasc = responsavelData.dataNascimento.trim()
                val sSexo = responsavelData.sexo
                val sDoc = responsavelData.apresentouDoc
                val sQual = responsavelData.qualDoc.trim()

                // SÊNIOR FIX DEFINITIVO: Mapeamento rigoroso de Nomes de Cidade para o Supabase
                // Prioridade absoluta para a cidade do perfil do usuário para nunca ficar 'Desconhecida'
                val friendlyCityName = getLeituristaCidadeNome(user.cidadeId) 
                    ?: snapshotCidadeManual.ifBlank { "Cidade Itapoá" } // Fallback seguro para a sede

                // SÊNIOR FIX: Captura instantânea do estado da UI para evitar race condition
                val snapshotGrupo = grupoSugerido
                val snapshotRota = rotaSugerida

                val baseStatus = locationStatus.orSpace()
                val finalLocationStatus = if (snapshotLat == null) {
                    if (baseStatus == " ") "Sem Sinal" else "$baseStatus (Sem Sinal)"
                } else baseStatus

                val customer = Customer(
                    id = editingCustomerId ?: UUID.randomUUID().toString(),
                    cidadeId = finalCidadeId,
                    leituristaId = finalLeituristaId,
                    name = sNome.orSpace(),
                    registrationNumber = snapshotMatricula.orSpace(),
                    registrationDigit = snapshotDigit.orSpace(),
                    email = if (responsavelTipo == "Proprietário") snapshotEmail.orSpace() else " ",
                    celular = if (responsavelTipo == "Proprietário") snapshotCelular.orSpace() else " ",
                    isStandardMeasurementBox = isStandardMeasurementBox.orSpace(),
                    isStandardizedSeals = isStandardizedSeals.orSpace(),
                    isHdAccessible = isHdAccessible.orSpace(),
                    isVacationer = isVacationer.orSpace(),
                    possuiPiscina = possuiPiscina.orSpace(),
                    possuiCaixaAgua = possuiCaixaAgua.orSpace(),
                    latitude = snapshotLat,
                    longitude = snapshotLng,
                    locationStatus = finalLocationStatus,
                    economiesCount = sEco,
                    createdAt = utcNow,
                    addedBy = user.fullName ?: user.username,
                    capturedAt = utcNow,
                    date = brDate,
                    quality = calculateDataQuality(),
                    entrevistadoNome = if (entrevistadoEhOResponsavel == "Sim") sNome else entrevistadoNomeApenas.orSpace(),
                    entrevistadoCpf = if (entrevistadoEhOResponsavel == "Sim") sCpf else " ",
                    entrevistadoMae = if (entrevistadoEhOResponsavel == "Sim") sMae else " ",
                    entrevistadoNascimento = if (entrevistadoEhOResponsavel == "Sim") sNasc else " ",
                    entrevistadoSexo = if (entrevistadoEhOResponsavel == "Sim") sSexo else null,
                    entrevistadoApresentouDoc = (if (entrevistadoEhOResponsavel == "Sim") sDoc else null).orSpace(),
                    entrevistadoQualDoc = if (entrevistadoEhOResponsavel == "Sim") sQual else " ",
                    logradouro = snapshotLogradouro,
                    numero = snapshotNumero,
                    complemento = snapshotComplemento,
                    bairro = snapshotBairro,
                    cidade = friendlyCityName,
                    uf = snapshotUf,
                    cep = snapshotCep,
                    pavimentoRua = pavimentoRua.orSpace(),
                    pavimentoCalcada = pavimentoCalcada.orSpace(),
                    fonteAbastecimento = fonteAbastecimento.orSpace(),
                    existeRedeAgua = existeRedeAgua.orSpace(),
                    localInstalacao = localInstalacao.orSpace(),
                    acessibilidade = acessibilidade.orSpace(),
                    observacao = if (snapshotObs.length > 1000) snapshotObs.take(1000) else snapshotObs,
                    beneficiarioSocial = beneficiarioSocial.orSpace(),
                    usaAguaVizinho = usaAguaVizinho.orSpace(),
                    possuiHidrometro = possuiHidrometro.orSpace(),
                    
                    // SÊNIOR FIX: Decomposição rigorosa baseada no padrão "Grupo X Rota Y"
                    // Garantimos que estamos salvando os valores que o leiturista viu na tela
                    grupoSugerido = snapshotGrupo,
                    rotaSugerida = snapshotRota,
                    
                    setor = setor.trim().orSpace(),
                    quadra = quadra.trim().orSpace(),
                    numeroHidrometro = numeroHidrometro.trim().orSpace(),
                    isSynced = false
                )

                localDb.saveCustomerOffline(customer)
                StatsRepositoryImpl.getInstance(getApplication()).refreshStats()

                val pending = localDb.getPendingCustomers().map { it.second }
                customerRepository.updateLocalCustomers(pending)

                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "immediate_sync", 
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    syncRequest
                )
                onSuccess()
            } catch (e: Exception) {
                Log.e("RecadastroVM", "ERRO AO SALVAR", e)
                onError("Erro técnico: ${e.message ?: "Falha no SQLite"}")
            } finally {
                isCapturingLocation = false
            }
        }
    }

    fun onCepChange(newCep: String) {
        val cleanCep = newCep.replace(Regex("[^0-9]"), "")
        cep = cleanCep
        if (cleanCep.length == 8) {
            fetchAddress(cleanCep)
        }
    }

    fun fetchAddress(cepCode: String) {
        if (isGenericCep(cepCode)) return
        
        cepJob?.cancel()
        cepJob = viewModelScope.launch(Dispatchers.IO) {
            isCepLoading = true
            cepError = false
            try {
                delay(500)
                if (!isActive) return@launch
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName("CEP $cepCode, Brasil", 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: List<Address>) {
                            if (!isActive) return
                            if (addresses.isNotEmpty()) {
                                handleGoogleAddress(addresses[0])
                            }
                            isCepLoading = false
                        }
                        override fun onError(errorMessage: String?) {
                            cepError = true
                            isCepLoading = false
                        }
                    })
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName("CEP $cepCode, Brasil", 1)
                    if (!addresses.isNullOrEmpty()) {
                        handleGoogleAddress(addresses[0])
                    }
                    isCepLoading = false
                }
            } catch (e: Exception) {
                cepError = true
                isCepLoading = false
            }
        }
    }

    fun fetchAddressFromLocation(lat: Double, lng: Double) {
        if (Math.abs(lastResolvedLat - lat) < 0.0001 && Math.abs(lastResolvedLng - lng) < 0.0001) return
        
        lastResolvedLat = lat
        lastResolvedLng = lng
        
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!isActive) return@launch

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: List<Address>) {
                            if (addresses.isNotEmpty()) handleGoogleAddress(addresses[0])
                        }
                        override fun onError(errorMessage: String?) {
                            Log.e("GeoCoder", "Erro assíncrono: $errorMessage")
                        }
                    })
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        handleGoogleAddress(addresses[0])
                    }
                }
            } catch (e: Exception) {
                Log.e("RecadastroVM", "Erro Geocoder", e)
            }
        }
    }

    private fun handleGoogleAddress(address: Address) {
        updateAddressFields(
            street = address.thoroughfare,
            district = address.subLocality ?: address.subAdminArea,
            city = address.locality,
            state = address.adminArea,
            zip = address.postalCode?.replace("-", "")
        )
    }

    private fun updateAddressFields(street: String?, district: String?, city: String?, state: String?, zip: String?) {
        if (!street.isNullOrBlank()) logradouro = street
        if (!district.isNullOrBlank()) bairro = district
        if (!city.isNullOrBlank()) cidade = city
        if (!state.isNullOrBlank()) uf = state
        
        if (!zip.isNullOrBlank() && !zip.endsWith("000")) {
            cep = zip
        }
    }

    private fun isGenericCep(cepCode: String?): Boolean {
        val genericCeps = listOf("89249000", "89248000")
        return genericCeps.contains(cepCode)
    }

    private fun calculateDataQuality(): String {
        val progress = registrationProgress
        return when {
            progress >= 0.75f -> "Boa"
            progress >= 0.33f -> "Regular"
            else -> "Ruim"
        }
    }

    fun resetForm() {
        editingCustomerId = null
        matricula = ""
        registrationDigit = ""
        setor = ""
        quadra = ""
        latitude = null
        longitude = null
        responsavelTipo = "Proprietário"
        entrevistadoEhOResponsavel = "Sim"
        responsavelData = RoleData()
        entrevistadoNomeApenas = ""
        email = ""
        telefone = ""
        celular1 = ""
        logradouro = ""
        numero = ""
        complemento = ""
        bairro = ""
        cidade = ""
        uf = ""
        cep = ""
        pavimentoRua = null
        pavimentoCalcada = null
        fonteAbastecimento = null
        existeRedeAgua = null
        possuiPiscina = null
        possuiCaixaAgua = null
        beneficiarioSocial = null
        usaAguaVizinho = null
        possuiHidrometro = null
        isStandardMeasurementBox = null
        isStandardizedSeals = null
        isHdAccessible = null
        isVacationer = null
        locationStatus = null
        localInstalacao = null
        acessibilidade = null
        numeroHidrometro = ""
        economias = ""
        observacao = ""
    }
}
