@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.recadastro.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.oaplicativo.data.SupabaseClient
import android.location.Geocoder
import com.example.oaplicativo.data.remote.viacep.RetrofitClient
import java.util.Locale
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.Cidade
import com.example.oaplicativo.util.LocationHelper
import com.example.oaplicativo.util.GeoFencingHelper
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RoleData {
    var nomeCompleto by mutableStateOf("")
    var cpfCnpj by mutableStateOf("")
    var nomeMae by mutableStateOf("")
    var dataNascimento by mutableStateOf("")
    var sexo by mutableStateOf<String?>(null)
    var apresentouDoc by mutableStateOf<Boolean?>(null)
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

    var matricula by mutableStateOf("")
    var registrationDigit by mutableStateOf("")
    var setor by mutableStateOf("")
    var quadra by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var isCapturingLocation by mutableStateOf(false)

    var currentRole by mutableStateOf("Entrevistado")
    var entrevistadoVinculo by mutableStateOf("Proprietário")

    var entrevistadoData = RoleData()
    var proprietarioData = RoleData()
    var locatarioData = RoleData()

    val activeRoleData: RoleData
        get() = when (currentRole) {
            "Proprietario" -> proprietarioData
            "Locatario" -> locatarioData
            else -> entrevistadoData
        }

    val isCurrentRoleLocked: Boolean
        get() = (currentRole == "Proprietario" && entrevistadoVinculo == "Proprietário") ||
                (currentRole == "Locatario" && entrevistadoVinculo == "Locatário")

    var email by mutableStateOf("")
    var telefone by mutableStateOf("")
    var celular1 by mutableStateOf("")
    var celular2 by mutableStateOf("")

    var logradouro by mutableStateOf("")
    var numero by mutableStateOf("")
    var complemento by mutableStateOf("")
    var bairro by mutableStateOf("")
    var cidade by mutableStateOf("")
    var uf by mutableStateOf("")
    var cep by mutableStateOf("")

    var isCepLoading by mutableStateOf(false)
    var cepError by mutableStateOf(false)

    var pavimentoRua by mutableStateOf<String?>(null)
    var pavimentoCalcada by mutableStateOf<String?>(null)
    var fonteAbastecimento by mutableStateOf<String?>(null)
    var existeRedeAgua by mutableStateOf<Boolean?>(null)
    var possuiPiscina by mutableStateOf<Boolean?>(null)
    var possuiCaixaAgua by mutableStateOf<String?>(null)
    var beneficiarioSocial by mutableStateOf<Boolean?>(null)
    var usaAguaVizinho by mutableStateOf<Boolean?>(null)

    var possuiHidrometro by mutableStateOf<Boolean?>(null)
    var isStandardMeasurementBox by mutableStateOf<Boolean?>(null)
    var isStandardizedSeals by mutableStateOf<Boolean?>(null)
    var isHdAccessible by mutableStateOf<Boolean?>(null)
    var isVacationer by mutableStateOf<Boolean?>(null)
    var locationStatus by mutableStateOf<String?>(null)
    
    var numeroHidrometro by mutableStateOf("")
    var localInstalacao by mutableStateOf<String?>(null)
    var acessibilidade by mutableStateOf<String?>(null)
    var economias by mutableStateOf("")

    var observacao by mutableStateOf("")

    private var cepJob: Job? = null
    private var lastResolvedLat: Double = 0.0
    private var lastResolvedLng: Double = 0.0
    private val geocoder: Geocoder by lazy { Geocoder(application, Locale("pt", "BR")) }

    val registrationProgress: Float
        get() {
            var count = 0
            if (matricula.isNotBlank()) count++
            if (latitude != null) count++
            if (cep.isNotBlank()) count++
            if (logradouro.isNotBlank()) count++
            if (numero.isNotBlank()) count++
            if (entrevistadoData.nomeCompleto.isNotBlank()) count++
            if (entrevistadoData.cpfCnpj.isNotBlank()) count++
            if (celular1.isNotBlank()) count++
            if (beneficiarioSocial != null) count++
            if (usaAguaVizinho != null) count++
            if (possuiHidrometro != null) count++
            return count.toFloat() / 11f
        }

    private suspend fun getLeituristaCidadeNome(cidadeId: String?): String? {
        if (cidadeId == null) return null
        return try {
            val response = client.postgrest["cidades"]
                .select { filter { eq("id", cidadeId) } }
                .decodeSingleOrNull<Cidade>()
            response?.nome
        } catch (_: Exception) {
            null
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
            celular1 = customer.cellPhone ?: ""
            
            beneficiarioSocial = customer.beneficiarioSocial
            usaAguaVizinho = customer.usaAguaVizinho
            possuiHidrometro = customer.possuiHidrometro
            isStandardMeasurementBox = customer.isStandardMeasurementBox
            isStandardizedSeals = customer.isStandardizedSeals
            isHdAccessible = customer.isHdAccessible
            isVacationer = customer.isVacationer
            locationStatus = customer.locationStatus
            existeRedeAgua = customer.existeRedeAgua
            possuiPiscina = customer.possuiPiscina
            possuiCaixaAgua = customer.possuiCaixaAgua
            pavimentoRua = customer.pavimentoRua
            pavimentoCalcada = customer.pavimentoCalcada
            fonteAbastecimento = customer.fonteAbastecimento
            observacao = customer.observacao ?: ""
            economias = customer.economiesCount?.toString() ?: ""
            numeroHidrometro = customer.numeroHidrometro ?: ""
            localInstalacao = customer.localInstalacao
            acessibilidade = customer.acessibilidade

            entrevistadoData.nomeCompleto = customer.entrevistadoNome ?: ""
            entrevistadoData.cpfCnpj = customer.entrevistadoCpf ?: ""
            entrevistadoData.nomeMae = customer.entrevistadoMae ?: ""
            entrevistadoData.dataNascimento = customer.entrevistadoNascimento ?: ""
            entrevistadoData.sexo = customer.entrevistadoSexo
            entrevistadoData.apresentouDoc = customer.entrevistadoApresentouDoc
            entrevistadoData.qualDoc = customer.entrevistadoQualDoc ?: ""
            
            proprietarioData.nomeCompleto = customer.proprietarioNome ?: ""
            proprietarioData.cpfCnpj = customer.proprietarioCpf ?: ""
            proprietarioData.nomeMae = customer.proprietarioMae ?: ""
            proprietarioData.dataNascimento = customer.proprietarioNascimento ?: ""
            proprietarioData.sexo = customer.proprietarioSexo
            proprietarioData.apresentouDoc = customer.proprietarioApresentouDoc
            proprietarioData.qualDoc = customer.proprietarioQual_doc ?: ""
            
            locatarioData.nomeCompleto = customer.locatarioNome ?: ""
            locatarioData.cpfCnpj = customer.locatarioCpf ?: ""
            locatarioData.nomeMae = customer.locatarioMae ?: ""
            locatarioData.dataNascimento = customer.locatarioNascimento ?: ""
            locatarioData.sexo = customer.locatarioSexo
            locatarioData.apresentouDoc = customer.locatarioApresentouDoc
            locatarioData.qualDoc = customer.locatarioQualDoc ?: ""
        }
    }

    fun saveRecadastro(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val snapshotMatricula = matricula.trim()
        val snapshotDigit = registrationDigit.trim()
        val snapshotLat = latitude
        val snapshotLng = longitude
        val snapshotSetor = setor.trim()
        val snapshotQuadra = quadra.trim()
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
        val snapshotVinculo = entrevistadoVinculo
        
        val sEntrevistadoNome = entrevistadoData.nomeCompleto.trim()
        val sEntrevistadoCpf = entrevistadoData.cpfCnpj.trim()
        val sEntrevistadoMae = entrevistadoData.nomeMae.trim()
        val sEntrevistadoNasc = entrevistadoData.dataNascimento.trim()
        val sEntrevistadoSexo = entrevistadoData.sexo
        val sEntrevistadoDoc = entrevistadoData.apresentouDoc
        val sEntrevistadoQual = entrevistadoData.qualDoc.trim()

        val sPropNome = proprietarioData.nomeCompleto.trim()
        val sPropCpf = proprietarioData.cpfCnpj.trim()
        val sPropMae = proprietarioData.nomeMae.trim()
        val sPropNasc = proprietarioData.dataNascimento.trim()
        val sPropSexo = proprietarioData.sexo
        val sPropDoc = proprietarioData.apresentouDoc
        val sPropQual = proprietarioData.qualDoc.trim()

        val sLocNome = locatarioData.nomeCompleto.trim()
        val sLocCpf = locatarioData.cpfCnpj.trim()
        val sLocMae = locatarioData.nomeMae.trim()
        val sLocNasc = locatarioData.dataNascimento.trim()
        val sLocSexo = locatarioData.sexo
        val sLocDoc = locatarioData.apresentouDoc
        val sLocQual = locatarioData.qualDoc.trim()

        val sSocial = beneficiarioSocial
        val sAguaVizinho = usaAguaVizinho
        val sHidrometro = possuiHidrometro
        val sRedeAtiva = existeRedeAgua
        val sPiscina = possuiPiscina
        val sCaixaAgua = possuiCaixaAgua
        val sPavRua = pavimentoRua
        val sPavCalc = pavimentoCalcada
        val sFonte = fonteAbastecimento
        val sEco = economias.toIntOrNull()
        val sNumHidro = numeroHidrometro.trim()
        val sLocalInst = localInstalacao
        val sAcess = acessibilidade

        viewModelScope.launch {
            try {
                if (snapshotLat == null || snapshotLng == null) {
                    onError("Coordenadas de GPS são obrigatórias.")
                    return@launch
                }

                val user = authRepository.currentUserProfile.value ?: run {
                    onError("Sessão inválida. Faça login novamente.")
                    return@launch
                }

                val finalCidade = snapshotCidadeManual.ifBlank { getLeituristaCidadeNome(user.cidadeId) ?: "" }
                val utcNow = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val brDate = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))

                val finalPropNome = if (snapshotVinculo == "Proprietário") sEntrevistadoNome else sPropNome
                val finalPropCpf = if (snapshotVinculo == "Proprietário") sEntrevistadoCpf else sPropCpf
                val finalPropMae = if (snapshotVinculo == "Proprietário") sEntrevistadoMae else sPropMae
                val finalPropNasc = if (snapshotVinculo == "Proprietário") sEntrevistadoNasc else sPropNasc
                val finalPropSexo = if (snapshotVinculo == "Proprietário") sEntrevistadoSexo else sPropSexo
                val finalPropDoc = if (snapshotVinculo == "Proprietário") sEntrevistadoDoc else sPropDoc
                val finalPropQual = if (snapshotVinculo == "Proprietário") sEntrevistadoQual else sPropQual

                val finalLocNome = if (snapshotVinculo == "Locatário") sEntrevistadoNome else sLocNome
                val finalLocCpf = if (snapshotVinculo == "Locatário") sEntrevistadoCpf else sLocCpf
                val finalLocMae = if (snapshotVinculo == "Locatário") sEntrevistadoMae else sLocMae
                val finalLocNasc = if (snapshotVinculo == "Locatário") sEntrevistadoNasc else sLocNasc
                val finalLocSexo = if (snapshotVinculo == "Locatário") sEntrevistadoSexo else sLocSexo
                val finalLocDoc = if (snapshotVinculo == "Locatário") sEntrevistadoDoc else sLocDoc
                val finalLocQual = if (snapshotVinculo == "Locatário") sEntrevistadoQual else sLocQual

                val customer = Customer(
                    id = java.util.UUID.randomUUID().toString(),
                    cidadeId = user.cidadeId,
                    leituristaId = user.id,
                    name = sEntrevistadoNome,
                    registrationNumber = snapshotMatricula,
                    registrationDigit = snapshotDigit,
                    email = snapshotEmail,
                    setor = snapshotSetor,
                    quadra = snapshotQuadra,
                    landline = snapshotLandline,
                    cellPhone = snapshotCelular,
                    isStandardMeasurementBox = isStandardMeasurementBox,
                    isStandardizedSeals = isStandardizedSeals,
                    isHdAccessible = isHdAccessible,
                    isVacationer = isVacationer,
                    possuiPiscina = sPiscina,
                    possuiCaixaAgua = sCaixaAgua,
                    beneficiarioSocial = sSocial,
                    usaAguaVizinho = sAguaVizinho,
                    possuiHidrometro = sHidrometro,
                    latitude = snapshotLat,
                    longitude = snapshotLng,
                    locationStatus = locationStatus,
                    economiesCount = sEco,
                    createdAt = utcNow,
                    addedBy = user.fullName ?: user.username,
                    capturedAt = utcNow,
                    date = brDate,
                    quality = calculateDataQuality(),
                    entrevistadoNome = sEntrevistadoNome,
                    entrevistadoCpf = sEntrevistadoCpf,
                    entrevistadoMae = sEntrevistadoMae,
                    entrevistadoNascimento = sEntrevistadoNasc,
                    entrevistadoSexo = sEntrevistadoSexo,
                    entrevistadoApresentouDoc = sEntrevistadoDoc,
                    entrevistadoQualDoc = sEntrevistadoQual,
                    proprietarioNome = finalPropNome,
                    proprietarioCpf = finalPropCpf,
                    proprietarioMae = finalPropMae,
                    proprietarioNascimento = finalPropNasc,
                    proprietarioSexo = finalPropSexo,
                    proprietarioApresentouDoc = finalPropDoc,
                    proprietarioQual_doc = finalPropQual,
                    locatarioNome = finalLocNome,
                    locatarioCpf = finalLocCpf,
                    locatarioMae = finalLocMae,
                    locatarioNascimento = finalLocNasc,
                    locatarioSexo = finalLocSexo,
                    locatarioApresentouDoc = finalLocDoc,
                    locatarioQualDoc = finalLocQual,
                    logradouro = snapshotLogradouro,
                    numero = snapshotNumero,
                    complemento = snapshotComplemento,
                    bairro = snapshotBairro,
                    cidade = finalCidade,
                    uf = snapshotUf,
                    cep = snapshotCep,
                    pavimentoRua = sPavRua,
                    pavimentoCalcada = sPavCalc,
                    hidrometroProximo = sNumHidro,
                    fonteAbastecimento = sFonte,
                    existeRedeAgua = sRedeAtiva,
                    observacao = snapshotObs,
                    localInstalacao = sLocalInst,
                    acessibilidade = sAcess,
                    numeroHidrometro = sNumHidro,
                    grupoSugerido = GeoFencingHelper.findSuggestedGroup(finalCidade, snapshotLat, snapshotLng),
                    isSynced = false
                )

                localDb.saveCustomerOffline(customer)
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

    private fun isGenericCep(cepIn: String?): Boolean {
        if (cepIn.isNullOrBlank()) return true
        val clean = cepIn.filter { it.isDigit() }
        return clean.endsWith("000") || clean.length != 8
    }

    fun calculateDataQuality(): String {
        val fields = listOf(matricula, email, logradouro, numero, cep, entrevistadoData.nomeCompleto)
        val filled = fields.count { it.isNotBlank() }
        return when {
            filled >= 5 -> "Boa"
            filled >= 3 -> "Regular"
            else -> "Ruim"
        }
    }

    fun onCepChange(newCep: String) {
        val cleanCep = newCep.filter { it.isDigit() }
        if (cleanCep.length <= 8) {
            cep = cleanCep
            if (cleanCep.length == 8) fetchAddress(cleanCep)
        }
    }

    private fun fetchAddress(cepIn: String) {
        cepJob?.cancel()
        cepJob = viewModelScope.launch {
            isCepLoading = true
            try {
                val response = RetrofitClient.viaCepService.getAddressByCep(cepIn)
                if (response.isSuccessful && response.body()?.erro != true) {
                    val address = response.body()!!
                    logradouro = address.logradouro
                    bairro = address.bairro
                    cidade = address.localidade
                    uf = address.uf
                }
            } catch (_: Exception) { } finally {
                isCepLoading = false
            }
        }
    }

    fun fetchAddressFromLocation(lat: Double, lng: Double) {
        val distance = locationHelper.calculateDistance(lastResolvedLat, lastResolvedLng, lat, lng)
        if (distance < 5.0 && lastResolvedLat != 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                lastResolvedLat = lat
                lastResolvedLng = lng
                val osmResponse = RetrofitClient.nominatimService.reverseGeocode(lat, lng)
                if (osmResponse.isSuccessful && osmResponse.body()?.address != null) {
                    val addr = osmResponse.body()!!.address!!
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        updateAddressFields(newLogradouro = addr.road, newBairro = addr.suburb, newCidade = addr.city ?: addr.town, newUf = addr.state, newCep = null)
                        val osmCepStr = addr.postcode?.filter { it.isDigit() } ?: ""
                        if (isGenericCep(osmCepStr)) refineCepWithViaCep(addr.state, addr.city ?: addr.town, addr.road)
                        else if (cep.isBlank() || isGenericCep(cep)) cep = osmCepStr
                    }
                    return@launch
                }
                if (!Geocoder.isPresent()) return@launch
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocation(lat, lng, 5) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val best = addresses.find { !it.thoroughfare.isNullOrBlank() } ?: addresses[0]
                            viewModelScope.launch(Dispatchers.Main) { handleGoogleAddress(best) }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 5)
                    if (!addresses.isNullOrEmpty()) {
                        val best = addresses.find { !it.thoroughfare.isNullOrBlank() } ?: addresses[0]
                        kotlinx.coroutines.withContext(Dispatchers.Main) { handleGoogleAddress(best) }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun handleGoogleAddress(bestAddr: android.location.Address) {
        updateAddressFields(newLogradouro = bestAddr.thoroughfare, newBairro = bestAddr.subLocality, newCidade = bestAddr.locality, newUf = bestAddr.adminArea, newCep = null)
        refineCepWithViaCep(bestAddr.adminArea, bestAddr.locality, bestAddr.thoroughfare)
    }

    private fun refineCepWithViaCep(ufIn: String?, cidadeIn: String?, logradouroIn: String?) {
        if (ufIn.isNullOrBlank() || cidadeIn.isNullOrBlank() || logradouroIn.isNullOrBlank()) return
        val stateMap = mapOf("Santa Catarina" to "SC", "Paraná" to "PR", "Rio Grande do Sul" to "RS", "São Paulo" to "SP")
        val cleanUf = stateMap[ufIn.trim()] ?: if (ufIn.length == 2) ufIn.uppercase() else "SC"
        val cleanStreet = logradouroIn.replace(Regex("\\d+"), "").trim()
        if (cleanStreet.length < 3) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.viaCepService.getCepByAddress(cleanUf, cidadeIn.trim(), cleanStreet)
                if (response.isSuccessful) {
                    val list = response.body()
                    if (!list.isNullOrEmpty()) {
                        val match = list.find { it.logradouro.normalizeForSearch().contains(cleanStreet.normalizeForSearch(), ignoreCase = true) } ?: list[0]
                        val refinedCep = match.cep.filter { it.isDigit() }
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            if (cep.isBlank() || isGenericCep(cep)) {
                                cep = refinedCep
                                if (logradouro.isBlank() || logradouro.length < match.logradouro.length) {
                                    logradouro = match.logradouro
                                    bairro = match.bairro ?: ""
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun updateAddressFields(newLogradouro: String?, newBairro: String?, newCidade: String?, newUf: String?, newCep: String?) {
        if (logradouro.isBlank() && !newLogradouro.isNullOrBlank()) logradouro = newLogradouro.trim()
        if (bairro.isBlank() && !newBairro.isNullOrBlank()) bairro = newBairro.trim()
        if (cidade.isBlank() && !newCidade.isNullOrBlank()) cidade = newCidade.trim()
        if (uf.isBlank() && !newUf.isNullOrBlank()) {
            val stateMap = mapOf("Santa Catarina" to "SC", "Paraná" to "PR", "Rio Grande do Sul" to "RS", "São Paulo" to "SP")
            uf = stateMap[newUf.trim()] ?: newUf.trim().take(2).uppercase()
        }
        val cleanCep = newCep?.filter { it.isDigit() } ?: ""
        if (cep.isBlank() && cleanCep.length == 8 && !isGenericCep(cleanCep)) cep = cleanCep
    }

    private fun String.normalizeForSearch(): String {
        return this.lowercase().replace(Regex("[áàâã]"), "a").replace(Regex("[éèê]"), "e").replace(Regex("[íìî]"), "i").replace(Regex("[óòôõ]"), "o").replace(Regex("[úùû]"), "u").replace("ç", "c").trim()
    }
}
