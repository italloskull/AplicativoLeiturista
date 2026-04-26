package com.example.oaplicativo.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.domain.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * SyncWorker: Responsável pela sincronização atômica e resiliente entre local e nuvem.
 */
class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        val db = LocalDatabase(applicationContext)
        val repository: CustomerRepository = CustomerRepositoryImpl.getInstance()
        
        Log.d("SyncWorker", "Iniciando verificação de itens pendentes...")

        val pendingCustomers = try {
            db.getPendingCustomers()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Erro ao acessar DB local: ${e.message}")
            return@withContext ListenableWorker.Result.retry()
        }

        if (pendingCustomers.isEmpty()) {
            Log.d("SyncWorker", "Fila vazia. Nada para sincronizar.")
            return@withContext ListenableWorker.Result.success()
        }

        Log.d("SyncWorker", "Processando ${pendingCustomers.size} registros...")

        var totalSuccess = 0
        var totalFails = 0

        for ((localId, customer) in pendingCustomers) {
            try {
                if (customer.registrationNumber.isNullOrBlank()) continue

                val syncTimestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val syncedCustomer = customer.copy(syncedAt = syncTimestamp)
                
                // Tenta enviar para o servidor
                repository.addCustomer(syncedCustomer)
                
                // SUCESSO: Remove do banco local IMEDIATAMENTE
                db.deleteSyncedCustomer(localId)
                totalSuccess++
                Log.d("SyncWorker", "Item $localId sincronizado com sucesso.")
                
            } catch (e: Exception) {
                Log.e("SyncWorker", "Falha no item $localId: ${e.message}")
                totalFails++
            }
        }

        // Força a UI a atualizar a lista local (removendo os itens que acabamos de deletar)
        val remainingPending = db.getPendingCustomers().map { it.second }
        repository.updateLocalCustomers(remainingPending)

        Log.d("SyncWorker", "Finalizado. Sucesso: $totalSuccess | Falhas: $totalFails")
        
        return@withContext if (totalFails == 0) {
            ListenableWorker.Result.success()
        } else {
            ListenableWorker.Result.retry()
        }
    }
}