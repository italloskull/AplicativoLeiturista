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
            val currentState = _uiState.value

            try {
                // SÊNIOR COMPAT FIX: Padronização de TimeZone para BI
                val brFormatter = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                val brTimeZone = java.util.TimeZone.getTimeZone("America/Sao_Paulo")
                brFormatter.timeZone = brTimeZone
                
                val calendar = java.util.Calendar.getInstance(brTimeZone)
                val nowStr = brFormatter.format(calendar.time)
                
                val filterStartDate = when(currentState.period) {
                    DashboardPeriod.TODAY -> nowStr
                    DashboardPeriod.LAST_7_DAYS -> {
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
                        brFormatter.format(calendar.time)
                    }
                    DashboardPeriod.LAST_30_DAYS -> {
                        calendar.add(java.util.Calendar.DAY_OF_YEAR, -30)
                        brFormatter.format(calendar.time)
                    }
                    DashboardPeriod.LAST_YEAR -> {
                        calendar.add(java.util.Calendar.YEAR, -1)
                        brFormatter.format(calendar.time)
                    }
                    DashboardPeriod.CUSTOM -> currentState.startDate?.take(10)?.replace("-", "/")
                }
                val filterEndDate = if (currentState.period == DashboardPeriod.CUSTOM) currentState.endDate?.take(10)?.replace("-", "/") else null

                val cityNameFilter = _selectedCityFilter.value?.nome
                Log.d("debugs", "📊 [BI_QUERY] Filtro Ativado: $filterStartDate até ${filterEndDate ?: "Hoje"} | CidadeFilter: $cityNameFilter")

                val authorizedCities = authRepo.getUserCities()
                val cityNames = authorizedCities.map { it.nome }
                val currentUid = authRepo.currentUserProfile.value?.id
                Log.d("debugs", "🆔 [BI_SECURITY] Meu UID Local: $currentUid")
                Log.d("debugs", "🎯 [BI_QUERY] Chaveiro do Gestor: $cityNames")

                // SÊNIOR BI PAGINATION: Busca exaustiva para superar o limite de 1000 do Supabase
                val remoteCustomers = mutableListOf<Customer>()
                var offset = 0L
                val pageSize = 1000L
                
                while (true) {
                    val chunk = SupabaseClient.client.postgrest["clientes"].select {
                        if (cityNameFilter != null) filter { eq("cidade", cityNameFilter) }
                        else if (!isDev) {
                            val targetCities = cityNames.ifEmpty { listOf("BLOCK_ALL_ACCESS") }
                            filter { or { targetCities.forEach { eq("cidade", it) } } }
                        }
                        if (filterStartDate != null) filter { gte("date", filterStartDate) }
                        if (filterEndDate != null) filter { lte("date", filterEndDate) }
                        
                        range(offset, offset + pageSize - 1)
                    }.decodeList<Customer>()
                    
                    remoteCustomers.addAll(chunk)
                    if (chunk.size < pageSize) break
                    offset += pageSize
                    if (offset > 50000) break // Segurança contra loop infinito
                }

                // SÊNIOR BI PAGINATION: Grandes Empreendimentos
                val remoteGE = mutableListOf<EconomyUpdate>()
                offset = 0L
                while (true) {
                    val chunk = SupabaseClient.client.postgrest["grandes_empreendimentos"].select {
                        if (cityNameFilter != null) filter { eq("cidade", cityNameFilter) }
                        else if (!isDev) {
                            val targetCities = cityNames.ifEmpty { listOf("BLOCK_ALL_ACCESS") }
                            filter { or { targetCities.forEach { eq("cidade", it) } } }
                        }
                        if (filterStartDate != null) filter { gte("data", filterStartDate) }
                        if (filterEndDate != null) filter { lte("data", filterEndDate) }
                        
                        range(offset, offset + pageSize - 1)
                    }.decodeList<EconomyUpdate>()
                    
                    remoteGE.addAll(chunk)
                    if (chunk.size < pageSize) break
                    offset += pageSize
                    if (offset > 10000) break
                }

                Log.d("debugs", "✅ [BI_LIVE] Total Real Capturado: ${remoteCustomers.size} Rec. e ${remoteGE.size} GE.")

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
                    cityName = cityNameFilter ?: com.example.oaplicativo.util.CityUtils.getFriendlyCityName(user?.cidadeId) ?: "Global"
                )

                // SÊNIOR BI AUDIT: Investigação profunda dos registros Sem Grupo (S/G)
                val sgItems = remoteCustomers.filter { it.grupoSugerido.isNullOrBlank() }
                if (sgItems.isNotEmpty()) {
                    Log.d("debugs", "🕵️‍♂️ [SG_AUDIT] Analisando os ${sgItems.size} registros Sem Grupo em Itapoá:")
                    sgItems.forEach { 
                        val gpsStatus = if (it.latitude != null) "${it.latitude}, ${it.longitude}" else "SEM GPS"
                        Log.d("debugs", "   📍 Matrícula: ${it.registrationNumber} | Rua: ${it.logradouro} | GPS: $gpsStatus")
                    }
                }
            } catch (e: Exception) {
                Log.e("debugs", "❌ [BI] Falha ao carregar dashboard: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
