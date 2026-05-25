@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.recadastro.viewmodel

import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.data.repository.StatsRepositoryImpl
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.util.GeoFencingHelper
import com.example.oaplicativo.util.LocationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
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

class RecadastroViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepositoryImpl.getInstance()
    private val customerRepository: CustomerRepository = CustomerRepositoryImpl.getInstance()
    private val localDb = LocalDatabase(application)
    private val locationHelper = LocationHelper(application)
    private val client = SupabaseClient.client
    
    var isDataCensoredInitial by mutableStateOf(false)
    val currentUserProfile = authRepository.currentUserProfile

    // --- ESTADO DO IMÓVEL ---
    var matricula by mutableStateOf("")
    var registrationDigit by mutableStateOf("")
    var setor by mutableStateOf("")
    var quadra by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var isCapturingLocation by mutableStateOf(false)

    // --- ESTADO DO RESPONSÁVEL (OPÇÃO 2) ---
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

    // --- UTILITÁRIOS ---
    private var cepJob: Job? = null
    private var lastResolvedLat = 0.0
    private var lastResolvedLng = 0.0
    private val geocoder = Geocoder(application, Locale.getDefault())

    val registrationProgress: Float
        get() {
            var score = 0
            if (matricula.isNotBlank()) score++
            if (latitude != null) score++
            if (responsavelData.nomeCompleto.isNotBlank()) score++
            if (responsavelData.cpfCnpj.isNotBlank()) score++
            if (logradouro.isNotBlank()) score++
            if (numero.isNotBlank()) score++
            if (bairro.isNotBlank()) score++
            if (celular1.isNotBlank()) score++
            return score / 8f
        }

    private fun getLeituristaCidadeNome(cidadeId: String?): String? {
        return when (cidadeId) {
            "342080a2-f2a8-47c1-8409-906d4e2808be" -> "Itapoá"
            "00b96845-f027-4638-8e6c-7f55f69c5e31" -> "Garuva"
            "c5643444-2396-4f4d-8724-4f014798c897" -> "Araquari"
            "13437e42-706f-44be-993d-d143c7b7440e" -> "Balneário Barra do Sul"
            else -> null
        }
    }

    fun loadCustomerForEdit(customerId: String?) {
        if (customerId == null) return
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
            telefone = customer.landline ?: ""
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

            val hasLoc = !customer.locatarioNome.isNullOrBlank()
            if (hasLoc) {
                responsavelTipo = "Locatário"
                responsavelData.nomeCompleto = customer.locatarioNome ?: ""
                responsavelData.cpfCnpj = customer.locatarioCpf ?: ""
                responsavelData.nomeMae = customer.locatarioMae ?: ""
                responsavelData.dataNascimento = customer.locatarioNascimento ?: ""
                responsavelData.sexo = customer.locatarioSexo
                responsavelData.apresentouDoc = customer.locatarioApresentouDoc.ifSpaceNull()
                responsavelData.qualDoc = customer.locatarioQualDoc ?: ""
            } else {
                responsavelTipo = "Proprietário"
                responsavelData.nomeCompleto = customer.proprietarioNome ?: ""
                responsavelData.cpfCnpj = customer.proprietarioCpf ?: ""
                responsavelData.nomeMae = customer.proprietarioMae ?: ""
                responsavelData.dataNascimento = customer.proprietarioNascimento ?: ""
                responsavelData.sexo = customer.proprietarioSexo
                responsavelData.apresentouDoc = customer.proprietarioApresentouDoc.ifSpaceNull()
                responsavelData.qualDoc = customer.proprietarioQual_doc ?: ""
            }
            entrevistadoEhOResponsavel = if (customer.entrevistadoNome == responsavelData.nomeCompleto) "Sim" else "Não"
            entrevistadoNomeApenas = if (entrevistadoEhOResponsavel == "Não") customer.entrevistadoNome ?: "" else ""
        }
    }

    fun saveRecadastro(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val snapshotLat = latitude
        val snapshotLng = longitude
        val snapshotMatricula = matricula.trim()
        val snapshotDigit = registrationDigit.trim()
        val snapshotEmail = email.trim().lowercase()
        val snapshotCelular = celular1.trim()
        val snapshotLandline = telefone.trim()
        val snapshotLogradouro = logradouro.trim()
        val snapshotNumero = numero.trim()
        val snapshotComplemento = complemento.trim()
        val snapshotBairro = bairro.trim()
        val snapshotCep = cep.trim()
        val snapshotUf = uf.trim()
        val snapshotCidadeManual = cidade.trim()
        val snapshotObs = observacao.trim()
        
        val sEco = economias.toIntOrNull()

        viewModelScope.launch {
            try {
                // MODO "INDSTRUTÍVEL": Removemos qualquer trava de obrigatoriedade.
                // Se o leiturista quiser salvar só com o nome ou só com o GPS, ele pode.

                val user = authRepository.currentUserProfile.value ?: run {
                    onError("Sessão inválida. Faça login novamente.")
                    return@launch
                }

                val finalCidade = snapshotCidadeManual.ifBlank { getLeituristaCidadeNome(user.cidadeId) ?: "" }
                val utcNow = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val brDate = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))

                val sNome = responsavelData.nomeCompleto.trim()
                val sCpf = responsavelData.cpfCnpj.trim()
                val sMae = responsavelData.nomeMae.trim()
                val sNasc = responsavelData.dataNascimento.trim()
                val sSexo = responsavelData.sexo
                val sDoc = responsavelData.apresentouDoc
                val sQual = responsavelData.qualDoc.trim()

                val finalPropNome = if (responsavelTipo == "Proprietário") sNome else ""
                val finalPropCpf = if (responsavelTipo == "Proprietário") sCpf else ""
                val finalPropMae = if (responsavelTipo == "Proprietário") sMae else ""
                val finalPropNasc = if (responsavelTipo == "Proprietário") sNasc else ""
                val finalPropSexo = if (responsavelTipo == "Proprietário") sSexo else null
                val finalPropDoc = if (responsavelTipo == "Proprietário") sDoc else null
                val finalPropQual = if (responsavelTipo == "Proprietário") sQual else ""

                val finalLocNome = if (responsavelTipo == "Locatário") sNome else ""
                val finalLocCpf = if (responsavelTipo == "Locatário") sCpf else ""
                val finalLocMae = if (responsavelTipo == "Locatário") sMae else ""
                val finalLocNasc = if (responsavelTipo == "Locatário") sNasc else ""
                val finalLocSexo = if (responsavelTipo == "Locatário") sSexo else null
                val finalLocDoc = if (responsavelTipo == "Locatário") sDoc else null
                val finalLocQual = if (responsavelTipo == "Locatário") sQual else ""

                val finalEntrevistadoNome = if (entrevistadoEhOResponsavel == "Sim") sNome else entrevistadoNomeApenas
                
                val finalCidadeId = if (user.cidadeId?.length == 36) user.cidadeId else null
                val finalLeituristaId = if (user.id?.length == 36) user.id else null

                // LÓGICA SÊNIOR: Não sobrescrever a situação escolhida, apenas complementar se o GPS falhou
                val baseStatus = locationStatus.orSpace()
                val finalLocationStatus = if (snapshotLat == null) {
                    if (baseStatus == " ") "Sem Sinal" else "$baseStatus (Sem Sinal)"
                } else baseStatus

                val customer = Customer(
                    id = java.util.UUID.randomUUID().toString(),
                    cidadeId = finalCidadeId,
                    leituristaId = finalLeituristaId,
                    name = sNome.orSpace(), // Até o nome vira espaço se estiver vazio
                    registrationNumber = snapshotMatricula.orSpace(),
                    registrationDigit = snapshotDigit.orSpace(),
                    email = if (responsavelTipo == "Proprietário") snapshotEmail.orSpace() else " ",
                    setor = setor.trim().orSpace(),
                    quadra = quadra.trim().orSpace(),
                    landline = snapshotLandline,
                    celular = if (responsavelTipo == "Proprietário") snapshotCelular.orSpace() else " ",
                    isStandardMeasurementBox = isStandardMeasurementBox.orSpace(),
                    isStandardizedSeals = isStandardizedSeals.orSpace(),
                    isHdAccessible = isHdAccessible.orSpace(),
                    isVacationer = isVacationer.orSpace(),
                    possuiPiscina = possuiPiscina.orSpace(),
                    possuiCaixaAgua = possuiCaixaAgua.orSpace(),
                    beneficiarioSocial = beneficiarioSocial.orSpace(),
                    usaAguaVizinho = usaAguaVizinho.orSpace(),
                    possuiHidrometro = possuiHidrometro.orSpace(),
                    latitude = snapshotLat,
                    longitude = snapshotLng,
                    locationStatus = finalLocationStatus,
                    economiesCount = sEco,
                    createdAt = utcNow,
                    addedBy = user.fullName ?: user.username,
                    capturedAt = utcNow,
                    date = brDate,
                    quality = calculateDataQuality(),
                    entrevistadoNome = finalEntrevistadoNome,
                    entrevistadoCpf = if (entrevistadoEhOResponsavel == "Sim") sCpf else "",
                    entrevistadoMae = if (entrevistadoEhOResponsavel == "Sim") sMae else "",
                    entrevistadoNascimento = if (entrevistadoEhOResponsavel == "Sim") sNasc else "",
                    entrevistadoSexo = if (entrevistadoEhOResponsavel == "Sim") sSexo else null,
                    entrevistadoApresentouDoc = (if (entrevistadoEhOResponsavel == "Sim") sDoc else null).orSpace(),
                    entrevistadoQualDoc = if (entrevistadoEhOResponsavel == "Sim") sQual else "",
                    proprietarioNome = finalPropNome,
                    proprietarioCpf = finalPropCpf,
                    proprietarioMae = finalPropMae,
                    proprietarioNascimento = finalPropNasc,
                    proprietarioSexo = finalPropSexo,
                    proprietarioApresentouDoc = finalPropDoc.orSpace(),
                    proprietarioQual_doc = finalPropQual,
                    locatarioNome = finalLocNome,
                    locatarioCpf = finalLocCpf,
                    locatarioMae = finalLocMae,
                    locatarioNascimento = finalLocNasc,
                    locatarioSexo = finalLocSexo,
                    locatarioApresentouDoc = finalLocDoc.orSpace(),
                    locatarioQualDoc = finalLocQual,
                    logradouro = snapshotLogradouro,
                    numero = snapshotNumero,
                    complemento = snapshotComplemento,
                    bairro = snapshotBairro,
                    cidade = finalCidade,
                    uf = snapshotUf,
                    cep = snapshotCep,
                    pavimentoRua = pavimentoRua.orSpace(),
                    pavimentoCalcada = pavimentoCalcada.orSpace(),
                    fonteAbastecimento = fonteAbastecimento.orSpace(),
                    existeRedeAgua = existeRedeAgua.orSpace(),
                    localInstalacao = localInstalacao.orSpace(),
                    acessibilidade = acessibilidade.orSpace(),
                    observacao = snapshotObs,
                    grupoSugerido = GeoFencingHelper.findSuggestedGroup(finalCidade, snapshotLat, snapshotLng),
                    rotaSugerida = GeoFencingHelper.findSuggestedRoute(finalCidade, snapshotLat, snapshotLng),
                    isSynced = false
                )

                localDb.saveCustomerOffline(customer)
                StatsRepositoryImpl.getInstance(getApplication()).refreshStats()

                val pending = localDb.getPendingCustomers().map { it.second }
                customerRepository.updateLocalCustomers(pending)

                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(getApplication()).enqueueUniqueWork("immediate_sync", ExistingWorkPolicy.REPLACE, syncRequest)
                onSuccess()
            } catch (e: Exception) {
                Log.e("RecadastroVM", "ERRO AO SALVAR", e)
                onError("Erro técnico: ${e.message ?: "Falha no SQLite"}")
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

    private fun fetchAddress(cepCode: String) {
        if (isGenericCep(cepCode)) return
        
        cepJob?.cancel()
        cepJob = viewModelScope.launch {
            isCepLoading = true
            cepError = false
            try {
                delay(500)
                val addresses = geocoder.getFromLocationName("CEP $cepCode, Brasil", 1)
                if (!addresses.isNullOrEmpty()) {
                    handleGoogleAddress(addresses[0])
                } else {
                    refineCepWithViaCep(cepCode, null, null)
                }
            } catch (e: Exception) {
                cepError = true
            } finally {
                isCepLoading = false
            }
        }
    }

    fun fetchAddressFromLocation(lat: Double, lng: Double) {
        if (Math.abs(lastResolvedLat - lat) < 0.0001 && Math.abs(lastResolvedLng - lng) < 0.0001) return
        
        lastResolvedLat = lat
        lastResolvedLng = lng
        
        viewModelScope.launch {
            try {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    handleGoogleAddress(addresses[0])
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

    private fun refineCepWithViaCep(zip: String?, street: String?, district: String?) {
        // Lógica de fallback para API externa se necessário
    }

    private fun updateAddressFields(street: String?, district: String?, city: String?, state: String?, zip: String?) {
        if (!street.isNullOrBlank()) logradouro = street
        if (!district.isNullOrBlank()) bairro = district
        if (!city.isNullOrBlank()) cidade = city
        if (!state.isNullOrBlank()) uf = state
        
        // SÊNIOR FIX: Bloquear CEPs genéricos (terminados em 000) no preenchimento automático
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
            progress >= 0.85f -> "Boa"
            progress >= 0.50f -> "Regular"
            else -> "Ruim"
        }
    }

    private fun String?.orSpace(): String? = if (this.isNullOrBlank()) " " else this
    private fun String?.ifSpaceNull(): String? = if (this == " ") null else this
}
