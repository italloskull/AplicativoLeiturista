package com.example.oaplicativo.data.repository

import android.content.Context
import android.util.Log
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.domain.repository.StatsRepository
import com.example.oaplicativo.ui.screens.visitas.VisitasStats
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class StatsRepositoryImpl private constructor(context: Context) : StatsRepository {
    private val db = LocalDatabase.getInstance(context)
    private val _stats = MutableStateFlow(VisitasStats())
    override val stats: StateFlow<VisitasStats> = _stats.asStateFlow()

    override suspend fun refreshStats(cidadeId: String?, isAdmin: Boolean) {
        try {
            // SÊNIOR BI FIX: Busca dados da NUVEM para o dashboard do leiturista
            // Resolve o problema de dados deletados localmente após o sincronismo.
            val profile = AuthRepositoryImpl.getInstance().currentUserProfile.value
            val userCityName = getFriendlyCityName(cidadeId)
            val today = ZonedDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            
            Log.d("debugs", "📊 [STATS] Sincronizando dashboard via Nuvem ($today)")

            // 1. Busca Clientes do dia na Nuvem
            val remoteCustomers = com.example.oaplicativo.data.SupabaseClient.client.postgrest["clientes"]
                .select {
                    filter { eq("date", today) }
                    if (userCityName != null) filter { eq("cidade", userCityName) }
                    // Se não for admin global, vê apenas o seu próprio trabalho
                    if (!isAdmin) filter { eq("adicionado_por", profile?.fullName ?: profile?.username ?: "") }
                }.decodeList<com.example.oaplicativo.model.Customer>()

            // 2. Busca GE do dia na Nuvem
            val remoteGE = com.example.oaplicativo.data.SupabaseClient.client.postgrest["grandes_empreendimentos"]
                .select {
                    filter { eq("data", today) } // SÊNIOR FIX: Padronizado para 'data' conforme print oficial GE
                    if (userCityName != null) filter { eq("cidade", userCityName) }
                    if (!isAdmin) filter { eq("adicionado_por", profile?.fullName ?: profile?.username ?: "") }
                }.decodeList<com.example.oaplicativo.model.EconomyUpdate>()

            val totalHoje = remoteCustomers.size + remoteGE.size
            
            // 3. Processamento de Qualidade
            val countBoa = remoteCustomers.count { it.quality?.lowercase() == "boa" } + remoteGE.size
            val countRegular = remoteCustomers.count { it.quality?.lowercase() == "regular" }
            val countRuim = remoteCustomers.count { it.quality?.lowercase() == "ruim" }

            val percentBoa = if (totalHoje > 0) countBoa.toFloat() / totalHoje else 0f
            val percentRegular = if (totalHoje > 0) countRegular.toFloat() / totalHoje else 0f
            val percentRuim = if (totalHoje > 0) countRuim.toFloat() / totalHoje else 0f

            // Sincroniza recorde pessoal (Persistência local)
            db.updateRecordIfHigher(countBoa + countRegular)
            val record = db.getPersonalRecord()

            _stats.value = VisitasStats(
                hojeTotal = totalHoje,
                recordePessoal = record,
                percentualBoa = percentBoa,
                percentualRegular = percentRegular,
                percentualRuim = percentRuim,
                totalHistorico = totalHoje
            )
            Log.d("debugs", "✅ [STATS] Dashboard atualizado: $totalHoje visitas registradas.")
        } catch (e: Exception) {
            Log.e("debugs", "❌ [STATS] Erro ao carregar: ${e.message}")
            // Fallback para o que estiver no SQLite no momento (pendentes)
            val local = db.getTodayStats(cidadeId, isAdmin)
            val total = local["Total"] ?: 0
            if (total > 0) {
                _stats.value = VisitasStats(hojeTotal = total, totalHistorico = total)
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

    companion object {
        @Volatile
        private var instance: StatsRepositoryImpl? = null
        fun getInstance(context: Context): StatsRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: StatsRepositoryImpl(context.applicationContext).also { instance = it }
            }
        }
    }
}
