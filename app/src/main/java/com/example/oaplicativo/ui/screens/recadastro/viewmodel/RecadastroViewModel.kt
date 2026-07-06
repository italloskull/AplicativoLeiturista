package com.example.oaplicativo.ui.screens.recadastro.viewmodel

import android.app.Application
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.model.Cidade
import com.example.oaplicativo.util.ifSpaceNull
import com.example.oaplicativo.util.orSpace
import com.example.oaplicativo.util.DateVisualTransformation
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
    var telefone by mutableStateOf("")
    var celular1 by mutableStateOf("")
    var logradouro by mutableStateOf("")
    var numero by mutableStateOf("")
    var complemento by mutableStateOf("")
    var bairro by mutableStateOf("")
    var cidade by mutableStateOf("")
    var uf by mutableStateOf("")
    var cel by mutableStateOf("")
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
    var locationStatus by mutableStateOf<String?>(null)
    var localInstalacao by mutableStateOf<String?>(null)
    var acessibilidade by mutableStateOf<String?>(null)

    var numeroHidrometro by mutableStateOf("")
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
        if (matricula.isNotBlank()) score += 10f
        if (latitude != null && longitude != null) score += 15f
        if (logradouro.isNotBlank() && numero.isNotBlank()) score += 10f
        if (beneficiarioSocial != null) score += 5f
        if (usaAguaVizinho != null) score += 5f
        if (possuiPiscina != null) score += 5f
        if (isVacationer != null) score += 5f
        if (locationStatus != null) score += 5f
        if (possuiHidrometro != null) score += 5f
        if (numeroHidrometro.isNotBlank() && numeroHidrometro != " ") score += 10f
        if (isStandardMeasurementBox != null) score += 5f
        if (isStandardizedSeals != null) score += 5f
        if (isHdAccessible != null) score += 5f
        if (economias.isNotBlank()) score += 5f
        if (responsavelData.nomeCompleto.isNotBlank()) score += 5f
        score / 100f
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
            cidade = customer.cidade ?: ""

            responsavelTipo = "Proprietário" 
            responsavelData.nomeCompleto = customer.entrevistadoNome ?: ""
            responsavelData.cpfCnpj = customer.entrevistadoCpf ?: ""
            responsavelData.nomeMae = customer.entrevistadoMae ?: ""
            responsavelData.dataNascimento = customer.entrevistadoNascimento ?: ""
            responsavelData.sexo = customer.entrevistadoSexo
            responsavelData.apresentouDoc = customer.entrevistadoApresentouDoc ?: "Não"
            responsavelData.qualDoc = customer.entrevistadoQualDoc ?: ""
            
            numeroHidrometro = customer.numeroHidrometro ?: ""
            
            entrevistadoEhOResponsavel = "Sim"
            entrevistadoNomeApenas = ""
            
            _authorizedCities.value.find { it.nome == customer.cidade }?.let {
                selectedCidadeForRegistry = it
            }
        }
    }

    fun saveRecadastro(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = currentUserProfile.value ?: return@launch
                val snapshotMatricula = matricula
                val snapshotDigit = registrationDigit
                val snapshotLat = latitude
                val snapshotLng = longitude
                val snapshotLogradouro = logradouro
                val snapshotNumero = numero
                val snapshotComplemento = complemento
                val snapshotBairro = bairro
                val snapshotCep = cep
                val snapshotUf = uf
                val snapshotEmail = email
                val snapshotCelular = celular1
                val snapshotObs = observacao
                val sEco = economias.toIntOrNull()

                val selectedCity = selectedCidadeForRegistry
                if (selectedCity == null) {
                    onError("Por favor, selecione a cidade do registro no topo da tela.")
                    return@launch
                }

                val brNow = ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                val brDate = brNow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                val brFullTimestamp = brNow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd_HH:mm:ss"))

                val sNome = responsavelData.nomeCompleto.trim()
                val sCpf = responsavelData.cpfCnpj.trim()
                val sMae = responsavelData.nomeMae.trim()
                val rawNasc = responsavelData.dataNascimento.trim()
                // SÊNIOR DATA FIX: Formata a data bruta (21062001) para o padrão banco (21/06/2001)
                val sNasc = if (rawNasc.length == 8) {
                    "${rawNasc.substring(0, 2)}/${rawNasc.substring(2, 4)}/${rawNasc.substring(4)}"
                } else rawNasc

                val sSexo = responsavelData.sexo
                val sDoc = responsavelData.apresentouDoc
                val sQual = responsavelData.qualDoc.trim()

                val customer = Customer(
                    id = editingCustomerId ?: UUID.randomUUID().toString(),
                    cidadeId = selectedCity.id,
                    leituristaId = user.id,
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
                    locationStatus = locationStatus.orSpace(),
                    economiesCount = sEco,
                    addedBy = user.fullName ?: user.username ?: "Equipe de Campo",
                    capturedAt = brFullTimestamp,
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
                    cidade = selectedCity.nome,
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
                    grupoSugerido = com.example.oaplicativo.util.GeoFencingHelper.findSuggestedGroup(selectedCity.nome, snapshotLat, snapshotLng) ?: "S/G",
                    setor = setor,
                    quadra = quadra,
                    rotaSugerida = com.example.oaplicativo.util.GeoFencingHelper.findSuggestedRoute(selectedCity.nome, snapshotLat, snapshotLng) ?: "S/R",
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

    fun resetForm() {
        editingCustomerId = null
        matricula = ""; registrationDigit = ""; setor = ""; quadra = ""
        latitude = null; longitude = null; isCapturingLocation = false
        responsavelTipo = "Proprietário"; entrevistadoEhOResponsavel = "Sim"
        responsavelData.nomeCompleto = ""; responsavelData.cpfCnpj = ""; responsavelData.nomeMae = ""
        responsavelData.dataNascimento = ""; responsavelData.sexo = null; responsavelData.apresentouDoc = null
        responsavelData.qualDoc = ""; entrevistadoNomeApenas = ""; entrevistadoEmailApenas = ""; entrevistadoCelularApenas = ""
        email = ""; telefone = ""; celular1 = ""; logradouro = ""; numero = ""; complemento = ""
        bairro = ""; cidade = ""; uf = ""; cep = ""
        pavimentoRua = null; pavimentoCalcada = null; fonteAbastecimento = null; existeRedeAgua = null
        possuiPiscina = null; possuiCaixaAgua = null; beneficiarioSocial = null; usaAguaVizinho = null
        possuiHidrometro = null; isStandardMeasurementBox = null; isStandardizedSeals = null
        isHdAccessible = null; isVacationer = null; locationStatus = null; localInstalacao = null
        acessibilidade = null; numeroHidrometro = ""; economias = ""; observacao = ""
    }
}
