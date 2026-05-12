package com.example.oaplicativo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.ui.navigation.SetupNavGraph
import java.util.concurrent.TimeUnit

@Composable
fun SanitationApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Adicionado a TAG "SyncWorkerTag" para a tela poder vigiar o progresso
        val immediateSync = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag("SyncWorkerTag")
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.REPLACE,
            immediateSync
        )

        val periodicSync = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .addTag("SyncWorkerTag")
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSync
        )
    }

    SetupNavGraph(navController = navController)
}
