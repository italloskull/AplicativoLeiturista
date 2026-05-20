package com.example.oaplicativo.ui.screens.economy_update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.repository.EconomyRepositoryImpl
import com.example.oaplicativo.domain.repository.EconomyRepository
import com.example.oaplicativo.model.EconomyUpdate
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

class EconomyUpdateViewModel(
    application: Application,
    private val repository: EconomyRepository = EconomyRepositoryImpl.getInstance()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<EconomyUpdateState>(EconomyUpdateState.Idle)
    val state: StateFlow<EconomyUpdateState> = _state.asStateFlow()

    val items: StateFlow<List<EconomyUpdate>> = repository.items

    init {
        fetchEconomyUpdates()
    }

    fun fetchEconomyUpdates() {
        viewModelScope.launch {
            repository.fetchEconomyUpdates()
        }
    }

    fun saveEconomyUpdate(item: EconomyUpdate) {
        viewModelScope.launch {
            _state.value = EconomyUpdateState.Loading
            try {
                repository.saveEconomyUpdate(item)
                _state.value = EconomyUpdateState.Success
            } catch (e: Exception) {
                _state.value = EconomyUpdateState.Error(e.message ?: "Erro ao salvar")
            }
        }
    }

    fun getItemById(id: String): EconomyUpdate? {
        return items.value.find { it.id == id }
    }

    fun resetState() {
        _state.value = EconomyUpdateState.Idle
    }
}
