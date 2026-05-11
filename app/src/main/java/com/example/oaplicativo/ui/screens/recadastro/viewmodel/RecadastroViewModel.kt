@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.recadastro.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.remote.viacep.RetrofitClient
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.model.Customer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

    var matricula by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)

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

    fun saveRecadastro(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val quality = calculateDataQuality()
                val fullNow = ZonedDateTime.now()
                val timestampStr = fullNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                
                val user = authRepository.currentUserProfile.value
                val leituristaNome = user?.fullName ?: user?.username ?: "Leiturista"
                val leituristaCidadeId = user?.cidadeId

                val customer = Customer(
                    cidadeId = leituristaCidadeId,
                    name = entrevistadoData.nomeCompleto,
                    registrationNumber = matricula,
                    email = email,
                    landline = telefone,
                    cellPhone = celular1,
                    latitude = latitude,
                    longitude = longitude,
                    quality = quality,
                    
                    // --- PREENCHIMENTO AUTOMÁTICO DE METADADOS ---
                    addedBy = leituristaNome,
                    capturedAt = timestampStr, 
                    createdAt = timestampStr,
                    date = fullNow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                    
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
                    cidade = cidade,
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

                try {
                    repository.addCustomer(customer)
                    Log.d("RecadastroVM", "Sincronizado com Supabase.")
                } catch (_: Exception) {
                    Log.e("RecadastroVM", "Offline: registro aguardando conexão.")
                }

                onSuccess()
            } catch (e: Exception) {
                Log.e("RecadastroVM", "CRITICAL SAVE ERROR", e)
            }
        }
    }

    fun calculateDataQuality(): String {
        val fields = mutableListOf<String>()
        fields.addAll(listOf(matricula, email, logradouro, numero, cep, numeroMoradores))
        fields.addAll(listOf(pavimentoRua ?: "", localInstalacao ?: "", acessibilidade ?: ""))
        if (possuiHidrometro) fields.add(numeroHidrometro)
        fields.add(entrevistadoData.nomeCompleto)
        fields.add(entrevistadoData.cpfCnpj)
        val filledCount = fields.count { it.isNotBlank() }
        val percentage = (filledCount.toFloat() / fields.size.toFloat()) * 100
        return when {
            percentage >= 90f -> "Boa"
            percentage >= 50f -> "Regular"
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
