package com.example.oaplicativo.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onBack: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val authorizedCities by viewModel.authorizedCities.collectAsState()
    val selectedCityFilter by viewModel.selectedCityFilter.collectAsState()
    var showCityMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PAINEL GESTOR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        
                        if (authorizedCities.size > 1) {
                            Box {
                                TextButton(
                                    onClick = { showCityMenu = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = selectedCityFilter?.nome?.uppercase() ?: "TODAS AS CIDADES", 
                                            style = MaterialTheme.typography.labelSmall, 
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown, 
                                            null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                DropdownMenu(expanded = showCityMenu, onDismissRequest = { showCityMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("🌎 TODAS AS CIDADES") },
                                        onClick = {
                                            viewModel.selectCityFilter(null)
                                            showCityMenu = false
                                        }
                                    )
                                    authorizedCities.forEach { cidade ->
                                        DropdownMenuItem(
                                            text = { Text(cidade.nome.uppercase()) },
                                            onClick = {
                                                viewModel.selectCityFilter(cidade)
                                                showCityMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else if (state.cityName.isNotEmpty()) {
                            Text(state.cityName.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f))
                        )
                    ),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    PeriodSelector(
                        currentPeriod = state.period,
                        onPeriodSelected = { viewModel.setPeriod(it) }
                    )
                }

                item {
                    Text("RESUMO GERAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            label = "Recadastros",
                            value = state.cityTotalRecadastro.toString(),
                            icon = Icons.Default.AssignmentInd,
                            color = Color(0xFF6366F1)
                        )
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            label = "Grandes Emp.",
                            value = state.cityTotalGE.toString(),
                            icon = Icons.Default.Business,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                if (state.statsByGroup.isNotEmpty()) {
                    item {
                        Text("DOMÍNIO POR GRUPO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                state.statsByGroup.take(10).forEach { group ->
                                    GroupProgressBar(
                                        group = group.group,
                                        count = group.total,
                                        total = state.cityTotalRecadastro + state.cityTotalGE,
                                        recCount = group.recadastroCount,
                                        geCount = group.geCount
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("EQUIPE DE CAMPO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                if (state.teamPerformance.isEmpty()) {
                    item {
                        Text("Nenhuma atividade registrada na equipe hoje.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(state.teamPerformance) { member ->
                        TeamMemberCard(member)
                    }
                }
                
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun PeriodSelector(currentPeriod: DashboardPeriod, onPeriodSelected: (DashboardPeriod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    val label = when(currentPeriod) {
        DashboardPeriod.TODAY -> "Hoje"
        DashboardPeriod.LAST_7_DAYS -> "Últimos 7 dias"
        DashboardPeriod.LAST_30_DAYS -> "Últimos 30 dias"
        DashboardPeriod.LAST_YEAR -> "Último ano"
        DashboardPeriod.CUSTOM -> "Período Personalizado"
    }

    Column {
        Text("FILTRAR PERÍODO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box {
            Surface(
                onClick = { expanded = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                    Icon(Icons.Default.ExpandMore, null)
                }
            }
            
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Hoje") }, onClick = { onPeriodSelected(DashboardPeriod.TODAY); expanded = false })
                DropdownMenuItem(text = { Text("Últimos 7 dias") }, onClick = { onPeriodSelected(DashboardPeriod.LAST_7_DAYS); expanded = false })
                DropdownMenuItem(text = { Text("Últimos 30 dias") }, onClick = { onPeriodSelected(DashboardPeriod.LAST_30_DAYS); expanded = false })
                DropdownMenuItem(text = { Text("Último ano") }, onClick = { onPeriodSelected(DashboardPeriod.LAST_YEAR); expanded = false })
            }
        }
    }
}

@Composable
fun KpiCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GroupProgressBar(group: String, count: Int, total: Int, recCount: Int, geCount: Int) {
    val progress = if (total > 0) count.toFloat() / total else 0f
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Grupo $group", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.End) {
                Text("$recCount Recadastros, $geCount Grandes Empreendimentos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Total: $count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun TeamMemberCard(member: TeamMemberStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(member.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(12.dp), tint = Color(0xFFF59E0B))
                    Spacer(Modifier.width(4.dp))
                    Text("Qualidade: ${(member.averageQuality * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${member.totalRecadastro + member.totalGE}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("Visitas", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
