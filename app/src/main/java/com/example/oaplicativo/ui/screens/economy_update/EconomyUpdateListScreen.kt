@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.economy_update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.presentation.components.SyncIndicator
import com.example.oaplicativo.ui.components.AsyncDataContainer
import com.example.oaplicativo.ui.components.GlobalActionMenu
import com.example.oaplicativo.util.LocationHelper
import com.example.oaplicativo.util.navigation.NavigationUtils
import com.example.oaplicativo.util.removeAccents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomyUpdateListScreen(
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit,
    onNavigateToUserRegistration: () -> Unit,
    onNavigateToAdminPanel: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    viewModel: EconomyUpdateViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val items by viewModel.items.collectAsState()
    val state by viewModel.state.collectAsState()
    val authorizedCities by viewModel.authorizedCities.collectAsState()
    val selectedCityFilter by viewModel.selectedCityFilter.collectAsState()
    
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    var userLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                viewModel.refreshData()
                scope.launch {
                    try {
                        userLocation = locationHelper.getCurrentLocation()
                    } catch (_: Exception) { }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // SÊNIOR PERF: Separamos a filtragem (texto/cidade) da ordenação (distância)
    val baseFilteredItems by remember(items, searchQuery, selectedCityFilter) {
        derivedStateOf {
            val queryRaw = searchQuery.trim().lowercase()
            val queryNormalized = queryRaw.removeAccents()
            val selectedCityName = selectedCityFilter?.nome?.lowercase()?.trim()

            items.asSequence().filter { item ->
                val matchesSearch = if (queryRaw.isBlank()) true 
                                   else (item.buildingName?.lowercase()?.removeAccents()?.contains(queryNormalized) == true || 
                                         item.hdNumber?.contains(queryRaw) == true)
                
                val customerCity = item.cidade?.lowercase()?.trim()
                val matchesCity = if (selectedCityName == null) true
                                 else (customerCity == selectedCityName || 
                                       customerCity?.removeAccents() == selectedCityName.removeAccents())
                
                matchesSearch && matchesCity
            }.toList()
        }
    }

    val filteredItems by remember(baseFilteredItems, userLocation) {
        derivedStateOf {
            val currentLoc = userLocation
            val resultList = if (currentLoc != null) {
                baseFilteredItems.sortedBy { item ->
                    if (item.latitude != null && item.longitude != null) {
                        // SÊNIOR PERF: Cálculo direto de distância para ordenação rápida
                        val latDiff = item.latitude - currentLoc.latitude
                        val lngDiff = item.longitude - currentLoc.longitude
                        latDiff * latDiff + lngDiff * lngDiff
                    } else Double.MAX_VALUE
                }
            } else baseFilteredItems

            // Se não estiver buscando, limita a 50 para performance. Se estiver buscando, mostra tudo.
            if (searchQuery.isBlank()) resultList.take(50) else resultList
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "EMPREENDIMENTOS", // SÊNIOR FIX: Nome mais curto e sem weight para evitar erro de layout "G."
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                        
                        var showCityMenu by remember { mutableStateOf(false) }
                        val authRepo = remember { com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance() }
                        val profile by authRepo.currentUserProfile.collectAsState()
                        val isPowerUser = profile?.isPowerUser ?: false

                        if (isPowerUser) {
                            Box(modifier = Modifier.padding(start = 8.dp)) {
                                TextButton(
                                    onClick = { showCityMenu = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = selectedCityFilter?.nome?.uppercase() ?: "TODAS", 
                                            style = MaterialTheme.typography.labelSmall, 
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown, 
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = showCityMenu, 
                                    onDismissRequest = { showCityMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("🌎 TODAS AS CIDADES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.selectCityFilter(null)
                                            showCityMenu = false
                                        }
                                    )
                                    authorizedCities.forEach { cidade ->
                                        DropdownMenuItem(
                                            text = { Text(cidade.nome.uppercase(), style = MaterialTheme.typography.labelMedium) },
                                            onClick = {
                                                viewModel.selectCityFilter(cidade)
                                                showCityMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // SÊNIOR FIX: Mostra a cidade mesmo se tiver apenas 1 (consistência com lista de clientes)
                            val rawCity = authorizedCities.firstOrNull()?.nome ?: profile?.cidadeId ?: ""
                            val cleanCityName = when(rawCity) {
                                "c2be642b-2823-41b9-8f54-0b8c84db9a14" -> "Itapoá"
                                "ff9166b8-63b1-4481-a26a-64778181fa08" -> "Guabiruba"
                                "74df760a-0120-42b4-bb4d-03cfd92e79b0" -> "Gaivota"
                                "93fee74f-6cbb-4638-868d-ef5c17b081a4" -> "Gravatal"
                                "9ed90b8c-1b63-44b7-88cd-c2b9b6babcc7" -> "Sombrio"
                                else -> rawCity
                            }

                            if (cleanCityName.isNotEmpty()) {
                                Text(
                                    text = " • ${cleanCityName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    val authRepo = remember { com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance() }
                    val profile by authRepo.currentUserProfile.collectAsState()
                    val isPowerUser = profile?.isPowerUser ?: false

                    GlobalActionMenu(
                        isDarkTheme = isDarkTheme,
                        isAdmin = isPowerUser,
                        onToggleTheme = onToggleTheme,
                        onLogout = onLogout,
                        onNavigateToUserRegistration = onNavigateToUserRegistration,
                        onNavigateToAdminPanel = onNavigateToAdminPanel,
                        onNavigateToUserManagement = onNavigateToUserManagement,
                        onForceSync = {
                            viewModel.forceSyncAll()
                            android.widget.Toast.makeText(context, "Sincronização iniciada manualmente!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Buscar por Edifício ou HD...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = MaterialTheme.shapes.medium
            )

            AsyncDataContainer(
                items = filteredItems,
                isLoading = state is EconomyUpdateState.Loading,
                onRetry = { viewModel.fetchEconomyUpdates() },
                onRefresh = { viewModel.refreshData() },
                error = if (state is EconomyUpdateState.Error) (state as EconomyUpdateState.Error).message else null
            ) { list ->
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(list, key = { it.id ?: "" }) { item ->
                        EconomyItemRow(item, userLocation, locationHelper, { onItemClick(item.id ?: "") }) {
                            if (item.latitude != null && item.longitude != null) {
                                NavigationUtils.openNavigation(context, item.latitude, item.longitude)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EconomyItemRow(
    item: EconomyUpdate,
    userLocation: android.location.Location?,
    locationHelper: LocationHelper,
    onClick: () -> Unit,
    onNavClick: () -> Unit
) {
    // SÊNIOR PERF: Cache de cálculos pesados para evitar lag no scroll
    val distLabel = remember(userLocation, item.latitude, item.longitude) {
        if (userLocation != null && item.latitude != null && item.longitude != null) {
            val d = locationHelper.calculateDistance(userLocation.latitude, userLocation.longitude, item.latitude, item.longitude)
            locationHelper.formatDistance(d)
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = (item.buildingName ?: "Sem Nome").uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "HD: ${item.hdNumber ?: "Vazio"} • ${item.economiesCount ?: 0} Econ.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge de Sincronismo local apenas para feedback visual imediato
                    if (distLabel != null) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(4.dp))
                                Text(text = distLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    
                    Spacer(Modifier.weight(1f))
                    SyncIndicator(isSynced = item.isSynced)
                }
            }

            IconButton(onClick = onNavClick) {
                Icon(Icons.Default.Navigation, contentDescription = "Navegar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
