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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.ui.components.AsyncDataContainer
import com.example.oaplicativo.util.LocationHelper
import com.example.oaplicativo.util.navigation.NavigationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomyUpdateListScreen(
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (String) -> Unit,
    viewModel: EconomyUpdateViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    
    var searchQuery by remember { mutableStateOf("") }
    var userLocation by remember { mutableStateOf<android.location.Location?>(null) }

    // --- ATUALIZAÇÃO AUTOMÁTICA ---
    // Toda vez que a tela ganha foco, pedimos os dados mais recentes.
    LaunchedEffect(Unit) {
        viewModel.fetchEconomyUpdates()
        userLocation = locationHelper.getCurrentLocation()
    }

    val filteredItems by remember(items, searchQuery, userLocation) {
        derivedStateOf {
            val base = if (searchQuery.isBlank()) items 
                      else items.filter { it.buildingName.contains(searchQuery, true) || it.hdNumber.contains(searchQuery, true) }
            
            if (userLocation != null) {
                base.sortedBy { item ->
                    if (item.latitude != null && item.longitude != null) {
                        locationHelper.calculateDistance(userLocation!!.latitude, userLocation!!.longitude, item.latitude, item.longitude)
                    } else Float.MAX_VALUE
                }.take(10)
            } else base
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ECONOMIAS PREDIAIS", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchEconomyUpdates() }) { Icon(Icons.Default.Refresh, contentDescription = "Atualizar") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Default.Add, contentDescription = "Nova Atualização")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por Edifício ou HD...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )

            PullToRefreshBox(
                isRefreshing = state is EconomyUpdateState.Loading,
                onRefresh = { viewModel.fetchEconomyUpdates() },
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncDataContainer(
                    items = filteredItems,
                    isLoading = state is EconomyUpdateState.Loading,
                    error = (state as? EconomyUpdateState.Error)?.message,
                    onRetry = { viewModel.fetchEconomyUpdates() }
                ) { data: List<EconomyUpdate> ->
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                        items(data) { item ->
                            EconomyItemRow(
                                item = item,
                                userLocation = userLocation,
                                locationHelper = locationHelper,
                                onClick = { item.id?.let { onItemClick(it) } },
                                onNavigate = { NavigationUtils.openNavigation(context, item.latitude, item.longitude) }
                            )
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
    onNavigate: () -> Unit
) {
    val distance = remember(item, userLocation) {
        if (userLocation != null && item.latitude != null && item.longitude != null) {
            val d = locationHelper.calculateDistance(userLocation.latitude, userLocation.longitude, item.latitude, item.longitude)
            locationHelper.formatDistance(d)
        } else null
    }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(item.buildingName, fontWeight = FontWeight.Bold) },
        supportingContent = {
            Column {
                Text("HD: ${item.hdNumber} • ${item.economiesCount} Econ.")
                if (distance != null) {
                    Text("Distância: $distance", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        leadingContent = { 
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(20.dp)) }
            }
        },
        trailingContent = {
            if (item.latitude != null) {
                IconButton(onClick = onNavigate) {
                    Icon(Icons.Default.Navigation, contentDescription = "Navegar", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
