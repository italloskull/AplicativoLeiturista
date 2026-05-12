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
import com.example.oaplicativo.data.remote.viacep.RetrofitClient
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.Cidade
import com.example.oaplicativo.util.LocationHelper
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class RoleData {
    var nomeCompleto by mutableStateOf("")
    var cpfCnpj by mutableStateOf("")
    var nomeMae by mutableStateOf("")
    var dataNascimento by mutableStateOf("")
    var sexo by mutableStateOf<String?>(null)
    var apresentouDoc by mutableStateOf(false)
    var qualDoc by mutableStateOf("")
}

class RecadastroViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CustomerRepositoryImpl.getInstance()
    private val authRepository = AuthRepositoryImpl.getInstance()
    private val localDb = LocalDatabase(application)
    private val locationHelper = LocationHelper(application)
    private val client = SupabaseClient.client

    var matricula by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var isCapturingLocation by mutableStateOf(false)

    var currentRole by mutableStateOf("Entrevistado")
    var entrevistadoVinculo by mutableStateOf("Outro")

    val entrevistadoData = RoleData()
    val proprietarioData = RoleData()
    val locatarioData = RoleData()

    val activeRoleData: RoleData
        get() = when (currentRole) {
            "Proprietario" -> if (entrevistadoVinculo == "Proprietário") entrevistadoData else proprietarioData
            "Locatario" -> if (entrevistadoVinculo == "Locatário") entrevistadoData else locatarioData
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

    var numeroMoradores by mutableStateOf("")
    var pavimentoRua by mutableStateOf<String?>(null)
    var pavimentoCalcada by mutableStateOf<String?>(null)
    var fonteAbastecimento by mutableStateOf<String?>(null)
    var existeRedeAgua by mutableStateOf<Boolean?>(null)
    var possuiPiscina by mutableStateOf<Boolean?>(null)
    var possuiCaixaAgua by mutableStateOf<String?>(null)

    var possuiHidrometro by mutableStateOf(false)
    var numeroHidrometro by mutableStateOf("")
    var localInstalacao by mutableStateOf<String?>(null)
    var acessibilidade by mutableStateOf<String?>(null)
    var economias by mutableStateOf("")

    var observacao by mutableStateOf("")

    private var cepJob: Job? = null

    private suspend fun getLeituristaCidadeNome(cidadeId: String?): String? {
        if (cidadeId == null) return null
        return try {
            val res = client.postgrest["cidades"]
                .select { filter { eq("id", cidadeId) } }
                .decodeSingleOrNull<Cidade>()
            res?.nome
        } catch (_: Exception) {
            null
        }
    }

    fun saveRecadastro(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. GPS FORCE
                if (latitude == null || longitude == null) {
                    isCapturingLocation = true
                    val location = locationHelper.getCurrentLocation() ?: locationHelper.getCachedLocation()
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                    }
                    isCapturingLocation = false
                }

                if (latitude == null || longitude == null) {
                    onError("Localização obrigatória! Por favor, ative o GPS.")
                    return@launch
                }

                val user = authRepository.currentUserProfile.value
                if (user == null) {
                    onError("Sessão expirada. Faça login novamente.")
                    return@launch
                }

                val leituristaNome = user.fullName ?: user.username ?: "Leiturista"
                val leituristaCidadeId = user.cidadeId
                
                var finalCidade = cidade
                if (finalCidade.isBlank()) {
                    val fallback = getLeituristaCidadeNome(leituristaCidadeId)
                    if (fallback != null) {
                        finalCidade = fallback
                        cidade = fallback
                    }
                }

                val quality = calculateDataQuality()
                val now = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                val customer = Customer(
                    cidadeId = leituristaCidadeId,
                    cidade = finalCidade,
                    name = entrevistadoData.nomeCompleto,
                    registrationNumber = matricula,
                    email = email,
                    landline = telefone,
                    cellPhone = celular1,
                    latitude = latitude,
                    longitude = longitude,
                    quality = quality,
                    addedBy = leituristaNome,
                    capturedAt = now, 
                    createdAt = now,
                    date = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
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
                    existeRedeAgua = existeRedeAgua,
                    possuiPiscina = possuiPiscina,
                    possuiCaixaAgua = possuiCaixaAgua,
                    pavimentoRua = pavimentoRua,
                    pavimentoCalcada = pavimentoCalcada,
                    fonteAbastecimento = fonteAbastecimento,
                    observacao = observacao,
                    isSynced = false
                )

                localDb.saveCustomerOffline(customer)

                // --- NOVO: GATILHO ATIVO DE SINCRONIZAÇÃO ---
                // Enfileira um trabalho que dispara ASSIM QUE a internet voltar.
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .addTag("SyncWorkerTag")
                    .build()

                WorkManager.getInstance(getApplication()).enqueueUniqueWork(
                    "immediate_sync_after_save",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )

                try {
                    repository.addCustomer(customer)
                } catch (_: Exception) { }

                onSuccess()
            } catch (e: Exception) {
                Log.e("RecadastroVM", "SAVE ERROR", e)
                onError("Erro interno ao salvar.")
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
            cepError = false
            if (cleanCep.length == 8) fetchAddress(cleanCep)
        }
    }

    private fun fetchAddress(cep: String) {
        cepJob?.cancel()
        cepJob = viewModelScope.launch {
            isCepLoading = true
            try {
                val response = RetrofitClient.viaCepService.getAddressByCep(cep)
                if (response.isSuccessful && response.body()?.erro != true) {
                    val address = response.body()!!
                    logradouro = address.logradouro
                    bairro = address.bairro
                    cidade = address.localidade
                    uf = address.uf
                    cepError = false
                } else {
                    cepError = true
                }
            } catch (_: Exception) {
                cepError = true
            } finally {
                isCepLoading = false
            }
        }
    }
}
