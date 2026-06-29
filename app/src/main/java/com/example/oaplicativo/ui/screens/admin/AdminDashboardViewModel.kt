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
    val averageQuality: Float // 0.0 to 1.0
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

    fun setPeriod(period: DashboardPeriod, start: String? = null, end: String? = null) {
        _uiState.value = _uiState.value.copy(period = period, startDate = start, endDate = end)
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val user = authRepo.currentUserProfile.value
            val isDev = user?.cargo?.lowercase() == "desenvolvedor"
            val targetCidadeId = user?.cidadeId
            val currentState = _uiState.value

            try {
                // SÊNIOR BI FIX: Inteligência Temporal via Coluna 'DATE' (100% Fidedigna)
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

                val cityName = getFriendlyCityName(targetCidadeId)
                Log.d("debugs", "📊 [BI_QUERY] Reativando Filtros: Cidade=$cityName | Periodo=${currentState.period}")

                // 1. Busca Clientes com Normalização de Filtro
                val remoteCustomers = SupabaseClient.client.postgrest["clientes"]
                    .select {
                        if (!isDev && cityName != null) filter { eq("cidade", cityName) }
                        if (filterStartDate != null) filter { gte("date", filterStartDate) }
                        if (filterEndDate != null) filter { lte("date", filterEndDate) }
                    }.decodeList<Customer>()

                // 2. Busca GE com Filtro Temporal (SÊNIOR FIX: Usando a coluna 'data' conforme esquema real)
                val remoteGE = SupabaseClient.client.postgrest["grandes_empreendimentos"]
                    .select {
                        if (!isDev && cityName != null) filter { eq("cidade", cityName) }
                        if (filterStartDate != null) filter { gte("data", filterStartDate) }
                        if (filterEndDate != null) filter { lte("data", filterEndDate) }
                    }.decodeList<EconomyUpdate>()

                Log.d("debugs", "✅ [BI_LIVE] Dados filtrados recebidos: ${remoteCustomers.size} Rec. e ${remoteGE.size} GE.")

                // --- PROCESSAMENTO DOS DADOS (Macro para Micro) ---
                val totalRec = remoteCustomers.size
                val totalGE = remoteGE.size
                
                val avgQual = if (remoteCustomers.isNotEmpty()) {
                    remoteCustomers.sumOf { customer ->
                        when(customer.quality?.lowercase()) {
                            "boa" -> 1.0
                            "regular" -> 0.5
                            else -> 0.1
                        }
                    }.toFloat() / remoteCustomers.size
                } else 1.0f

                val groupDetails = mutableMapOf<String, Pair<Int, Int>>() 
                remoteCustomers.forEach { 
                    val g = it.grupoSugerido ?: "S/G"
                    val current = groupDetails[g] ?: Pair(0, 0)
                    groupDetails[g] = current.copy(first = current.first + 1)
                }
                remoteGE.forEach { 
                    val g = it.grupoSugerido ?: "S/G"
                    val current = groupDetails[g] ?: Pair(0, 0)
                    groupDetails[g] = current.copy(second = current.second + 1)
                }
                
                val statsByGroup = groupDetails.map { (group, counts) ->
                    GroupStats(group, counts.first + counts.second, counts.first, counts.second)
                }.sortedByDescending { it.total }

                val teamMap = mutableMapOf<String, Triple<String, Int, Int>>() 
                remoteCustomers.forEach {
                    val id = it.leituristaId ?: "unknown"
                    val name = it.addedBy ?: "Equipe"
                    val current = teamMap[id] ?: Triple(name, 0, 0)
                    teamMap[id] = current.copy(second = current.second + 1)
                }
                remoteGE.forEach {
                    val id = it.leituristaId ?: "unknown"
                    val name = it.addedBy ?: "Equipe"
                    val current = teamMap[id] ?: Triple(name, 0, 0)
                    teamMap[id] = current.copy(third = current.third + 1)
                }

                val teamPerformance = teamMap.map { (id, data) ->
                    val memberCustomers = remoteCustomers.filter { it.leituristaId == id || it.addedBy == data.first }
                    val memberQual = if (memberCustomers.isNotEmpty()) {
                        memberCustomers.sumOf { customer ->
                            when (customer.quality?.lowercase()) {
                                "boa" -> 100.0
                                "regular" -> 50.0
                                else -> 20.0
                            }
                        }.toFloat() / memberCustomers.size
                    } else 100f

                    TeamMemberStats(id, data.first, data.second, data.third, memberQual / 100f)
                }.sortedByDescending { it.totalRecadastro + it.totalGE }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    cityTotalRecadastro = totalRec,
                    cityTotalGE = totalGE,
                    averageQuality = avgQual,
                    statsByGroup = statsByGroup,
                    teamPerformance = teamPerformance,
                    cityName = cityName ?: "Global"
                )
            } catch (e: Exception) {
                Log.e("debugs", "❌ [BI] Falha ao carregar dashboard: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun getFriendlyCityName(cidadeId: String?): String? {
        return when (cidadeId) {
            "c2be642b-2823-41b9-8f54-0b8c84db9a14" -> "Itapoá"
            "ff9166b8-63b1-4481-a26a-64778181fa08" -> "Guabiruba"
            "74df760a-0120-42b4-bb4d-03cfd92e79b0" -> "Gaivota"
            "93fee74f-6cbb-4638-868d-ef5c17b081a4" -> "Gravatal"
            "9ed90b8c-1b63-44b7-88cd-c2b9b6babcc7" -> "Sombrio"
            else -> null
        }
    }
}
