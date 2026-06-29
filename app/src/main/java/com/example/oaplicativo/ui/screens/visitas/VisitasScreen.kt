@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.visitas

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.data.repository.StatsRepositoryImpl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitasScreen(
    onBack: () -> Unit,
    onNavigateToAdminPanel: () -> Unit // SÊNIOR FIX: Adicionado parâmetro de navegação
) {
    val context = LocalContext.current
    
    // SÊNIOR FIX DEFINITIVO: Fábrica manual para evitar o NoSuchMethodException
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return VisitasViewModel(
                    application = context.applicationContext as Application,
                    repository = StatsRepositoryImpl.getInstance(context.applicationContext as Application)
                ) as T
            }
        }
    }
    
    val viewModel: VisitasViewModel = viewModel(factory = factory)
    val stats by viewModel.stats.collectAsState()

    // SÊNIOR PERF: Monitoramento do Ciclo de Vida para atualização instantânea
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        // Toda vez que a tela voltar ao primeiro plano, as estatísticas são recalculadas
        lifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                viewModel.loadStats()
            }
        })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MEU DESEMPENHO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    val profile by com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance().currentUserProfile.collectAsState()
                    val isPowerUser = profile?.cargo?.lowercase()?.let { 
                        it == "administrador" || it == "desenvolvedor" 
                    } ?: false

                    com.example.oaplicativo.ui.components.GlobalActionMenu(
                        isDarkTheme = false, // TODO
                        isAdmin = isPowerUser,
                        onToggleTheme = { /* TODO */ },
                        onLogout = { /* TODO */ },
                        onNavigateToUserRegistration = { /* TODO */ },
                        onNavigateToAdminPanel = onNavigateToAdminPanel,
                        onForceSync = {
                            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.oaplicativo.data.sync.SyncWorker>().build()
                            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("force_sync_visitas", androidx.work.ExistingWorkPolicy.REPLACE, syncRequest)
                            android.widget.Toast.makeText(context, "Sincronização iniciada!", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.loadStats()
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- HEADER: RECORDE ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EmojiEvents, null, tint = Color.Yellow, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("SEU RECORDE DIÁRIO", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text("${stats.recordePessoal}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                        Text("Cadastros Úteis (Boa/Regular)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            // --- CONTADORES RÁPIDOS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatsMiniCard(
                    modifier = Modifier.weight(1f).height(120.dp),
                    title = "Visitas Hoje",
                    value = stats.hojeTotal.toString(),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = Color(0xFF6366F1)
                )
                StatsMiniCard(
                    modifier = Modifier.weight(1f).height(120.dp),
                    title = "Recorde",
                    value = stats.recordePessoal.toString(),
                    icon = Icons.Default.History,
                    color = Color(0xFFF59E0B)
                )
            }

            // --- RESUMO DE QUALIDADE ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Qualidade das Visitas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Impacto no faturamento e auditoria", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    QualityProgressBar(label = "Qualidade BOA", percentage = stats.percentualBoa, color = Color(0xFF10B981), count = (stats.hojeTotal * stats.percentualBoa).toInt())
                    Spacer(modifier = Modifier.height(16.dp))
                    QualityProgressBar(label = "Qualidade REGULAR", percentage = stats.percentualRegular, color = Color(0xFFF59E0B), count = (stats.hojeTotal * stats.percentualRegular).toInt())
                    Spacer(modifier = Modifier.height(16.dp))
                    QualityProgressBar(label = "Qualidade RUIM", percentage = stats.percentualRuim, color = Color(0xFFEF4444), count = (stats.hojeTotal * stats.percentualRuim).toInt())
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StatsMiniCard(modifier: Modifier, title: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun QualityProgressBar(label: String, percentage: Float, color: Color, count: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
                Text(" • ", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Text("${(percentage * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = color,
            trackColor = color.copy(alpha = 0.1f),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
