package com.example.oaplicativo.ui.screens.recadastro.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.remote.viacep.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RecadastroViewModel : ViewModel() {

    // SEÇÃO 1: Identificação
    var matricula by mutableStateOf("")
    var lote by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)

    // SEÇÃO 2: Dados Pessoais
    var nomeCompleto by mutableStateOf("")
    var cpfCnpj by mutableStateOf("")
    var nomeMae by mutableStateOf("")
    var dataNascimento by mutableStateOf("")
    var sexo by mutableStateOf<String?>(null)
    var apresentouDoc by mutableStateOf(false)
    var qualDoc by mutableStateOf("")

    // SEÇÃO 3: Vínculo e Contato
    var isProprietario by mutableStateOf(false)
    var isMorador by mutableStateOf(false)
    var email by mutableStateOf("")
    var telefone by mutableStateOf("")
    var celular1 by mutableStateOf("")
    var celular2 by mutableStateOf("")
    var celular3 by mutableStateOf("")
    var celular4 by mutableStateOf("")

    // SEÇÃO 4: Endereço
    var logradouro by mutableStateOf("")
    var numero by mutableStateOf("")
    var complemento by mutableStateOf("")
    var bairro by mutableStateOf("")
    var cidade by mutableStateOf("")
    var uf by mutableStateOf("")
    var cep by mutableStateOf("")
    
    // UI State for CEP
    var isCepLoading by mutableStateOf(false)
    var cepError by mutableStateOf(false)

    // SEÇÃO 5: Características
    var numeroMoradores by mutableStateOf("")
    var pavimentoRua by mutableStateOf<String?>(null)
    var pavimentoCalcada by mutableStateOf<String?>(null)
    var fonteAbastecimento by mutableStateOf<String?>(null)
    var categoria1 by mutableStateOf<String?>(null)
    var categoria2 by mutableStateOf<String?>(null)
    var situacaoImovel by mutableStateOf<String?>(null)
    var situacaoAgua by mutableStateOf<String?>(null)

    // SEÇÃO 6: Hidrometria
    var possuiHidrometro by mutableStateOf(false)
    var numeroHidrometro by mutableStateOf("")
    var localInstalacao by mutableStateOf<String?>(null)
    var acessibilidade by mutableStateOf("")
    var economias by mutableStateOf("")
    var qualidadeCadastrado by mutableStateOf<String?>(null)

    // SEÇÃO 7: Finalização
    var observacao by mutableStateOf("")
    var nomePrestadorInformacoes by mutableStateOf("")

    private var cepJob: Job? = null

    fun onCepChange(newCep: String) {
        val cleanCep = newCep.filter { it.isDigit() }
        if (cleanCep.length <= 8) {
            cep = cleanCep
            cepError = false
            
            if (cleanCep.length == 8) {
                fetchAddress(cleanCep)
            }
        }
    }

    private fun fetchAddress(cep: String) {
        cepJob?.cancel()
        cepJob = viewModelScope.launch {
            isCepLoading = true
            try {
                val response = RetrofitClient.viaCepService.getAddressByCep(cep)
                if (response.isSuccessful && response.body() != null && response.body()?.erro != true) {
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
