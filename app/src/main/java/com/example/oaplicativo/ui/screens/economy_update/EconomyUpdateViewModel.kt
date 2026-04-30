package com.example.oaplicativo.ui.screens.economy_update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.model.EconomyUpdate
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EconomyUpdateState {
    object Idle : EconomyUpdateState()
    object Loading : EconomyUpdateState()
    object Success : EconomyUpdateState()
    data class Error(val message: String) : EconomyUpdateState()
}

class EconomyUpdateViewModel : ViewModel() {
    private val client = SupabaseClient.client
    
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
                val response = client.postgrest["building_economies"]
                    .select {
                        order("created_at", order = Order.DESCENDING)
                    }.decodeList<EconomyUpdate>()
                _items.value = response
                _state.value = EconomyUpdateState.Idle
            } catch (e: Exception) {
                _state.value = EconomyUpdateState.Error(e.message ?: "Erro ao buscar dados")
            }
        }
    }

    fun saveEconomyUpdate(data: EconomyUpdate) {
        viewModelScope.launch {
            _state.value = EconomyUpdateState.Loading
            try {
                val user = client.auth.currentUserOrNull()
                val dataToSave = data.copy(userId = user?.id)
                
                if (data.id == null) {
                    client.postgrest["building_economies"].insert(dataToSave)
                } else {
                    client.postgrest["building_economies"].update(dataToSave) {
                        filter { eq("id", data.id) }
                    }
                }
                
                fetchEconomyUpdates()
                _state.value = EconomyUpdateState.Success
            } catch (e: Exception) {
                _state.value = EconomyUpdateState.Error(e.message ?: "Erro ao salvar dados")
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