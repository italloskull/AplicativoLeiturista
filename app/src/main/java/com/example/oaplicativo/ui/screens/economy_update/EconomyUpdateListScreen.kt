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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomyUpdateListScreen(
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
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

    // SÊNIOR PERF: Filtro inteligente via Sequence para Grandes Empreendimentos
    val filteredItems by remember(items, searchQuery, selectedCityFilter) {
        derivedStateOf {
            val query = searchQuery.trim().lowercase()
            val selectedCityName = selectedCityFilter?.nome?.lowercase()?.trim()

            items.asSequence().filter { item ->
                val matchesSearch = if (query.isBlank()) true 
                                   else (item.buildingName?.lowercase()?.contains(query) == true || 
                                         item.hdNumber?.contains(query) == true)
                
                val customerCity = item.cidade?.lowercase()?.trim()
                val matchesCity = if (selectedCityName == null) true
                                 else (customerCity == selectedCityName || 
                                       customerCity?.replace("á", "a") == selectedCityName.replace("á", "a"))
                
                matchesSearch && matchesCity
            }.take(100).toList()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text(
                            text = "G. EMPREENDIMENTOS", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        
                        var showCityMenu by remember { mutableStateOf(false) }
                        val profile by com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance().currentUserProfile.collectAsState()
                        val isPowerUser = profile?.cargo?.lowercase()?.let { it == "administrador" || it == "desenvolvedor" } ?: false

                        if (isPowerUser && authorizedCities.size > 1) {
                            Box(modifier = Modifier.padding(start = 4.dp)) {
                                TextButton(
                                    onClick = { showCityMenu = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = selectedCityFilter?.nome?.uppercase() ?: "TODAS", 
                                            style = MaterialTheme.typography.labelSmall, 
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                        Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(expanded = showCityMenu, onDismissRequest = { showCityMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("🌎 TODAS AS CIDADES") },
                                        onClick = { viewModel.selectCityFilter(null); showCityMenu = false }
                                    )
                                    authorizedCities.forEach { cidade ->
                                        DropdownMenuItem(
                                            text = { Text(cidade.nome.uppercase()) },
                                            onClick = { viewModel.selectCityFilter(cidade); showCityMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    val profile by com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance().currentUserProfile.collectAsState()
                    val isPowerUser = profile?.cargo?.lowercase()?.let { it == "administrador" || it == "desenvolvedor" } ?: false

                    GlobalActionMenu(
                        isDarkTheme = false,
                        isAdmin = isPowerUser,
                        onToggleTheme = { },
                        onLogout = { },
                        onNavigateToAdminPanel = onNavigateToAdminPanel,
                        onNavigateToUserManagement = onNavigateToUserManagement,
                        onForceSync = {
                            viewModel.forceSyncAll()
                            android.widget.Toast.makeText(context, "Sincronização iniciada!", android.widget.Toast.LENGTH_SHORT).show()
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
                isLoading = state is EconomyUpdateState.Loading && items.isEmpty(),
                onRetry = { viewModel.fetchEconomyUpdates() },
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(item.buildingName ?: "Sem Nome", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("HD: ${item.hdNumber ?: "Vazio"} • ${item.economiesCount ?: 0} Econ.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                if (item.latitude != null && item.longitude != null && userLocation != null) {
                    val dist = remember(item.latitude, item.longitude, userLocation) {
                        locationHelper.calculateDistance(userLocation.latitude, userLocation.longitude, item.latitude, item.longitude)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(4.dp))
                            Text(locationHelper.formatDistance(dist), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                SyncIndicator(isSynced = item.isSynced)
            }
            
            IconButton(onClick = onNavClick) {
                Icon(Icons.Default.Navigation, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
