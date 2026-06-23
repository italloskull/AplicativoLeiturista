@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.menu

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.ui.components.GlobalActionMenu
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

@Composable
fun MenuScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localDb = remember { LocalDatabase.getInstance(context) }
    val authRepository = remember { AuthRepositoryImpl.getInstance() }
    val userProfile by authRepository.currentUserProfile.collectAsState()

    var recadastroPending by remember { mutableIntStateOf(0) }
    var economiasPending by remember { mutableIntStateOf(0) }

    // SÊNIOR PERF: Centralizamos o monitoramento em um único loop inteligente
    LaunchedEffect(Unit) {
        // Carregamento de Perfil
        authRepository.loadProfileFromCache(context)
        if (userProfile == null) {
            authRepository.fetchProfile()
        }

        // Loop de Monitoramento de Pendências
        while (true) {
            val recStats = localDb.getRecadastroStats()
            val econStats = localDb.getEconomyStats()
            recadastroPending = recStats.second
            economiasPending = econStats.second
            
            // Frequência adaptativa: 3s se houver pendência, 10s se estiver limpo
            val waitTime = if (recadastroPending + economiasPending > 0) 3000L else 10000L
            delay(waitTime)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface // SÊNIOR FIX: Fundo sólido branco (ou surface)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // --- TOP BAR INTEGRADA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "RECADASTRE.IA",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Gestão de Saneamento",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                GlobalActionMenu(
                    isDarkTheme = isDarkTheme,
                    isAdmin = userProfile?.cargo == "administrador",
                    onToggleTheme = onToggleTheme,
                    onLogout = onLogout,
                    onNavigateToUserRegistration = { onNavigate("user_registration") },
                    onForceSync = {
                        localDb.resetSyncAttempts()
                        val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.oaplicativo.data.sync.SyncWorker>().build()
                        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("force_sync_manual", androidx.work.ExistingWorkPolicy.REPLACE, syncRequest)
                        Toast.makeText(context, "Sincronização iniciada!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(Modifier.height(40.dp))

            // --- CARD DE BOAS-VINDAS ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Reduzido para fundo branco
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        val saudacao = when (LocalTime.now().hour) {
                            in 5..11 -> "Bom dia"
                            in 12..17 -> "Boa tarde"
                            else -> "Boa noite"
                        }
                        Text(saudacao, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = userProfile?.fullName?.split(" ")?.firstOrNull() ?: "Leiturista",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            Text(
                text = "SERVIÇOS EM CAMPO",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.Start).padding(start = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            // --- GRID DE SERVIÇOS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuCard(
                    modifier = Modifier.weight(1f),
                    title = "Recadastro",
                    subtitle = if (recadastroPending > 0) "$recadastroPending Pendentes" else "Sincronizado",
                    icon = Icons.Default.AssignmentInd,
                    color = if (recadastroPending > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                    onClick = { onNavigate("customer_list") }
                )
                MenuCard(
                    modifier = Modifier.weight(1f),
                    title = "Economias",
                    subtitle = if (economiasPending > 0) "$economiasPending Pendentes" else "Sincronizado",
                    icon = Icons.Default.Business,
                    color = if (economiasPending > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                    onClick = { onNavigate("economy_update_list") }
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuCard(
                    modifier = Modifier.weight(1f),
                    title = "Visitas",
                    subtitle = "Produtividade",
                    icon = Icons.Default.BarChart,
                    color = Color(0xFF6366F1),
                    onClick = { onNavigate("visitas") }
                )
                MenuCard(
                    modifier = Modifier.weight(1f),
                    title = "Suporte",
                    subtitle = "Apoio Técnico",
                    icon = Icons.Default.HeadsetMic,
                    color = Color(0xFFEC4899),
                    onClick = { /* Suporte */ }
                )
            }

            Spacer(Modifier.height(64.dp))

            // --- FOOTER DINÂMICO ---
            if (recadastroPending + economiasPending > 0) {
                var isSyncingManual by remember { mutableStateOf(false) }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "⚠️ ${recadastroPending + economiasPending} Pendências",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = {
                            isSyncingManual = true
                            scope.launch {
                                localDb.resetSyncAttempts()
                                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.oaplicativo.data.sync.SyncWorker>().build()
                                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("force_sync_${System.currentTimeMillis()}", androidx.work.ExistingWorkPolicy.REPLACE, syncRequest)
                                repeat(5) { delay(2000); recadastroPending = localDb.getRecadastroStats().second; economiasPending = localDb.getEconomyStats().second }
                                isSyncingManual = false
                            }
                        }
                    ) {
                        if (isSyncingManual) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Sincronizando...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Text("Sincronizar Agora", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "Recadastre.IA • v${com.example.oaplicativo.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun MenuCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(150.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = MaterialTheme.shapes.large,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(10.dp), tint = color.copy(alpha = 0.6f))
                }
            }
        }
    }
}
