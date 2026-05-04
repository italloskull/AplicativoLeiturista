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
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.oaplicativo.util.navigation.NavigationUtils
import com.example.oaplicativo.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🚀 OTIMIZAÇÃO DE PERFORMANCE: CustomerListScreen
 * Estratégia: Memoização de filtragem e re-composição inteligente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
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
    val userProfile by viewModel.currentUserProfile.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    
    var userLocation by remember { mutableStateOf<android.location.Location?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            scope.launch {
                userLocation = locationHelper.getCurrentLocation()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        while (true) {
            userLocation = locationHelper.getCurrentLocation()
            delay(30000)
        }
    }

    // 🔥 OTIMIZAÇÃO 1: Memoização da lista filtrada via derivedStateOf
    // Evita recalcular o sort de GPS e o filtro de texto em cada frame de animação.
    val filteredCustomers by remember {
        derivedStateOf {
            val baseList = if (searchQuery.isBlank()) {
                customers
            } else {
                customers.filter {
                    (it.name?.contains(searchQuery, ignoreCase = true) == true) ||
                            (it.registrationNumber?.contains(searchQuery, ignoreCase = true) == true)
                }
            }

            if (userLocation != null) {
                baseList.sortedBy { customer ->
                    if (customer.latitude != null && customer.longitude != null) {
                        locationHelper.calculateDistance(
                            userLocation!!.latitude, userLocation!!.longitude,
                            customer.latitude, customer.longitude
                        )
                    } else {
                        Float.MAX_VALUE
                    }
                }.take(10)
            } else {
                baseList.take(10)
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
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (userProfile?.isAdmin == true) {
                            DropdownMenuItem(
                                text = { Text("Gerenciar Usuários") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToUserRegistration()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Sobre") },
                            onClick = {
                                showMenu = false
                                showAboutDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sair") },
                            onClick = {
                                showMenu = false
                                onLogout()
                            }
                        )
                    }
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
                value = searchQuery,
                onValueChange = { searchQuery = it },
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
                if (filteredCustomers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum registro encontrado.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        item {
                            Text(
                                text = "Clientes Próximos (Top 10)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        // 🔥 OTIMIZAÇÃO 2: Uso de Keys estáveis
                        // Evita que o Compose re-desenhe a lista inteira quando apenas 1 item muda.
                        items(
                            items = filteredCustomers,
                            key = { it.id ?: it.registrationNumber ?: it.name ?: "" }
                        ) { customer ->
                            CustomerListItem(
                                customer = customer,
                                userLocation = userLocation,
                                locationHelper = locationHelper,
                                onCustomerClick = onCustomerClick,
                                onNavigateClick = {
                                    NavigationUtils.openNavigation(context, it.latitude, it.longitude)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Sobre o Aplicativo") },
            text = {
                Column {
                    Text("Recadastre.IA", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Versão: ${com.example.oaplicativo.BuildConfig.VERSION_NAME}")
                    Text("Engenharia: Mathey e Itallo")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Fechar")
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
    // 🔥 OTIMIZAÇÃO 3: Memoização de cálculos internos do item
    val distance = remember(customer.id, userLocation) {
        if (userLocation != null && customer.latitude != null && customer.longitude != null) {
            val meters = locationHelper.calculateDistance(
                userLocation.latitude, userLocation.longitude,
                customer.latitude, customer.longitude
            )
            locationHelper.formatDistance(meters)
        } else null
    }

    ListItem(
        modifier = Modifier.clickable { onCustomerClick(customer) },
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(customer.name ?: "Titular não identificado", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                SyncIndicator(isSynced = customer.isSynced)
            }
        },
        supportingContent = { 
            Column {
                Text("Matrícula: ${customer.registrationNumber ?: "N/A"}")
                if (distance != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DistanceBadge(distance = distance)
                }
            }
        },
        leadingContent = { 
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        trailingContent = {
            if (customer.latitude != null && customer.longitude != null) {
                IconButton(onClick = { onNavigateClick(customer) }) {
                    Icon(
                        imageVector = Icons.Default.Navigation, 
                        contentDescription = "Traçar rota",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
}
