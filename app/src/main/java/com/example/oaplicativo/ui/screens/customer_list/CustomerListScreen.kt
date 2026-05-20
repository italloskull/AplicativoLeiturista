@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.customer_list

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.presentation.components.DistanceBadge
import com.example.oaplicativo.presentation.components.SyncIndicator
import com.example.oaplicativo.ui.components.AppStatusBadge
import com.example.oaplicativo.ui.components.GlobalActionMenu
import com.example.oaplicativo.util.navigation.NavigationUtils
import com.example.oaplicativo.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * CUSTOMER LIST SCREEN
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAddCustomer: () -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onNavigateToUserRegistration: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CustomerListViewModel = viewModel(factory = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current?.let {
        androidx.lifecycle.viewmodel.viewModelFactory {
            addInitializer(CustomerListViewModel::class) {
                CustomerListViewModel(application = (this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application))
            }
        }
    } ?: androidx.lifecycle.viewmodel.viewModelFactory { })
) {
    val customers by viewModel.customers.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState() // Restaurado
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember(context) { LocationHelper(context) }
    
    val userLocationState = remember { mutableStateOf<android.location.Location?>(null) }
    val searchQueryState = remember { mutableStateOf("") }
    val showAboutDialogState = remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            scope.launch {
                userLocationState.value = locationHelper.getCurrentLocation()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        while (true) {
            val newLocation = locationHelper.getCurrentLocation()
            if (newLocation != null && (userLocationState.value == null || 
                locationHelper.calculateDistance(userLocationState.value!!.latitude, userLocationState.value!!.longitude, newLocation.latitude, newLocation.longitude) > 10)) {
                userLocationState.value = newLocation
            }
            delay(60000)
        }
    }

    val filteredCustomers by remember(customers, searchQueryState.value, userLocationState.value) {
        derivedStateOf {
            val query = searchQueryState.value.trim()
            
            // 1. FILTRAGEM INICIAL (Pesquisa ou Todos)
            val baseList = if (query.isBlank()) {
                customers
            } else {
                customers.filter {
                    (it.name?.contains(query, ignoreCase = true) == true) ||
                            (it.registrationNumber?.contains(query, ignoreCase = true) == true)
                }
            }

            // 2. INTELIGÊNCIA GEOGRÁFICA
            if (userLocationState.value != null) {
                val sortedList = baseList.sortedBy { customer ->
                    if (customer.latitude != null && customer.longitude != null) {
                        locationHelper.calculateDistance(
                            userLocationState.value!!.latitude, userLocationState.value!!.longitude,
                            customer.latitude, customer.longitude
                        )
                    } else {
                        Float.MAX_VALUE
                    }
                }
                
                // REGRA DE OURO: Mostrar apenas 10 se não estiver pesquisando
                if (query.isBlank()) {
                    sortedList.take(10)
                } else {
                    sortedList
                }
            } else {
                // Fallback se GPS estiver desligado: mostrar primeiros 20
                baseList.take(20)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recadastro de Clientes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCustomers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                    GlobalActionMenu(
                        isDarkTheme = isDarkTheme,
                        isAdmin = userProfile?.isAdmin ?: false,
                        onToggleTheme = onToggleTheme,
                        onLogout = onLogout,
                        onNavigateToUserRegistration = onNavigateToUserRegistration,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCustomer,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Cliente")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQueryState.value,
                onValueChange = { searchQueryState.value = it },
                placeholder = { Text("Pesquisar por nome ou matrícula...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refreshCustomers() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // SOLUÇÃO SÊNIOR: Fallback de chave para evitar conflito de IDs vazios/nulos
                    items(
                        items = filteredCustomers,
                        key = { customer -> 
                            customer.id ?: "temp_${filteredCustomers.indexOf(customer)}_${System.currentTimeMillis()}" 
                        }
                    ) { customer ->
                        CustomerListItem(
                            customer = customer,
                            userLocation = userLocationState.value,
                            locationHelper = locationHelper,
                            onCustomerClick = onCustomerClick,
                            onNavigateClick = { NavigationUtils.openNavigation(context, it.latitude, it.longitude) }
                        )
                    }
                }
            }
        }
    }

    if (showAboutDialogState.value) {
        AlertDialog(
            onDismissRequest = { showAboutDialogState.value = false },
            title = { Text("Sobre o Sistema") },
            text = { Text("Recadastre.IA\nVersão 0.9.2.6.4\nSistema de Saneamento em Campo.") },
            confirmButton = {
                TextButton(onClick = { showAboutDialogState.value = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CustomerListItem(
    customer: Customer,
    userLocation: android.location.Location?,
    locationHelper: LocationHelper,
    onCustomerClick: (Customer) -> Unit,
    onNavigateClick: (Customer) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onCustomerClick(customer) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name ?: "Sem Nome",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Matrícula: ${customer.registrationNumber ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    AppStatusBadge(customer.quality)
                    
                    if (userLocation != null && customer.latitude != null && customer.longitude != null) {
                        val dist = locationHelper.calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            customer.latitude, customer.longitude
                        )
                        Spacer(Modifier.width(8.dp))
                        DistanceBadge(String.format("%.1fm", dist))
                    }
                    
                    Spacer(Modifier.weight(1f))
                    SyncIndicator(customer.isSynced)
                }
            }

            IconButton(onClick = { onNavigateClick(customer) }) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = "Navegar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
