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
import java.util.concurrent.TimeUnit

class RoleData {
    var nomeCompleto by mutableStateOf("")
    var cpfCnpj by mutableStateOf("")
    var nomeMae by mutableStateOf("")
    var dataNascimento by mutableStateOf("")
    var sexo by mutableStateOf<String?>(null)
    var apresentouDoc by mutableStateOf<Boolean?>(null)
    var qualDoc by mutableStateOf("")

    // VALIDAÇÕES REATIVAS
    val isDocValid: Boolean
        get() = cpfCnpj.isBlank() || com.example.oaplicativo.util.ValidationUtils.isValidDoc(cpfCnpj)
    
    val isBirthDateValid: Boolean
        get() = dataNascimento.isBlank() || com.example.oaplicativo.util.ValidationUtils.isValidBirthDate(dataNascimento)
}

class RecadastroViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepositoryImpl.getInstance()
    private val customerRepository: com.example.oaplicativo.domain.repository.CustomerRepository = CustomerRepositoryImpl.getInstance()
    private val localDb = LocalDatabase(application)
    private val locationHelper = LocationHelper(application)
    private val client = SupabaseClient.client

    var matricula by mutableStateOf("")
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
    val isEmailValid: Boolean
        get() = email.isBlank() || android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    var telefone by mutableStateOf("")
    var celular1 by mutableStateOf("")
    val isCelular1Valid: Boolean
        get() {
            val digits = celular1.filter { it.isDigit() }
            return digits.isBlank() || digits.length == 11 || digits.length == 10
        }
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

    var numeroMoradores by mutableStateOf("")
    var pavimentoRua by mutableStateOf<String?>(null)
    var pavimentoCalcada by mutableStateOf<String?>(null)
    var fonteAbastecimento by mutableStateOf<String?>(null)
    var existeRedeAgua by mutableStateOf<Boolean?>(null)
    var possuiPiscina by mutableStateOf<Boolean?>(null)
    var possuiCaixaAgua by mutableStateOf<String?>(null)
    var beneficiarioSocial by mutableStateOf<Boolean?>(null)
    var usaAguaVizinho by mutableStateOf<Boolean?>(null)

    var possuiHidrometro by mutableStateOf<Boolean?>(null)
    var numeroHidrometro by mutableStateOf("")
    var localInstalacao by mutableStateOf<String?>(null)
    var acessibilidade by mutableStateOf<String?>(null)
    var economias by mutableStateOf("")

    var observacao by mutableStateOf("")

    private var cepJob: Job? = null
    private var lastResolvedLat: Double = 0.0
    private var lastResolvedLng: Double = 0.0
    private val geocoder: Geocoder by lazy { Geocoder(application, Locale("pt", "BR")) }

    // OTIMIZAÇÃO: Progresso agora é um estado derivado estável
    val registrationProgress: Float
        get() {
            // Lista estática para evitar alocações repetitivas no cálculo
            val filledCount = countFilledFields()
            return filledCount.toFloat() / 11f // 11 é o número total de campos peso
        }

    private fun countFilledFields(): Int {
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
        return count
    }

    private suspend fun getLeituristaCidadeNome(cidadeId: String?): String? {
        if (cidadeId == null) return null
        return try {
            val response = client.postgrest["cidades"]
                .select { filter { eq("id", cidadeId) } }
                .decodeSingleOrNull<Cidade>()
            response?.nome
        } catch (e: Exception) {
            null
        }
    }

    fun saveRecadastro(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (latitude == null || longitude == null) {
                    isCapturingLocation = true
                    val location = locationHelper.getCurrentLocation() ?: locationHelper.getCachedLocation()
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                    }
                    isCapturingLocation = false
                }

                if (latitude == null) {
                    onError("Localização obrigatória!")
                    return@launch
                }

                val user = authRepository.currentUserProfile.value
                if (user == null) {
                    onError("Sessão expirada.")
                    return@launch
                }

                var finalCidade = cidade
                if (finalCidade.isBlank()) {
                    finalCidade = getLeituristaCidadeNome(user.cidadeId) ?: ""
                }

                val now = ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                val customer = Customer(
                    cidadeId = user.cidadeId,
                    leituristaId = user.id,
                    cidade = finalCidade,
                    name = entrevistadoData.nomeCompleto,
                    registrationNumber = matricula,
                    setor = setor,
                    quadra = quadra,
                    email = email,
                    landline = telefone,
                    cellPhone = celular1,
                    latitude = latitude,
                    longitude = longitude,
                    quality = calculateDataQuality(),
                    addedBy = user.fullName ?: user.username,
                    capturedAt = now, 
                    createdAt = now,
                    date = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                    syncedAt = null,
                    entrevistadoNome = entrevistadoData.nomeCompleto,
                    entrevistadoCpf = entrevistadoData.cpfCnpj,
                    entrevistadoMae = entrevistadoData.nomeMae,
                    entrevistadoNascimento = entrevistadoData.dataNascimento,
                    entrevistadoSexo = entrevistadoData.sexo,
                    entrevistadoApresentouDoc = entrevistadoData.apresentouDoc,
                    entrevistadoQualDoc = entrevistadoData.qualDoc,
                    proprietarioNome = if (entrevistadoVinculo == "Proprietário") entrevistadoData.nomeCompleto else proprietarioData.nomeCompleto,
                    proprietarioCpf = if (entrevistadoVinculo == "Proprietário") entrevistadoData.cpfCnpj else proprietarioData.cpfCnpj,
                    locatarioNome = if (entrevistadoVinculo == "Locatário") entrevistadoData.nomeCompleto else locatarioData.nomeCompleto,
                    locatarioCpf = if (entrevistadoVinculo == "Locatário") entrevistadoData.cpfCnpj else locatarioData.cpfCnpj,
                    logradouro = logradouro,
                    numero = numero,
                    complemento = complemento,
                    bairro = bairro,
                    uf = uf,
                    cep = cep,
                    beneficiarioSocial = beneficiarioSocial,
                    usaAguaVizinho = usaAguaVizinho,
                    possuiHidrometro = possuiHidrometro,
                    existeRedeAgua = existeRedeAgua,
                    possuiPiscina = possuiPiscina,
                    possuiCaixaAgua = possuiCaixaAgua,
                    pavimentoRua = pavimentoRua,
                    pavimentoCalcada = pavimentoCalcada,
                    fonteAbastecimento = fonteAbastecimento,
                    observacao = observacao,
                    grupoSugerido = GeoFencingHelper.findSuggestedGroup(finalCidade, latitude, longitude),
                    isSynced = false
                )

                localDb.saveCustomerOffline(customer)

                // FORÇA ATUALIZAÇÃO DA LISTA: Notifica o repositório sobre o novo dado local
                customerRepository.updateLocalCustomers(localDb.getPendingCustomers().map { it.second })

                val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).addTag("SyncWorkerTag").build()
                WorkManager.getInstance(getApplication()).enqueueUniqueWork("immediate_sync", ExistingWorkPolicy.REPLACE, syncRequest)

                onSuccess()

            } catch (e: Exception) {
                Log.e("RecadastroVM", "ERRO AO SALVAR: ${e.message}", e)
                onError("Erro técnico: ${e.message ?: "Falha no SQLite"}")
            }
        }
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
        // OTIMIZAÇÃO: Só dispara busca se o leiturista se mover mais de 5 metros
        val distance = locationHelper.calculateDistance(lastResolvedLat, lastResolvedLng, lat, lng)
        if (distance < 5.0 && lastResolvedLat != 0.0) {
            Log.d("RecadastroVM", "Distância insignificante ($distance m). Pulando busca de endereço.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                lastResolvedLat = lat
                lastResolvedLng = lng

                // TENTATIVA 1: OSM Nominatim
                val osmResponse = RetrofitClient.nominatimService.reverseGeocode(lat, lng)
                if (osmResponse.isSuccessful && osmResponse.body()?.address != null) {
                    val addr = osmResponse.body()!!.address!!
                    
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        updateAddressFields(
                            newLogradouro = addr.road,
                            newBairro = addr.suburb,
                            newCidade = addr.city ?: addr.town,
                            newUf = addr.state,
                            newCep = null 
                        )
                        val osmCepStr = addr.postcode?.filter { it.isDigit() } ?: ""
                        if (osmCepStr.endsWith("000") || osmCepStr.isBlank()) {
                            refineCepWithViaCep(addr.state, addr.city ?: addr.town, addr.road)
                        } else {
                            if (cep.isBlank() || cep.endsWith("000")) cep = osmCepStr
                        }
                    }
                    return@launch
                }

                // TENTATIVA 2: Google Geocoder
                if (!Geocoder.isPresent()) return@launch
                val geocoder = Geocoder(getApplication(), Locale("pt", "BR"))
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocation(lat, lng, 5) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val best = addresses.find { !it.thoroughfare.isNullOrBlank() } ?: addresses[0]
                            viewModelScope.launch(Dispatchers.Main) {
                                handleGoogleAddress(best)
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 5)
                    if (!addresses.isNullOrEmpty()) {
                        val best = addresses.find { !it.thoroughfare.isNullOrBlank() } ?: addresses[0]
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            handleGoogleAddress(best)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("RecadastroVM", "Erro na Geocodificação: ${e.message}")
            }
        }
    }

    private fun handleGoogleAddress(bestAddr: android.location.Address) {
        updateAddressFields(
            newLogradouro = bestAddr.thoroughfare,
            newBairro = bestAddr.subLocality,
            newCidade = bestAddr.locality,
            newUf = bestAddr.adminArea,
            newCep = null
        )
        refineCepWithViaCep(bestAddr.adminArea, bestAddr.locality, bestAddr.thoroughfare)
    }

    private fun refineCepWithViaCep(ufIn: String?, cidadeIn: String?, logradouroIn: String?) {
        if (ufIn.isNullOrBlank() || cidadeIn.isNullOrBlank() || logradouroIn.isNullOrBlank()) return
        
        val stateMap = mapOf(
            "Acre" to "AC", "Alagoas" to "AL", "Amapá" to "AP", "Amazonas" to "AM", "Bahia" to "BA",
            "Ceará" to "CE", "Distrito Federal" to "DF", "Espírito Santo" to "ES", "Goiás" to "GO",
            "Maranhão" to "MA", "Mato Grosso" to "MT", "Mato Grosso do Sul" to "MS", "Minas Gerais" to "MG",
            "Pará" to "PA", "Paraíba" to "PB", "Paraná" to "PR", "Pernambuco" to "PE", "Piauí" to "PI",
            "Rio de Janeiro" to "RJ", "Rio Grande do Norte" to "RN", "Rio Grande do Sul" to "RS",
            "Rondônia" to "RO", "Roraima" to "RR", "Santa Catarina" to "SC", "São Paulo" to "SP",
            "Sergipe" to "SE", "Tocantins" to "TO"
        )
        
        val cleanUf = stateMap[ufIn.trim()] ?: if (ufIn.length == 2) ufIn.uppercase() else "SC"
        val cleanStreet = logradouroIn.replace(Regex("\\d+"), "").trim()

        if (cleanStreet.length < 3) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.viaCepService.getCepByAddress(cleanUf, cidadeIn.trim(), cleanStreet)
                if (response.isSuccessful) {
                    val list = response.body()
                    if (!list.isNullOrEmpty()) {
                        val match = list.find { it.logradouro.contains(cleanStreet, ignoreCase = true) } ?: list[0]
                        val refinedCep = match.cep.filter { it.isDigit() }
                        
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            if (cep.isBlank() || cep.endsWith("000")) {
                                cep = refinedCep
                                if (logradouro.isBlank() || logradouro.length < match.logradouro.length) {
                                    logradouro = match.logradouro
                                    bairro = match.bairro
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RecadastroVM", "Erro ViaCEP: ${e.message}")
            }
        }
    }

    private fun updateAddressFields(
        newLogradouro: String?, 
        newBairro: String?, 
        newCidade: String?, 
        newUf: String?, 
        newCep: String?
    ) {
        // SEGURANÇA SÊNIOR: Se o usuário já interagiu com o campo, a IA NUNCA sobrescreve.
        if (logradouro.isBlank() && !newLogradouro.isNullOrBlank()) logradouro = newLogradouro.trim()
        if (bairro.isBlank() && !newBairro.isNullOrBlank()) bairro = newBairro.trim()
        if (cidade.isBlank() && !newCidade.isNullOrBlank()) cidade = newCidade.trim()
        if (uf.isBlank() && !newUf.isNullOrBlank()) {
            val stateMap = mapOf("Santa Catarina" to "SC", "Paraná" to "PR", "Rio Grande do Sul" to "RS", "São Paulo" to "SP")
            uf = stateMap[newUf.trim()] ?: newUf.trim().take(2).uppercase()
        }

        // TRAVA DE QUALIDADE DE CEP: Bloqueia lixo (-000) e preserva dados manuais
        val cleanCep = newCep?.filter { it.isDigit() } ?: ""
        if (cep.isBlank() && cleanCep.length == 8 && !cleanCep.endsWith("000")) {
            cep = cleanCep
        }
    }

    /**
     * Normaliza strings para busca em APIs brasileiras (ViaCEP)
     */
    private fun String.normalizeForSearch(): String {
        return this.lowercase()
            .replace(Regex("[áàâã]"), "a")
            .replace(Regex("[éèê]"), "e")
            .replace(Regex("[íìî]"), "i")
            .replace(Regex("[óòôõ]"), "o")
            .replace(Regex("[úùû]"), "u")
            .replace("ç", "c")
            .trim()
    }
}
