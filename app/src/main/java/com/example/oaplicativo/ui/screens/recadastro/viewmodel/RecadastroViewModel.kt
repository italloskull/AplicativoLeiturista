package com.example.oaplicativo.ui.screens.recadastro.viewmodel

import android.app.Application
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.model.Cidade
import com.example.oaplicativo.util.ifSpaceNull
import com.example.oaplicativo.util.orSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    application: Application
) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepositoryImpl.getInstance()
    private val customerRepository: CustomerRepository = CustomerRepositoryImpl.getInstance()
    private val geocoder = Geocoder(application, Locale.getDefault())

    var isDataCensoredInitial by mutableStateOf(false)
    val currentUserProfile: StateFlow<UserProfile?> = authRepository.currentUserProfile

    var editingCustomerId: String? = null
        private set

    var matricula by mutableStateOf("")
    var registrationDigit by mutableStateOf("")
    var setor by mutableStateOf("")
    var quadra by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    
    val grupoSugerido: String?
        get() = com.example.oaplicativo.util.GeoFencingHelper.findSuggestedGroup(selectedCidadeForRegistry?.nome, latitude, longitude)
    
    val rotaSugerida: String?
        get() = com.example.oaplicativo.util.GeoFencingHelper.findSuggestedRoute(selectedCidadeForRegistry?.nome, latitude, longitude)

    var isCapturingLocation by mutableStateOf(false)

    var responsavelTipo by mutableStateOf("Proprietário")
    var entrevistadoEhOResponsavel by mutableStateOf("Sim")
    val responsavelData = RoleData()
    var entrevistadoNomeApenas by mutableStateOf("")
    var entrevistadoEmailApenas by mutableStateOf("")
    var entrevistadoCelularApenas by mutableStateOf("")

    var email by mutableStateOf("")
    var celular1 by mutableStateOf("")
    var logradouro by mutableStateOf("")
    var numero by mutableStateOf("")
    var complemento by mutableStateOf("")
    var bairro by mutableStateOf("")
    var cidade by mutableStateOf("")
    var uf by mutableStateOf("")
    var cep by mutableStateOf("")

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
    var tilEsgoto by mutableStateOf<String?>(null)
    var locationStatus by mutableStateOf<String?>(null)
    var localInstalacao by mutableStateOf<String?>(null)
    var acessibilidade by mutableStateOf<String?>(null)

    var numeroHidrometro by mutableStateOf("")
    var electricityMeter by mutableStateOf("")
    var economias by mutableStateOf("")
    var observacao by mutableStateOf("")

    var isCepLoading by mutableStateOf(false)
    var cepError by mutableStateOf(false)
    var isCapturingGpsOnSave by mutableStateOf(false)

    private var geocodeJob: Job? = null
    private var cepJob: Job? = null

    private val _authorizedCities = MutableStateFlow<List<Cidade>>(emptyList())
    val authorizedCities: StateFlow<List<Cidade>> = _authorizedCities.asStateFlow()
    var selectedCidadeForRegistry by mutableStateOf<Cidade?>(null)

    init {
        loadAuthorizedCities()
    }

    private fun loadAuthorizedCities() {
        viewModelScope.launch {
            val cities = authRepository.getUserCities()
            _authorizedCities.value = cities
            if (cities.size == 1) {
                selectedCidadeForRegistry = cities.first()
            }
        }
    }

    private val _registrationProgress = derivedStateOf {
        var score = 0f
        val criticalWeight = 10f
        val normalWeight = 2f
        val totalWeight = 136f

        // 1. CAMPOS CRÍTICOS (Peso 8 cada)
        if (matricula.isNotBlank()) score += criticalWeight
        if (numeroHidrometro.isNotBlank() && numeroHidrometro != " ") score += criticalWeight
        
        // E-mail e Celular (Conta do Responsável ou do Entrevistado dependendo do contexto)
        if (entrevistadoEhOResponsavel == "Sim") {
            if (email.isNotBlank()) score += criticalWeight
            if (celular1.isNotBlank()) score += criticalWeight
        } else {
            if (entrevistadoEmailApenas.isNotBlank()) score += criticalWeight
            if (entrevistadoCelularApenas.isNotBlank()) score += criticalWeight
        }

        // 2. DEMAIS CAMPOS (Peso 2 cada)
        
        // Identificação (4)
        if (registrationDigit.isNotBlank()) score += normalWeight
        if (setor.isNotBlank()) score += normalWeight
        if (quadra.isNotBlank()) score += normalWeight
        if (selectedCidadeForRegistry != null) score += normalWeight

        // Localização (1)
        if (latitude != null && longitude != null) score += normalWeight

        // Endereço (6)
        if (cep.isNotBlank()) score += normalWeight
        if (logradouro.isNotBlank()) score += normalWeight
        if (numero.isNotBlank()) score += normalWeight
        if (complemento.isNotBlank()) score += normalWeight
        if (bairro.isNotBlank()) score += normalWeight
        if (uf.isNotBlank()) score += normalWeight

        // Dados do Responsável (7)
        if (responsavelData.nomeCompleto.isNotBlank()) score += normalWeight
        if (responsavelData.cpfCnpj.isNotBlank()) score += normalWeight
        if (responsavelData.nomeMae.isNotBlank()) score += normalWeight
        if (responsavelData.dataNascimento.isNotBlank()) score += normalWeight
        if (responsavelData.sexo != null) score += normalWeight
        if (responsavelData.apresentouDoc != null) score += normalWeight
        if (responsavelData.apresentouDoc == "Sim") {
            if (responsavelData.qualDoc.isNotBlank()) score += normalWeight
        } else {
            // Se não apresentou, o ponto de "Qual Doc" é concedido por não ser aplicável? 
            // Seguindo a lógica de peso fixo (124), se não é aplicável, não pontua.
        }

        // Dados do Entrevistado (1)
        if (entrevistadoEhOResponsavel == "Não") {
            if (entrevistadoNomeApenas.isNotBlank()) score += normalWeight
        }

        // Características Físicas e Sanitárias (17)
        if (pavimentoRua != null) score += normalWeight
        if (pavimentoCalcada != null) score += normalWeight
        if (fonteAbastecimento != null) score += normalWeight
        if (existeRedeAgua != null) score += normalWeight
        if (possuiPiscina != null) score += normalWeight
        if (possuiCaixaAgua != null) score += normalWeight
        if (beneficiarioSocial != null) score += normalWeight
        if (usaAguaVizinho != null) score += normalWeight
        if (possuiHidrometro != null) score += normalWeight
        if (isStandardMeasurementBox != null) score += normalWeight
        if (isStandardizedSeals != null) score += normalWeight
        if (isHdAccessible != null) score += normalWeight
        if (isVacationer != null) score += normalWeight
        if (tilEsgoto != null) score += normalWeight
        if (locationStatus != null) score += normalWeight
        if (localInstalacao != null) score += normalWeight
        if (acessibilidade != null) score += normalWeight

        // Medidores (1)
        if (electricityMeter.isNotBlank()) score += normalWeight

        // Outros (1)
        if (economias.isNotBlank()) score += normalWeight

        score / totalWeight
    }
    val registrationProgress: Float get() = _registrationProgress.value

    fun loadCustomerForEdit(customerId: String?) {
        if (customerId == null) return
        editingCustomerId = customerId
        viewModelScope.launch {
            val customer = customerRepository.getCustomerById(customerId) ?: return@launch
            isDataCensoredInitial = com.example.oaplicativo.util.privacy.PrivacyUtils.shouldMaskSensitiveData(customer.capturedAt)
            
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
            celular1 = customer.celular ?: ""
            
            beneficiarioSocial = customer.beneficiarioSocial.ifSpaceNull()
            usaAguaVizinho = customer.usaAguaVizinho.ifSpaceNull()
            possuiHidrometro = customer.possuiHidrometro.ifSpaceNull()
            isStandardMeasurementBox = customer.isStandardMeasurementBox.ifSpaceNull()
            isStandardizedSeals = customer.isStandardizedSeals.ifSpaceNull()
            isHdAccessible = customer.isHdAccessible.ifSpaceNull()
            isVacationer = customer.isVacationer.ifSpaceNull()
            tilEsgoto = customer.tilEsgoto.ifSpaceNull()
            locationStatus = customer.locationStatus.ifSpaceNull()
            existeRedeAgua = customer.existeRedeAgua.ifSpaceNull()
            possuiPiscina = customer.possuiPiscina.ifSpaceNull()
            possuiCaixaAgua = customer.possuiCaixaAgua.ifSpaceNull()
            pavimentoRua = customer.pavimentoRua.ifSpaceNull()
            pavimentoCalcada = customer.pavimentoCalcada.ifSpaceNull()
            fonteAbastecimento = customer.fonteAbastecimento.ifSpaceNull()
            localInstalacao = customer.localInstalacao.ifSpaceNull()
            acessibilidade = customer.accessibilityReading.ifSpaceNull()
            observacao = customer.observacao ?: ""
            economias = customer.economiesCount?.toString() ?: ""
            cidade = customer.cidade ?: ""

            responsavelTipo = "Proprietário" 
            responsavelData.nomeCompleto = customer.name ?: ""
            responsavelData.cpfCnpj = customer.entrevistadoCpf ?: ""
            responsavelData.nomeMae = customer.entrevistadoMae ?: ""
            responsavelData.dataNascimento = customer.entrevistadoNascimento ?: ""
            responsavelData.sexo = customer.entrevistadoSexo
            responsavelData.apresentouDoc = customer.entrevistadoApresentouDoc ?: "Não"
            responsavelData.qualDoc = customer.entrevistadoQualDoc ?: ""
            
            numeroHidrometro = customer.numeroHidrometro ?: ""
            electricityMeter = customer.electricityMeter ?: ""
            
            // SÊNIOR FIX: Restaura o status do entrevistado de forma inteligente
            val savedEntrevistadoNome = customer.entrevistadoNome?.trim()
            val savedResponsavelNome = customer.name?.trim()
            
            if (!savedEntrevistadoNome.isNullOrBlank() && savedEntrevistadoNome != savedResponsavelNome) {
                entrevistadoEhOResponsavel = "Não"
                entrevistadoNomeApenas = savedEntrevistadoNome
                entrevistadoEmailApenas = customer.entrevistadoEmail ?: ""
                entrevistadoCelularApenas = customer.entrevistadoCelular ?: ""
            } else {
                entrevistadoEhOResponsavel = "Sim"
                entrevistadoNomeApenas = ""
                entrevistadoEmailApenas = ""
                entrevistadoCelularApenas = ""
            }
            
            _authorizedCities.value.find { it.nome == customer.cidade }?.let {
                selectedCidadeForRegistry = it
            }
        }
    }

    fun saveRecadastro(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = currentUserProfile.value ?: return@launch
                val selectedCity = selectedCidadeForRegistry ?: run {
                    onError("Por favor, selecione a cidade do registro no topo da tela.")
                    return@launch
                }

                val brNow = ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                val brDate = brNow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                val brFullTimestamp = brNow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd_HH:mm:ss"))

                // SÊNIOR DATA FIX: Formata a data de nascimento bruta
                val sNasc = responsavelData.dataNascimento.trim().let { 
                    if (it.length == 8) "${it.substring(0, 2)}/${it.substring(2, 4)}/${it.substring(4)}" else it 
                }

                val customer = Customer(
                    id = editingCustomerId ?: UUID.randomUUID().toString(),
                    cidadeId = selectedCity.id,
                    leituristaId = user.id,
                    name = responsavelData.nomeCompleto.trim().orSpace(),
                    registrationNumber = matricula.orSpace(),
                    registrationDigit = registrationDigit.orSpace(),
                    email = if (responsavelTipo == "Proprietário") email.orSpace() else " ",
                    celular = if (responsavelTipo == "Proprietário") celular1.orSpace() else " ",
                    isStandardMeasurementBox = isStandardMeasurementBox.orSpace(),
                    isStandardizedSeals = isStandardizedSeals.orSpace(),
                    isHdAccessible = isHdAccessible.orSpace(),
                    isVacationer = isVacationer.orSpace(),
                    possuiPiscina = possuiPiscina.orSpace(),
                    possuiCaixaAgua = possuiCaixaAgua.orSpace(),
                    latitude = latitude,
                    longitude = longitude,
                    locationStatus = locationStatus.orSpace(),
                    economiesCount = economias.toIntOrNull(),
                    addedBy = user.fullName ?: user.username ?: "Equipe de Campo",
                    capturedAt = brFullTimestamp,
                    date = brDate,
                    quality = calculateDataQuality(),
                    entrevistadoNome = if (entrevistadoEhOResponsavel == "Sim") responsavelData.nomeCompleto.trim() else entrevistadoNomeApenas.trim().orSpace(),
                    entrevistadoCpf = if (entrevistadoEhOResponsavel == "Sim") responsavelData.cpfCnpj.trim() else " ",
                    entrevistadoMae = if (entrevistadoEhOResponsavel == "Sim") responsavelData.nomeMae.trim() else " ",
                    entrevistadoNascimento = if (entrevistadoEhOResponsavel == "Sim") sNasc else " ",
                    entrevistadoSexo = if (entrevistadoEhOResponsavel == "Sim") responsavelData.sexo else null,
                    entrevistadoApresentouDoc = (if (entrevistadoEhOResponsavel == "Sim") responsavelData.apresentouDoc else null).orSpace(),
                    entrevistadoQualDoc = if (entrevistadoEhOResponsavel == "Sim") responsavelData.qualDoc.trim() else " ",
                    entrevistadoEmail = if (entrevistadoEhOResponsavel == "Sim") email.trim() else entrevistadoEmailApenas.trim().orSpace(),
                    entrevistadoCelular = if (entrevistadoEhOResponsavel == "Sim") celular1.trim() else entrevistadoCelularApenas.trim().orSpace(),
                    logradouro = logradouro,
                    numero = numero,
                    complemento = complemento,
                    bairro = bairro,
                    cidade = selectedCity.nome,
                    uf = uf,
                    cep = cep,
                    pavimentoRua = pavimentoRua.orSpace(),
                    pavimentoCalcada = pavimentoCalcada.orSpace(),
                    fonteAbastecimento = fonteAbastecimento.orSpace(),
                    existeRedeAgua = existeRedeAgua.orSpace(),
                    localInstalacao = localInstalacao.orSpace(),
                    accessibilityReading = acessibilidade.orSpace(),
                    observacao = if (observacao.length > 1000) observacao.take(1000) else observacao,
                    usaAguaVizinho = usaAguaVizinho.orSpace(),
                    possuiHidrometro = possuiHidrometro.orSpace(),
                    tilEsgoto = tilEsgoto.orSpace(),
                    electricityMeter = electricityMeter.orSpace(),
                    grupoSugerido = com.example.oaplicativo.util.GeoFencingHelper.findSuggestedGroup(selectedCity.nome, latitude, longitude) ?: "S/G",
                    setor = setor,
                    quadra = quadra,
                    rotaSugerida = com.example.oaplicativo.util.GeoFencingHelper.findSuggestedRoute(selectedCity.nome, latitude, longitude) ?: "S/R",
                    numeroHidrometro = numeroHidrometro.trim().orSpace(),
                    isSynced = false
                )

                Log.d("debugs", "💾 [SQLITE] Gravando Recadastro: ${customer.name} | Cidade: ${customer.cidade}")
                customerRepository.saveCustomerLocallyAndSync(customer)
                onSuccess()
            } catch (e: Exception) {
                Log.e("debugs", "❌ [SQLITE] Falha ao salvar: ${e.message}")
                onError(e.message ?: "Erro ao salvar")
            }
        }
    }

    fun onCepChange(newCep: String) {
        if (newCep.length <= 8) {
            cep = newCep
            if (newCep.length == 8) fetchAddress(newCep)
        }
    }

    private fun fetchAddress(cepCode: String) {
        cepJob?.cancel()
        cepJob = viewModelScope.launch(Dispatchers.IO) {
            isCepLoading = true; cepError = false
            try {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocationName(cepCode, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            addresses.firstOrNull()?.let { handleGoogleAddress(it) }
                            isCepLoading = false
                        }
                        override fun onError(errorMessage: String?) {
                            isCepLoading = false; cepError = true
                        }
                    })
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(cepCode, 1)
                    addresses?.firstOrNull()?.let { handleGoogleAddress(it) }
                    isCepLoading = false
                }
            } catch (_: Exception) { isCepLoading = false; cepError = true }
        }
    }

    fun fetchAddressFromLocation(lat: Double, lng: Double) {
        geocodeJob?.cancel()
        geocodeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            addresses.firstOrNull()?.let { handleGoogleAddress(it) }
                        }
                        override fun onError(errorMessage: String?) {
                            Log.e("GeoDebug", "Erro Geocoder: $errorMessage")
                        }
                    })
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    addresses?.firstOrNull()?.let { handleGoogleAddress(it) }
                }
            } catch (e: Exception) { Log.e("GeoDebug", "Falha Geocode: ${e.message}") }
        }
    }

    private fun handleGoogleAddress(address: android.location.Address) {
        logradouro = address.thoroughfare ?: logradouro
        bairro = address.subLocality ?: bairro
        cidade = address.locality ?: cidade
        uf = address.adminArea ?: uf
        if (cep.isBlank()) cep = address.postalCode?.replace("-", "") ?: ""
    }

    private fun calculateDataQuality(): String {
        val progress = registrationProgress
        return when {
            progress >= 0.85f -> "Boa"
            progress >= 0.50f -> "Regular"
            else -> "Ruim"
        }
    }
}
