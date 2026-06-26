package com.example.oaplicativo.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.oaplicativo.MainActivity
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.data.repository.EconomyRepositoryImpl
import com.example.oaplicativo.domain.repository.CustomerRepository
import com.example.oaplicativo.domain.repository.EconomyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * SyncWorker: Responsável pela sincronização robusta e resiliente.
 * SÊNIOR DEBUG FIX: Garantia de remoção atômica e relatório de falhas.
 */
class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = LocalDatabase.getInstance(applicationContext)
        val repository: CustomerRepository = CustomerRepositoryImpl.getInstance()
        
        Log.d("SyncWorker", "🤖 Robô de Sincronização: Iniciando...")

        val pendingCustomers = try { db.getPendingCustomers() } catch (e: Exception) { emptyList() }
        val pendingEconomy = try { db.getPendingEconomyUpdates() } catch (e: Exception) { emptyList() }

        if (pendingCustomers.isEmpty() && pendingEconomy.isEmpty()) {
            Log.d("SyncWorker", "✅ Nada pendente em nenhuma fila.")
            return@withContext Result.success()
        }

        Log.d("SyncWorker", "🤖 Robô de Sincronização: Processando ${pendingCustomers.size} clientes e ${pendingEconomy.size} economias...")

        var successCount = 0
        var failCount = 0

        // 1. Sincroniza Clientes (Recadastro) - AGORA COM FALLBACK DE ISOLAMENTO
        if (pendingCustomers.isNotEmpty()) {
            try {
                val customersList = pendingCustomers.map { it.second }
                repository.addCustomers(customersList)
                
                pendingCustomers.forEach { db.deleteSyncedCustomer(it.first) }
                successCount += pendingCustomers.size
                Log.d("SyncWorker", "✅ Lote de ${pendingCustomers.size} clientes enviado.")
            } catch (e: Exception) {
                Log.w("SyncWorker", "⚠️ Falha no lote de clientes. Iniciando decomposição para isolar erro.")
                // SÊNIOR DEBUG FIX: Se o lote falhar, tentamos um por um para não travar a fila inteira
                pendingCustomers.forEach { (localId, customer) ->
                    try {
                        repository.addCustomer(customer)
                        db.deleteSyncedCustomer(localId)
                        successCount++
                    } catch (inner: Exception) {
                        db.incrementSyncAttempt("customers", localId, inner.message)
                        failCount++
                        Log.e("SyncWorker", "❌ Erro fatal no registro $localId: ${inner.message}")
                    }
                }
            }
        }

        // 2. Sincroniza Economias (Atualização Predial) - AGORA COM FALLBACK DE ISOLAMENTO
        val economyRepo: EconomyRepository = EconomyRepositoryImpl.getInstance()
        
        val pendingEconomyReloaded = try { db.getPendingEconomyUpdates() } catch (e: Exception) { emptyList() }
        
        if (pendingEconomyReloaded.isNotEmpty()) {
            try {
                val economyList = pendingEconomyReloaded.map { it.second }
                Log.d("SyncWorker", "🚀 Tentando enviar lote de ${economyList.size} economias.")
                economyRepo.saveEconomyUpdates(economyList)
                
                pendingEconomyReloaded.forEach { db.deleteSyncedEconomyUpdate(it.first) }
                successCount += pendingEconomyReloaded.size
                Log.d("SyncWorker", "✅ Economias enviadas com sucesso.")
            } catch (e: Exception) {
                Log.w("SyncWorker", "⚠️ Falha no lote de economias: ${e.message}. Decompondo...")
                pendingEconomyReloaded.forEach { (localId, item) ->
                    try {
                        Log.d("SyncWorker", "🔍 Tentando registro individual: $localId (HD: ${item.hdNumber})")
                        economyRepo.saveEconomyUpdate(item)
                        db.deleteSyncedEconomyUpdate(localId)
                        successCount++
                        Log.d("SyncWorker", "✅ Sincronizado individualmente: $localId")
                    } catch (inner: Exception) {
                        db.incrementSyncAttempt("grandes_empreendimentos", localId, inner.message)
                        failCount++
                        Log.e("SyncWorker", "❌ Erro fatal na economia $localId: ${inner.message}")
                    }
                }
            }
        }

        Log.i("SyncWorker", "📊 Sincronização Finalizada: $successCount Sucessos, $failCount Falhas.")

        // SÊNIOR REFRESH: Força a atualização dos repositórios globais após o worker
        try {
            CustomerRepositoryImpl.getInstance().fetchCustomers()
            EconomyRepositoryImpl.getInstance().fetchEconomyUpdates()
        } catch (_: Exception) {}

        // SÊNIOR PERF: Se falhou, o retry agora usa política exponencial (evita spam no servidor)
        return@withContext if (failCount == 0) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
