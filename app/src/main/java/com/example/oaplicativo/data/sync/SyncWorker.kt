package com.example.oaplicativo.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.data.repository.EconomyRepositoryImpl
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.domain.repository.EconomyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SyncWorker: Responsável pela sincronização robusta e resiliente.
 */
class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = LocalDatabase.getInstance(applicationContext)
        val customerRepo: CustomerRepository = CustomerRepositoryImpl.getInstance()
        val economyRepo: EconomyRepository = EconomyRepositoryImpl.getInstance()
        
        val pendingCustomers = try { db.getPendingCustomers() } catch (_: Exception) { emptyList() }
        val pendingEconomy = try { db.getPendingEconomyUpdates() } catch (_: Exception) { emptyList() }

        if (pendingCustomers.isEmpty() && pendingEconomy.isEmpty()) return@withContext Result.success()

        Log.d("debugs", "🤖 [SYNC] Robô iniciado. Pendentes: ${pendingCustomers.size} Clientes, ${pendingEconomy.size} GE.")

        var successCount = 0
        var failCount = 0

        // 1. Sincroniza Clientes (Recadastro)
        if (pendingCustomers.isNotEmpty()) {
            try {
                customerRepo.addCustomers(pendingCustomers.map { it.second })
                pendingCustomers.forEach { db.deleteSyncedCustomer(it.first) }
                successCount += pendingCustomers.size
                
                // SÊNIOR FIX: Força a limpeza imediata do cache de pendências no repositório
                val updatedPending = db.getPendingCustomers().map { it.second }
                customerRepo.updateLocalCustomers(updatedPending)
                
                Log.d("debugs", "✅ [SYNC] Clientes enviados e cache limpo.")
            } catch (e: Exception) {
                Log.e("debugs", "❌ [SYNC] Erro no lote de clientes: ${e.message}")
                failCount++
            }
        }

        // 2. Sincroniza Economias (Grandes Empreendimentos)
        if (pendingEconomy.isNotEmpty()) {
            try {
                economyRepo.saveEconomyUpdates(pendingEconomy.map { it.second })
                pendingEconomy.forEach { db.deleteSyncedEconomyUpdate(it.first) }
                successCount += pendingEconomy.size
                
                // SÊNIOR FIX: Força a limpeza imediata do cache de pendências no repositório de economia
                val updatedGE = db.getPendingEconomyUpdates().map { it.second }
                economyRepo.updateLocalEconomyUpdates(updatedGE)
                
                Log.d("debugs", "✅ [SYNC] GE enviados e cache limpo.")
            } catch (e: Exception) {
                Log.e("debugs", "❌ [SYNC] Erro no lote de GE: ${e.message}")
                failCount++
            }
        }

        // Notifica UI do sucesso final baixando os dados atualizados
        val profile = com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance().currentUserProfile.value
        customerRepo.fetchCustomers(profile?.cidadeId, profile?.cargo?.lowercase() == "desenvolvedor")
        economyRepo.fetchEconomyUpdates(profile?.cidadeId, profile?.cargo?.lowercase() == "desenvolvedor")

        if (failCount == 0) {
            Log.i("debugs", "📊 [SYNC] Finalizado com sucesso absoluto ($successCount itens).")
            Result.success()
        } else {
            Log.w("debugs", "📊 [SYNC] Finalizado com falhas parciais. Re-tentando mais tarde.")
            Result.retry()
        }
    }
}
