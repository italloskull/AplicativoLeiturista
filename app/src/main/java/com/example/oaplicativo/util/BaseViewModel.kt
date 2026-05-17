package com.example.oaplicativo.util

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BaseViewModel: Padroniza como todos os ViewModels do sistema emitem estados.
 */
abstract class BaseViewModel<T> : ViewModel() {
    protected val _uiState = MutableStateFlow<Resource<T>>(Resource.Loading)
    val uiState: StateFlow<Resource<T>> = _uiState.asStateFlow()

    protected fun emitSuccess(data: T) {
        _uiState.value = Resource.Success(data)
    }

    protected fun emitError(message: String, exception: Throwable? = null) {
        _uiState.value = Resource.Error(message, exception)
    }

    protected fun emitLoading() {
        _uiState.value = Resource.Loading
    }
}
