@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.customer_list

import android.app.Application
import android.location.Location
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import com.example.oaplicativo.data.repository.CustomerRepositoryImpl
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.model.Cidade
import com.example.oaplicativo.presentation.components.SyncIndicator
import com.example.oaplicativo.ui.components.AppStatusBadge
import com.example.oaplicativo.ui.components.AsyncDataContainer
import com.example.oaplicativo.ui.components.GlobalActionMenu
import com.example.oaplicativo.util.LocationHelper
import com.example.oaplicativo.util.navigation.NavigationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onAddCustomer: () -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onLogout: () -> Unit,
    onToggleTheme: () -> Unit,
    onNavigateToUserRegistration: () -> Unit,
    onNavigateToAdminPanel: () -> Unit,
    onNavigateToUserManagement: () -> Unit
) {
    val context = LocalContext.current
    
    val factory = remember {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CustomerListViewModel(
                    application = context.applicationContext as Application,
                    customerRepository = CustomerRepositoryImpl.getInstance(),
                    authRepository = AuthRepositoryImpl.getInstance()
                ) as T
            }
        }
    }
    
    val viewModel: CustomerListViewModel = viewModel(factory = factory)
    
    val customers by viewModel.customers.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val authorizedCities by viewModel.authorizedCities.collectAsState()
    val selectedCityFilter by viewModel.selectedCityFilter.collectAsState()
    var showCityMenu by remember { mutableStateOf(false) }

    val authRepository = remember { AuthRepositoryImpl.getInstance() }
    val userProfile by authRepository.currentUserProfile.collectAsState()
    
    val isPowerUser = remember(userProfile) {
        userProfile?.cargo?.lowercase()?.let { 
            it == "administrador" || it == "desenvolvedor" 
        } ?: false
    }

    val locationHelper = remember { LocationHelper(context) }
    val searchQueryState = remember { mutableStateOf("") }
    val userLocationState = remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
        // SÊNIOR REATIVITY FIX: Forçamos a carga de cidades autorizadas na abertura da tela
        // Isso resolve o problema da lista vir vazia no primeiro clique
        viewModel.refreshCustomers() 

        val fineLoc = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLoc == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            userLocationState.value = locationHelper.getCurrentLocation()
        }
    }

    val filteredCustomers by remember(customers, searchQueryState.value, userLocationState.value, selectedCityFilter) {
        derivedStateOf {
            val query = searchQueryState.value.trim().lowercase()
            val selectedCityName = selectedCityFilter?.nome?.lowercase()?.trim()
            
            val baseList = customers.filter { customer ->
                // 1. Filtro de Busca (Nome ou Matrícula)
                val matchesSearch = if (query.isBlank()) true 
                                   else (customer.name?.lowercase()?.contains(query) == true || 
                                         customer.registrationNumber?.contains(query) == true)
                
                // 2. Filtro Regional (UI Select) - SÊNIOR FIX: Normalização para evitar sumiço por acento
                val customerCity = customer.cidade?.lowercase()?.trim()
                val matchesCity = if (selectedCityName == null) true
                                 else (customerCity == selectedCityName || 
                                       customerCity?.replace("á", "a") == selectedCityName.replace("á", "a"))
                
                matchesSearch && matchesCity
            }
            
            // SÊNIOR SMART SEARCH TRIGGER: Se a busca local falhar, dispara busca remota automática
            if (query.length >= 4 && baseList.isEmpty() && !isRefreshing) {
                viewModel.searchRemote(query)
            }
            
            val currentLoc = userLocationState.value
            if (currentLoc != null) {
                val sortedList = baseList.sortedBy { customer ->
                    if (customer.latitude != null && customer.longitude != null) {
                        locationHelper.calculateDistance(currentLoc.latitude, currentLoc.longitude, customer.latitude, customer.longitude)
                    } else Float.MAX_VALUE
                }
                if (query.isBlank()) sortedList.take(15) else sortedList
            } else baseList.take(30)
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
                            text = "Lista de Clientes", 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Black
                        )
                        
                        // SÊNIOR BI SELECTOR: Menu de cidades autorizadas (Apenas para Admin+)
                        // SÊNIOR FIX: Liberamos o seletor para Admin mesmo que a lista tenha apenas 1 cidade, para permitir o "TODAS"
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
                            // Se for leiturista comum, mostra apenas o nome da cidade fixo
                            val rawCity = authorizedCities.firstOrNull()?.nome ?: userProfile?.cidadeId ?: ""
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
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    GlobalActionMenu(
                        isDarkTheme = isDarkTheme,
                        isAdmin = isPowerUser,
                        onToggleTheme = onToggleTheme,
                        onLogout = onLogout,
                        onNavigateToUserRegistration = onNavigateToUserRegistration,
                        onNavigateToAdminPanel = onNavigateToAdminPanel,
                        onNavigateToUserManagement = onNavigateToUserManagement,
                        onForceSync = {
                            val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.oaplicativo.data.sync.SyncWorker>().build()
                            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("force_sync_list", androidx.work.ExistingWorkPolicy.REPLACE, syncRequest)
                            Toast.makeText(context, "Sincronização iniciada!", Toast.LENGTH_SHORT).show()
                            viewModel.refreshCustomers()
                        }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCustomer, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                Icon(Icons.Default.Add, contentDescription = "Novo Recadastro")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            OutlinedTextField(
                value = searchQueryState.value,
                onValueChange = { searchQueryState.value = it },
                placeholder = { Text("Buscar por Nome ou Matrícula...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )

            AsyncDataContainer(
                items = filteredCustomers,
                isLoading = isRefreshing && filteredCustomers.isEmpty(),
                error = null,
                onRetry = { viewModel.refreshCustomers() }
            ) { data ->
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(items = data, key = { it.id ?: it.hashCode() }) { customer ->
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
}

@Composable
fun CustomerListItem(
    customer: Customer,
    userLocation: Location?,
    locationHelper: LocationHelper,
    onCustomerClick: (Customer) -> Unit,
    onNavigateClick: (Customer) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onCustomerClick(customer) },
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
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
                    text = "Matrícula: ${customer.registrationNumber ?: "0"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppStatusBadge(customer.quality)
                    Spacer(Modifier.width(8.dp))
                    
                    val distLabel = remember(userLocation, customer) {
                        if (userLocation != null && customer.latitude != null && customer.longitude != null) {
                            val d = locationHelper.calculateDistance(userLocation.latitude, userLocation.longitude, customer.latitude, customer.longitude)
                            locationHelper.formatDistance(d)
                        } else null
                    }
                    
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
                    SyncIndicator(customer.isSynced)
                }
            }

            IconButton(onClick = { onNavigateClick(customer) }) {
                Icon(Icons.Default.Navigation, contentDescription = "Navegar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
