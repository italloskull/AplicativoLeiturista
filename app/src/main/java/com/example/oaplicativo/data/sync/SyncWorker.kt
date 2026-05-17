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
                // Tenta enviar para o Supabase
                repository.addCustomer(customer)
                
                // Sucesso absoluto: Remove do banco local somente após a confirmação do repositório
                db.deleteSyncedCustomer(localId)
                Log.d("SyncWorker", "Registro $localId sincronizado e removido do cache local.")
            } catch (e: Exception) {
                // Se falhar (ex: timeout, erro 500), mantemos no banco local para a próxima tentativa
                Log.e("SyncWorker", "Falha ao sincronizar registro $localId: ${e.message}. Tentará novamente mais tarde.")
                // Não interrompemos o loop para tentar enviar outros registros pendentes, 
                // mas sinalizamos ao WorkManager para agendar um retry para os que falharam.
            }
        }

        // Verifica se ainda restam pendências após o loop
        val finalPending = try { db.getPendingCustomers() } catch (e: Exception) { emptyList() }
        
        return@withContext if (finalPending.isEmpty()) {
            Result.success()
        } else {
            // Se restarem registros que falharam, o WorkManager tentará novamente conforme a política de backoff
            Result.retry()
        }
    }
}
