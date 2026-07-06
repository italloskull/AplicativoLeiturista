package com.example.oaplicativo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import androidx.work.*
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.ui.navigation.SetupNavGraph
import com.example.oaplicativo.ui.theme.OAplicativoTheme
import java.util.concurrent.TimeUnit

@Composable
fun SanitationApp() {
    val context = LocalContext.current
    
    // SÊNIOR FIX: Inicialização segura de repositórios globais com o Contexto da Aplicação
    // Isso evita que os repositórios fiquem orfãos se a Activity for recriada.
    LaunchedEffect(Unit) {
        com.example.oaplicativo.data.repository.EconomyRepositoryImpl.getInstance().initialize(context)
        com.example.oaplicativo.data.repository.CustomerRepositoryImpl.getInstance().initialize(context)
    }

    val navController = rememberNavController()
    
    // GESTÃO DE TEMA GLOBAL (Ref. Item 4 das Diretrizes)
    val isDarkTheme = remember { mutableStateOf(false) }

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

    OAplicativoTheme(darkTheme = isDarkTheme.value) {
        SetupNavGraph(
            navController = navController,
            isDarkTheme = isDarkTheme.value,
            onToggleTheme = { isDarkTheme.value = !isDarkTheme.value }
        )
    }
}
