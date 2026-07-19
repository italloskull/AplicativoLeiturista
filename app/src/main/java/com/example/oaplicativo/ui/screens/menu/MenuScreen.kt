@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.menu

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.ui.components.GlobalActionMenu
import com.example.oaplicativo.util.NetworkUtils
import kotlinx.coroutines.delay

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

    LaunchedEffect(Unit) {
        authRepository.loadProfileFromCache(context)
        if (userProfile == null) {
            authRepository.fetchProfile()
        }

        while (true) {
            val pendingRec = localDb.getPendingCustomers().size
            val pendingEcon = localDb.getPendingEconomyUpdates().size
            recadastroPending = pendingRec
            economiasPending = pendingEcon
            
            val waitTime = if (recadastroPending + economiasPending > 0) 3000L else 10000L
            delay(waitTime)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface
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
                        text = "Informações Atualizadas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                
                val isPowerUser = userProfile?.cargo?.lowercase()?.let { 
                    it == "administrador" || it == "desenvolvedor" 
                } ?: false

                GlobalActionMenu(
                    isDarkTheme = isDarkTheme,
                    isAdmin = isPowerUser,
                    onToggleTheme = onToggleTheme,
                    onLogout = onLogout,
                    onNavigateToUserRegistration = { onNavigate("user_registration") },
                    onNavigateToAdminPanel = { onNavigate("admin_dashboard") },
                    onNavigateToUserManagement = { onNavigate("user_management") },
                    onForceSync = {
                        val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.oaplicativo.data.sync.SyncWorker>().build()
                        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("force_sync_menu", androidx.work.ExistingWorkPolicy.REPLACE, syncRequest)
                        Toast.makeText(context, "Sincronização iniciada!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(Modifier.height(40.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("Boa noite,", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                        Text(userProfile?.fullName?.split(" ")?.firstOrNull() ?: userProfile?.username ?: "Leiturista", 
                             style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("SERVIÇOS EM CAMPO", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuCard(
                    modifier = Modifier.weight(1f),
                    title = "Recadastro",
                    subtitle = if (recadastroPending > 0) "$recadastroPending pendentes" else "Sincronizado",
                    icon = Icons.Default.AssignmentInd,
                    color = if (recadastroPending > 0) Color(0xFFF59E0B) else Color(0xFF10B981)
                ) { onNavigate("customer_list") }
                
                MenuCard(
                    modifier = Modifier.weight(1f),
                    title = "Grandes Emp.",
                    subtitle = if (economiasPending > 0) "$economiasPending pendentes" else "Sincronizado",
                    icon = Icons.Default.Business,
                    color = if (economiasPending > 0) Color(0xFFF59E0B) else Color(0xFF10B981)
                ) { onNavigate("economy_update_list") }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MenuCard(
                    modifier = Modifier.weight(1f),
                    title = "Visitas",
                    subtitle = "Produtividade",
                    icon = Icons.Default.BarChart,
                    color = Color(0xFF6366F1)
                ) { onNavigate("visitas") }
                
                MenuCard(
                    modifier = Modifier.weight(1f),
                    title = "Suporte",
                    subtitle = "Apoio Técnico",
                    icon = Icons.Default.HeadsetMic,
                    color = Color(0xFFEC4899)
                ) { 
                    Toast.makeText(context, "Suporte acionado!", Toast.LENGTH_SHORT).show()
                }
            }
            
            Spacer(Modifier.height(32.dp))

            // SÊNIOR BI UI: ZONA DE STATUS DE SINCRONISMO (REPOSICIONADA)
            if (recadastroPending + economiasPending > 0) {
                val isOnline = NetworkUtils.isInternetAvailable(context)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isOnline) Icons.Default.CloudUpload else Icons.Default.CloudOff, 
                                null, 
                                tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = if (isOnline) "Pendências prontas para envio" else "Sem conexão com a internet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.oaplicativo.data.sync.SyncWorker>().build()
                                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("force_sync_footer", androidx.work.ExistingWorkPolicy.REPLACE, syncRequest)
                                Toast.makeText(context, "Sincronizando com o banco de dados... 🚀", Toast.LENGTH_LONG).show()
                            },
                            enabled = isOnline,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Text(if (isOnline) "SINCRONIZAR COM O BANCO" else "AGUARDANDO INTERNET...", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Text("Recadastre.IA • v0.9.2.7.1", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun MenuCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(160.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
