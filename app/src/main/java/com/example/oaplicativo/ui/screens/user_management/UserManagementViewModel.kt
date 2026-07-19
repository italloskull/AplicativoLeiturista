package com.example.oaplicativo.ui.screens.user_management

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.model.Cidade
import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.model.UserCityRelation
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UserManagementState {
    object Idle : UserManagementState()
    object Loading : UserManagementState()
    object Success : UserManagementState()
    data class Error(val message: String) : UserManagementState()
}

class UserManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val client = SupabaseClient.client
    
    private val _users = MutableStateFlow<List<UserProfile>>(emptyList())
    val users: StateFlow<List<UserProfile>> = _users.asStateFlow()

    private val _cidades = MutableStateFlow<List<Cidade>>(emptyList())
    val cidades: StateFlow<List<Cidade>> = _cidades.asStateFlow()

    private val _state = MutableStateFlow<UserManagementState>(UserManagementState.Idle)
    val state: StateFlow<UserManagementState> = _state.asStateFlow()

    // Estado de edição do usuário selecionado
    var editingUser by mutableStateOf<UserProfile?>(null)
    val selectedCidades = mutableStateListOf<String>()

    fun loadInitialData() {
        viewModelScope.launch {
            _state.value = UserManagementState.Loading
            try {
                // SÊNIOR PERF: Carregamento paralelo de usuários e cidades
                val usersDef = async {
                    client.postgrest["perfis_usuario"]
                        .select() {
                            order("full_name", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                        }.decodeList<UserProfile>()
                }
                
                val citiesDef = async {
                    client.postgrest["cidades"]
                        .select() {
                            order("nome", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                        }.decodeList<Cidade>()
                }

                _users.value = usersDef.await()
                _cidades.value = citiesDef.await()
                
                _state.value = UserManagementState.Idle
            } catch (e: Exception) {
                _state.value = UserManagementState.Error("Erro ao carregar dados: ${e.message}")
            }
        }
    }

    fun startEditing(user: UserProfile) {
        editingUser = user
        selectedCidades.clear()
        viewModelScope.launch {
            try {
                // Busca vínculos atuais do usuário
                val relations = client.postgrest["usuario_cidades"]
                    .select {
                        filter { eq("usuario_id", user.id) }
                    }.decodeList<UserCityRelation>()
                
                selectedCidades.addAll(relations.map { it.cidadeId })
            } catch (e: Exception) {
                Log.e("debugs", "❌ [USER_MGMT] Erro ao buscar vínculos: ${e.message}")
            }
        }
    }

    fun saveUserPermissions(onComplete: () -> Unit) {
        val user = editingUser ?: return
        viewModelScope.launch {
            _state.value = UserManagementState.Loading
            try {
                Log.d("debugs", "🔨 [USER_MGMT] Iniciando salvamento para: ${user.username}")
                
                // 1. Atualiza o Cargo na tabela principal
                client.postgrest["perfis_usuario"].update({
                    set("cargo", user.cargo)
                }) {
                    filter { eq("id", user.id) }
                }
                Log.d("debugs", "   - [PASSO 1] Cargo atualizado para: ${user.cargo}")

                // 2. Limpa vínculos antigos
                client.postgrest["usuario_cidades"].delete {
                    filter { eq("usuario_id", user.id) }
                }
                Log.d("debugs", "   - [PASSO 2] Vínculos antigos removidos.")

                // 3. Insere novos vínculos
                if (selectedCidades.isNotEmpty()) {
                    val newRelations = selectedCidades.map { 
                        UserCityRelation(usuarioId = user.id, cidadeId = it) 
                    }
                    client.postgrest["usuario_cidades"].insert(newRelations)
                    Log.d("debugs", "   - [PASSO 3] ${newRelations.size} novos vínculos inseridos.")
                }

                Log.d("debugs", "✅ [USER_MGMT] SUCESSO TOTAL no salvamento.")
                _state.value = UserManagementState.Success
                loadInitialData() 
                onComplete()
            } catch (e: Exception) {
                Log.e("debugs", "❌ [USER_MGMT] FALHA CRÍTICA ao salvar: ${e.message}")
                _state.value = UserManagementState.Error("Erro ao salvar: ${e.message}")
            }
        }
    }
    
    fun resetState() {
        _state.value = UserManagementState.Idle
    }
}
