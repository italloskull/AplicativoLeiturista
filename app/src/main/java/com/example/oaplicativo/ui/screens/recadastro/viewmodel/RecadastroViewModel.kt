package com.example.oaplicativo.ui.screens.recadastro.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.remote.viacep.RetrofitClient
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.domain.usecase.SaveCustomerUseCase
import com.example.oaplicativo.model.Customer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RoleData {
    var nomeCompleto by mutableStateOf("")
    var cpfCnpj by mutableStateOf("")
    var nomeMae by mutableStateOf("")
    var dataNascimento by mutableStateOf("")
    var sexo by mutableStateOf<String?>(null)
    var apresentouDoc by mutableStateOf(false)
    var qualDoc by mutableStateOf("")
}

/**
 * [RecadastroViewModel] refatorado para usar Clean Architecture Patterns.
 */
class RecadastroViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repositórios e UseCases (Injetados manualmente via Singleton para este contexto)
    private val customerRepository = CustomerRepositoryImpl.getInstance()
    private val authRepository = AuthRepositoryImpl.getInstance()
    private val saveCustomerUseCase = SaveCustomerUseCase(customerRepository)

    // --- ESTADOS DE UI (Mantidos para compatibilidade com o Compose) ---
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
    var modeloHidrometro by mutableStateOf("")
    var localInstalacao by mutableStateOf<String?>(null)
    var acessibilidade by mutableStateOf<String?>(null)
    var economias by mutableStateOf("")
    var observacao by mutableStateOf("")

    private var cepJob: Job? = null

    /**
     * LÓGICA DE NEGÓCIO: Delegada ao Use Case
     */
    fun saveRecadastro(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val quality = calculateDataQuality()
            val user = authRepository.currentUserProfile.value
            val userFullName = user?.fullName ?: user?.username ?: "Usuário"
            
            val customer = Customer(
                name = entrevistadoData.nomeCompleto,
                registrationNumber = matricula,
                email = email,
                cellPhone = celular1,
                latitude = latitude,
                longitude = longitude,
                
                // Mapeamento dos campos multi-papel
                entrevistadoNome = entrevistadoData.nomeCompleto,
                entrevistadoCpf = entrevistadoData.cpfCnpj,
                proprietarioNome = if (entrevistadoVinculo == "Proprietário") entrevistadoData.nomeCompleto else proprietarioData.nomeCompleto,
                locatarioNome = if (entrevistadoVinculo == "Locatário") entrevistadoData.nomeCompleto else locatarioData.nomeCompleto,
                
                // Outros campos técnicos
                locationStatus = pavimentoRua, // Exemplo de mapeamento
                economiesCount = economias.toIntOrNull()
            )

            // Chamada ao Use Case (Domain Layer)
            saveCustomerUseCase(customer, quality, userFullName)
            onSuccess()
        }
    }

    /**
     * Métrica de Qualidade (Business Rule)
     */
    fun calculateDataQuality(): String {
        val fields = mutableListOf<String>()
        fields.addAll(listOf(matricula, email, logradouro, numero, cep, numeroMoradores))
        fields.addAll(listOf(pavimentoRua ?: "", localInstalacao ?: "", acessibilidade ?: ""))
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
            } catch (e: Exception) {
                cepError = true
            } finally {
                isCepLoading = false
            }
        }
    }
}
