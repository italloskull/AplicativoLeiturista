package com.example.oaplicativo.ui.screens.customer_list

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.data.sync.SyncWorker
import com.example.oaplicativo.presentation.components.DistanceBadge
import com.example.oaplicativo.presentation.components.SyncIndicator
import com.example.oaplicativo.util.navigation.NavigationUtils
import com.example.oaplicativo.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        locationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        while (true) {
            userLocation = locationHelper.getCurrentLocation()
            delay(30000)
        }
    }

    val filteredCustomers by remember(customers, searchQuery, userLocation) {
        derivedStateOf {
            val baseList = if (searchQuery.isBlank()) {
                customers
            } else {
                customers.filter {
                    (it.name?.contains(searchQuery, ignoreCase = true) ?: false) ||
                            (it.registrationNumber?.contains(searchQuery, ignoreCase = true) ?: false)
                }
            }

            val sortedList = if (userLocation != null) {
                baseList.sortedBy { customer ->
                    if (customer.latitude != null && customer.longitude != null) {
                        locationHelper.calculateDistance(
                            userLocation!!.latitude, userLocation!!.longitude,
                            customer.latitude, customer.longitude
                        )
                    } else {
                        Float.MAX_VALUE
                    }
                }
            } else {
                baseList
            }

            sortedList.take(10)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RECADASTRO") },
                navigationIcon = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        scope.launch { userLocation = locationHelper.getCurrentLocation() }
                    }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Minha Localização", tint = if (userLocation != null) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                    }
                    IconButton(onClick = { 
                        // Força a sincronização ao clicar no refresh
                        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
                        WorkManager.getInstance(context).enqueue(syncRequest)
                        viewModel.refreshCustomers() 
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (userProfile?.isAdmin == true) {
                            DropdownMenuItem(
                                text = { Text("Cadastrar Usuário") },
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
            FloatingActionButton(onClick = onAddCustomer) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Cliente")
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshCustomers() },
            modifier = Modifier.padding(paddingValues)
        ) {
            Column {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Pesquisar por nome ou matrícula") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") }
                )
                
                if (filteredCustomers.isNotEmpty()) {
                    Text(
                        text = "Mostrando os 10 clientes mais próximos",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = filteredCustomers,
                        key = { it.id ?: it.registrationNumber ?: it.name ?: "" },
                        contentType = { "customer_item" }
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
                        HorizontalDivider()
                    }
                }
            }
        }
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
    val distance = remember(customer, userLocation) {
        if (userLocation != null && customer.latitude != null && customer.longitude != null) {
            val meters = locationHelper.calculateDistance(
                userLocation.latitude, userLocation.longitude,
                customer.latitude, customer.longitude
            )
            locationHelper.formatDistance(meters)
        } else null
    }

    ListItem(
        headlineContent = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(customer.name ?: "Sem Nome")
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
        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
        trailingContent = {
            if (customer.latitude != null && customer.longitude != null) {
                IconButton(onClick = { onNavigateClick(customer) }) {
                    Icon(
                        imageVector = Icons.Default.Navigation, 
                        contentDescription = "Ir até o local",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = Modifier.clickable { onCustomerClick(customer) }
    )
}
