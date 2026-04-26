package com.example.oaplicativo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.ui.navigation.SetupNavGraph

@Composable
fun SanitationApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Toda vez que o app for aberto ou recomposto no nível raiz,
    // tentamos disparar o SyncWorker para limpar pendências.
    LaunchedEffect(Unit) {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    SetupNavGraph(navController = navController)
}