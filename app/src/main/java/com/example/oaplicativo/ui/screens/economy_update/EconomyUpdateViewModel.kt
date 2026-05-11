package com.example.oaplicativo.ui.screens.economy_update

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.model.EconomyUpdate
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

sealed class EconomyUpdateState {
    object Idle : EconomyUpdateState()
    object Loading : EconomyUpdateState()
    object Success : EconomyUpdateState()
    data class Error(val message: String) : EconomyUpdateState()
}

class EconomyUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val client = SupabaseClient.client
    private val authRepository = AuthRepositoryImpl.getInstance()

    private val _state = MutableStateFlow<EconomyUpdateState>(EconomyUpdateState.Idle)
    val state: StateFlow<EconomyUpdateState> = _state.asStateFlow()

    private val _items = MutableStateFlow<List<EconomyUpdate>>(emptyList())
    val items: StateFlow<List<EconomyUpdate>> = _items.asStateFlow()

    init {
        fetchEconomyUpdates()
    }

    fun fetchEconomyUpdates() {
        viewModelScope.launch {
            _state.value = EconomyUpdateState.Loading
            try {
                val list = client.postgrest["atualizacao_economias"]
                    .select {
                        order("criado_em", order = Order.DESCENDING)
                    }
                    .decodeList<EconomyUpdate>()
                _items.value = list
                _state.value = EconomyUpdateState.Idle
            } catch (e: Exception) {
                Log.e("EconomyUpdateVM", "Erro ao buscar: ${e.message}")
                _state.value = EconomyUpdateState.Error(e.message ?: "Erro desconhecido")
            }
        }
    }

    fun saveEconomyUpdate(update: EconomyUpdate) {
        viewModelScope.launch {
            Log.d("EconomyUpdateVM", "Função saveEconomyUpdate chamada.")
            _state.value = EconomyUpdateState.Loading
            try {
                val user = authRepository.currentUserProfile.value
                val fullNow = ZonedDateTime.now()
                
                val finalUpdate = update.copy(
                    addedBy = user?.fullName ?: user?.username ?: "Usuário",
                    createdAt = fullNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    date = fullNow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                )

                Log.d("EconomyUpdateVM", "Iniciando inserção no Supabase...")

                client.postgrest["atualizacao_economias"].insert(finalUpdate)
                
                Log.d("EconomyUpdateVM", "Inserção concluída. Atualizando lista local...")
                
                val currentList = _items.value.toMutableList()
                currentList.add(0, finalUpdate)
                _items.value = currentList
                
                _state.value = EconomyUpdateState.Success
            } catch (e: Exception) {
                Log.e("EconomyUpdateVM", "CRÍTICO: Falha ao salvar!", e)
                _state.value = EconomyUpdateState.Error("Falha ao salvar: ${e.message}")
            }
        }
    }

    fun getItemById(id: String): EconomyUpdate? {
        return _items.value.find { it.id == id }
    }

    fun resetState() {
        _state.value = EconomyUpdateState.Idle
    }
}
