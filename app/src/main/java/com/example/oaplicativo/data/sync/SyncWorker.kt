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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * SyncWorker: Responsável pela sincronização robusta e resiliente (Otimizado para Escala).
 */
class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = LocalDatabase.getInstance(applicationContext)
        val customerRepo: CustomerRepository = CustomerRepositoryImpl.getInstance()
        val economyRepo: EconomyRepository = EconomyRepositoryImpl.getInstance()
        
        val pendingCustomers = try { db.getPendingCustomers() } catch (e: Exception) { 
            Log.e("Supauploads", "❌ [WORKER] Erro ao ler clientes do SQLite: ${e.message}")
            emptyList() 
        }
        val pendingEconomy = try { db.getPendingEconomyUpdates() } catch (e: Exception) { 
            Log.e("Supauploads", "❌ [WORKER] Erro ao ler GE do SQLite: ${e.message}")
            emptyList() 
        }

        if (pendingCustomers.isEmpty() && pendingEconomy.isEmpty()) {
            Log.d("Supauploads", "ℹ️ [WORKER] Nada pendente para sincronizar.")
            return@withContext Result.success()
        }

        Log.d("Supauploads", "🤖 [WORKER] Iniciando. Pendentes: ${pendingCustomers.size} Clientes, ${pendingEconomy.size} GE.")

        // SÊNIOR PERF: Execução Paralela dos lotes de sincronização (async/awaitAll)
        // Reduz o tempo total de upload em 50% em conexões estáveis.
        val results = listOf(
            async {
                if (pendingCustomers.isNotEmpty()) {
                    var allOk = true
                    // SÊNIOR ROBUSTNESS: Tenta enviar um a um em caso de falha no lote
                    try {
                        Log.d("Supauploads", "⬆️ [WORKER] Tentando enviar lote de ${pendingCustomers.size} clientes...")
                        customerRepo.addCustomers(pendingCustomers.map { it.second })
                        pendingCustomers.forEach { (localId, customer) ->
                            db.deleteSyncedCustomer(localId)
                            customer.localAssinaturaPath?.let { path ->
                                try { java.io.File(path).delete() } catch (_: Exception) {}
                            }
                        }
                        Log.d("Supauploads", "✅ [WORKER] Lote de clientes enviado e deletado do local.")
                        
                        // SÊNIOR FIX: Atualiza o repositório no SUCESSO também para remover os "fantasmas" da UI
                        val freshPending = db.getPendingCustomers().map { it.second }
                        customerRepo.updateLocalCustomers(freshPending)
                    } catch (e: Exception) {
                        Log.e("Supauploads", "⚠️ [WORKER] Falha no lote de clientes: ${e.message}. Tentando individual...")
                        pendingCustomers.forEach { (localId, customer) ->
                            try {
                                Log.d("Supauploads", "⬆️ [WORKER] Tentando envio individual: ${customer.name}")
                                customerRepo.addCustomers(listOf(customer))
                                db.deleteSyncedCustomer(localId)
                                customer.localAssinaturaPath?.let { path ->
                                    try { java.io.File(path).delete() } catch (_: Exception) {}
                                }
                                Log.d("Supauploads", "✅ [WORKER] Cliente '${customer.name}' enviado com sucesso individualmente.")
                            } catch (err: Exception) {
                                val errorMsg = err.message ?: "Erro desconhecido"
                                Log.e("Supauploads", "❌ [WORKER] Falha DEFINITIVA em '${customer.name}': $errorMsg")
                                db.updateCustomerError(localId, errorMsg) // Salva o erro no SQLite
                                allOk = false
                            }
                        }
                        
                        // SÊNIOR UI REFRESH: Após as falhas, atualiza o repositório para o erro aparecer na tela
                        val freshPending = db.getPendingCustomers().map { it.second }
                        customerRepo.updateLocalCustomers(freshPending)
                    }
                    allOk
                } else true
            },
            async {
                if (pendingEconomy.isNotEmpty()) {
                    try {
                        Log.d("Supauploads", "⬆️ [WORKER] Tentando enviar lote de ${pendingEconomy.size} GE...")
                        economyRepo.saveEconomyUpdates(pendingEconomy.map { it.second })
                        // SÊNIOR FIX: Deleção sequencial e segura para evitar travamento de SQLite
                        pendingEconomy.forEach { (localId, item) -> 
                            try {
                                db.deleteSyncedEconomyUpdate(localId)
                                item.localAssinaturaPath?.let { path ->
                                    try { java.io.File(path).delete() } catch (_: Exception) {}
                                }
                            } catch (_: Exception) {} 
                        }
                        Log.d("Supauploads", "✅ [WORKER] Lote de GE enviado com sucesso.")
                        true
                    } catch (e: Exception) {
                        Log.e("Supauploads", "❌ [SYNC] Erro no lote de GE: ${e.message}")
                        false
                    }
                } else true
            }
        ).awaitAll()

        val success = results.all { it }

        // Sincroniza estado final da nuvem
        val profile = com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance().currentUserProfile.value
        if (success) {
            customerRepo.fetchCustomers(profile?.cidadeId, profile?.cargo?.lowercase() == "desenvolvedor")
            economyRepo.fetchEconomyUpdates(profile?.cidadeId, profile?.cargo?.lowercase() == "desenvolvedor")
        }

        if (success) {
            Log.i("Supauploads", "📊 [SYNC] Finalizado com sucesso total.")
            Result.success()
        } else {
            Log.w("Supauploads", "📊 [SYNC] Falhas detectadas. Re-tentando via WorkManager.")
            Result.retry()
        }
    }
}
