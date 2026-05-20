package com.example.oaplicativo.ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oaplicativo.data.local.LocalDatabase
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.ui.components.GlobalActionMenu
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToRecadastro: () -> Unit,
    onNavigateToEconomias: () -> Unit,
    onNavigateToVisitas: () -> Unit,
    onNavigateToUserRegistration: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val localDb = remember { LocalDatabase(context) }
    
    // ESTADOS DINÂMICOS DAS ESTATÍSTICAS (OTIMIZADOS COM mutableIntStateOf)
    var recadastroPending by remember { mutableIntStateOf(0) }
    var economiasPending by remember { mutableIntStateOf(0) }

    // Atualiza estatísticas ao entrar na tela
    LaunchedEffect(Unit) {
        recadastroPending = localDb.getRecadastroStats().second
        economiasPending = localDb.getEconomyStats().second
    }

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

    val authRepository = AuthRepositoryImpl.getInstance()
    val userProfile by authRepository.currentUserProfile.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "RECADASTRE.IA", 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                actions = {
                    GlobalActionMenu(
                        isDarkTheme = isDarkTheme,
                        isAdmin = userProfile?.isAdmin ?: false,
                        onToggleTheme = onToggleTheme,
                        onLogout = onLogout,
                        onNavigateToUserRegistration = onNavigateToUserRegistration,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            // --- HEADER: WELCOME CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "$greeting,",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Text(
                            text = userProfile?.fullName?.split(" ")?.firstOrNull() ?: "Leiturista",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Text(
                text = "SERVIÇOS DISPONÍVEIS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // --- GRID DE SERVIÇOS ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    DashboardCard(
                        title = "Recadastro Clientes",
                        subtitle = if (recadastroPending > 0) "$recadastroPending Pendentes" else "Sincronizado",
                        statusColor = if (recadastroPending > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                        icon = Icons.Default.AssignmentInd,
                        color = Color(0xFF0052CC),
                        onClick = onNavigateToRecadastro
                    )
                }
                item {
                    DashboardCard(
                        title = "Atualização Economias",
                        subtitle = if (economiasPending > 0) "$economiasPending Pendentes" else "Sincronizado",
                        statusColor = if (economiasPending > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                        icon = Icons.Default.Business,
                        color = Color(0xFF6B7280),
                        onClick = onNavigateToEconomias
                    )
                }
                item {
                    DashboardCard(
                        title = "Minhas Visitas",
                        subtitle = "Meu Desempenho",
                        statusColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Default.History,
                        color = Color(0xFFE11D48),
                        onClick = onNavigateToVisitas
                    )
                }
                item {
                    DashboardCard(
                        title = "Apoio Técnico",
                        subtitle = "Suporte",
                        statusColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = Icons.Default.SupportAgent,
                        color = Color(0xFF10B981),
                        onClick = { }
                    )
                }
            }

            // --- FOOTER ---
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Recadastre.IA • Versão 0.9.2.6.4",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    statusColor: Color,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon, 
                        contentDescription = null, 
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}
