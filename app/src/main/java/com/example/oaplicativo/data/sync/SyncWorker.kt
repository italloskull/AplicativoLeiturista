package com.example.oaplicativo.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.domain.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SyncWorker: Responsável pela sincronização robusta e reativa.
 * Revertido para a versão estável que nunca falha no envio.
 */
class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = LocalDatabase(applicationContext)
        val repository: CustomerRepository = CustomerRepositoryImpl.getInstance()
        
        Log.d("SyncWorker", "Iniciando verificação de pendências...")

        val pendingCustomers = try {
            db.getPendingCustomers()
        } catch (e: Exception) {
            return@withContext Result.retry()
        }

        if (pendingCustomers.isEmpty()) return@withContext Result.success()

        for ((localId, customer) in pendingCustomers) {
            try {
                // Tenta enviar (A Trigger no servidor cuidará do carimbo)
                repository.addCustomer(customer)
                
                // Sucesso: Limpa do banco local
                db.deleteSyncedCustomer(localId)
                Log.d("SyncWorker", "Registro $localId enviado com sucesso.")
            } catch (e: Exception) {
                Log.e("SyncWorker", "Erro ao enviar $localId: ${e.message}")
                return@withContext Result.retry()
            }
        }

        // Atualiza a UI local após o ciclo
        try {
            val remainingPending = db.getPendingCustomers().map { it.second }
            repository.updateLocalCustomers(remainingPending)
        } catch (_: Exception) {}

        return@withContext Result.success()
    }
}
