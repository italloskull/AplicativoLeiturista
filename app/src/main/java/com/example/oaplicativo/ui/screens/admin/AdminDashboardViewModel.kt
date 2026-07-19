package com.example.oaplicativo.ui.screens.admin

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oaplicativo.data.SupabaseClient
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.EconomyUpdate
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class DashboardPeriod {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_YEAR,
    CUSTOM
}

data class TeamMemberStats(
    val userId: String,
    val name: String,
    val totalRecadastro: Int,
    val totalGE: Int,
    val averageQuality: Float 
)

data class GroupStats(
    val group: String,
    val total: Int,
    val recadastroCount: Int,
    val geCount: Int
)

data class AdminDashboardState(
    val isLoading: Boolean = true,
    val period: DashboardPeriod = DashboardPeriod.LAST_7_DAYS,
    val startDate: String? = null,
    val endDate: String? = null,
    val cityTotalRecadastro: Int = 0,
    val cityTotalGE: Int = 0,
    val averageQuality: Float = 0f,
    val statsByGroup: List<GroupStats> = emptyList(),
    val teamPerformance: List<TeamMemberStats> = emptyList(),
    val cityName: String = ""
)

class AdminDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AdminDashboardState())
    val uiState: StateFlow<AdminDashboardState> = _uiState.asStateFlow()

    private val authRepo = AuthRepositoryImpl.getInstance()
    private val _authorizedCities = MutableStateFlow<List<com.example.oaplicativo.model.Cidade>>(emptyList())
    val authorizedCities: StateFlow<List<com.example.oaplicativo.model.Cidade>> = _authorizedCities.asStateFlow()

    private val _selectedCityFilter = MutableStateFlow<com.example.oaplicativo.model.Cidade?>(null)
    val selectedCityFilter: StateFlow<com.example.oaplicativo.model.Cidade?> = _selectedCityFilter.asStateFlow()

    fun setPeriod(period: DashboardPeriod, start: String? = null, end: String? = null) {
        _uiState.value = _uiState.value.copy(period = period, startDate = start, endDate = end)
        loadDashboardData()
    }

    fun selectCityFilter(cidade: com.example.oaplicativo.model.Cidade?) {
        _selectedCityFilter.value = cidade
        loadDashboardData()
    }

    private fun loadAuthorizedCities() {
        viewModelScope.launch {
            _authorizedCities.value = authRepo.getUserCities()
        }
    }

    fun loadDashboardData() {
        if (_authorizedCities.value.isEmpty()) {
            loadAuthorizedCities()
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val user = authRepo.currentUserProfile.value
            val isDev = user?.cargo?.lowercase() == "desenvolvedor"
            val targetCidadeId = user?.cidadeId
            val currentState = _uiState.value

            try {
                val brFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                val now = ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo"))
                
                val filterStartDate = when(currentState.period) {
                    DashboardPeriod.TODAY -> now.format(brFormatter)
                    DashboardPeriod.LAST_7_DAYS -> now.minusDays(7).format(brFormatter)
                    DashboardPeriod.LAST_30_DAYS -> now.minusDays(30).format(brFormatter)
                    DashboardPeriod.LAST_YEAR -> now.minusYears(1).format(brFormatter)
                    DashboardPeriod.CUSTOM -> currentState.startDate?.take(10)?.replace("-", "/")
                }
                val filterEndDate = if (currentState.period == DashboardPeriod.CUSTOM) currentState.endDate?.take(10)?.replace("-", "/") else null

                val cityNameFilter = _selectedCityFilter.value?.nome
                val cityName = com.example.oaplicativo.util.CityUtils.getFriendlyCityName(targetCidadeId)

                // SÊNIOR PERF: Consultas paralelas via async para carregamento instantâneo do cockpit
                val customersDef = async {
                    SupabaseClient.client.postgrest["clientes"].select {
                        if (cityNameFilter != null) filter { eq("cidade", cityNameFilter) }
                        else if (!isDev && cityName != null) filter { eq("cidade", cityName) }
                        if (filterStartDate != null) filter { gte("date", filterStartDate) }
                        if (filterEndDate != null) filter { lte("date", filterEndDate) }
                    }.decodeList<Customer>()
                }

                val geDef = async {
                    SupabaseClient.client.postgrest["grandes_empreendimentos"].select {
                        if (cityNameFilter != null) filter { eq("cidade", cityNameFilter) }
                        else if (!isDev && cityName != null) filter { eq("cidade", cityName) }
                        if (filterStartDate != null) filter { gte("data", filterStartDate) }
                        if (filterEndDate != null) filter { lte("data", filterEndDate) }
                    }.decodeList<EconomyUpdate>()
                }

                val remoteCustomers = customersDef.await()
                val remoteGE = geDef.await()

                // Processamento pesado via Sequence para economia de RAM
                val totalRec = remoteCustomers.size
                val totalGE = remoteGE.size
                
                val avgQual = if (remoteCustomers.isNotEmpty()) {
                    remoteCustomers.asSequence().map { c: Customer ->
                        when(c.quality?.lowercase()) {
                            "boa" -> 1.0f
                            "regular" -> 0.5f
                            else -> 0.1f
                        }
                    }.average().toFloat()
                } else 1.0f

                val groupDetails = mutableMapOf<String, Pair<Int, Int>>() 
                remoteCustomers.forEach { c ->
                    val g = c.grupoSugerido ?: "S/G"
                    val current = groupDetails[g] ?: Pair(0, 0)
                    groupDetails[g] = current.copy(first = current.first + 1)
                }
                remoteGE.forEach { gE ->
                    val g = gE.grupoSugerido ?: "S/G"
                    val current = groupDetails[g] ?: Pair(0, 0)
                    groupDetails[g] = current.copy(second = current.second + 1)
                }
                
                val statsByGroup = groupDetails.asSequence().map { (group, counts) ->
                    GroupStats(group, counts.first + counts.second, counts.first, counts.second)
                }.sortedByDescending { it.total }.take(20).toList()

                val teamMap = mutableMapOf<String, Triple<String, Int, Int>>() 
                remoteCustomers.forEach { c ->
                    val id = c.leituristaId ?: "unknown"
                    val name = c.addedBy ?: "Equipe"
                    val current = teamMap[id] ?: Triple(name, 0, 0)
                    teamMap[id] = current.copy(second = current.second + 1)
                }
                remoteGE.forEach { gE ->
                    val id = gE.leituristaId ?: "unknown"
                    val name = gE.addedBy ?: "Equipe"
                    val current = teamMap[id] ?: Triple(name, 0, 0)
                    teamMap[id] = current.copy(third = current.third + 1)
                }

                val teamPerformance = teamMap.asSequence().map { (id, data) ->
                    val memberRecadastros = remoteCustomers.filter { it.leituristaId == id || it.addedBy == data.first }
                    
                    // SÊNIOR BI FIX: Proteção contra divisão por zero e NaN para novos colaboradores
                    val memberQual = if (memberRecadastros.isNotEmpty()) {
                        memberRecadastros.asSequence()
                            .map { when (it.quality?.lowercase()) { "boa" -> 1.0f; "regular" -> 0.5f; else -> 0.2f } }
                            .average().toFloat()
                    } else 1.0f

                    TeamMemberStats(id, data.first, data.second, data.third, if (memberQual.isNaN()) 1.0f else memberQual)
                }.sortedByDescending { it.totalRecadastro + it.totalGE }.toList()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    cityTotalRecadastro = totalRec,
                    cityTotalGE = totalGE,
                    averageQuality = avgQual,
                    statsByGroup = statsByGroup,
                    teamPerformance = teamPerformance,
                    cityName = cityNameFilter ?: cityName ?: "Global"
                )
            } catch (e: Exception) {
                Log.e("debugs", "❌ [BI] Falha ao carregar dashboard: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
