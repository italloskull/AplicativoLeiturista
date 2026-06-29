package com.example.oaplicativo.ui.screens.economy_update

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.presentation.components.SyncIndicator
import com.example.oaplicativo.ui.components.AsyncDataContainer
import com.example.oaplicativo.util.LocationHelper
import com.example.oaplicativo.util.navigation.NavigationUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomyUpdateListScreen(
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onNavigateToAdminPanel: () -> Unit, // SÊNIOR FIX: Adicionado parâmetro de navegação
    viewModel: EconomyUpdateViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    
    var searchQuery by remember { mutableStateOf("") }
    var userLocation by remember { mutableStateOf<android.location.Location?>(null) }

    // SÊNIOR FIX: Monitoramento do Ciclo de Vida para atualização instantânea ao voltar do formulário
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                viewModel.refreshData()
                
                val fineLoc = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                if (fineLoc == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    scope.launch { userLocation = locationHelper.getCurrentLocation() }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    val filteredItems by remember(items, searchQuery, userLocation) {
        derivedStateOf {
            val base = if (searchQuery.isBlank()) items 
                      else items.filter { (it.buildingName?.contains(searchQuery, true) == true) || (it.hdNumber?.contains(searchQuery, true) == true) }
            
            val currentLoc = userLocation
            if (currentLoc != null) {
                base.sortedBy { item ->
                    if (item.latitude != null && item.longitude != null) {
                        locationHelper.calculateDistance(currentLoc.latitude, currentLoc.longitude, item.latitude, item.longitude)
                    } else Float.MAX_VALUE
                }.take(10)
            } else base
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GRANDES EMPREENDIMENTOS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    val profile by com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance().currentUserProfile.collectAsState()
                    val isPowerUser = profile?.cargo?.lowercase()?.let { 
                        it == "administrador" || it == "desenvolvedor" 
                    } ?: false

                    com.example.oaplicativo.ui.components.GlobalActionMenu(
                        isDarkTheme = false, // TODO: Propagar via NavGraph
                        isAdmin = isPowerUser,
                        onToggleTheme = { /* TODO */ },
                        onLogout = { /* TODO */ },
                        onNavigateToUserRegistration = { /* TODO */ },
                        onNavigateToAdminPanel = onNavigateToAdminPanel,
                        onForceSync = {
                            viewModel.forceSyncAll()
                            viewModel.refreshData()
                            android.widget.Toast.makeText(context, "Robô de Sincronização acionado! 🤖", android.widget.Toast.LENGTH_SHORT).show()
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
                    val dist = locationHelper.calculateDistance(userLocation.latitude, userLocation.longitude, item.latitude, item.longitude)
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
