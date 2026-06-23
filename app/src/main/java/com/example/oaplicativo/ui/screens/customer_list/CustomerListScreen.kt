@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.customer_list

import android.app.Application
import android.location.Location
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
    onNavigateToUserRegistration: () -> Unit
) {
    val context = LocalContext.current
    
    // SÊNIOR FIX DEFINITIVO: Fábrica manual para evitar o NoSuchMethodException
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
    val locationHelper = remember { LocationHelper(context) }
    val authRepository = remember { AuthRepositoryImpl.getInstance() }
    val userProfile by authRepository.currentUserProfile.collectAsState()
    
    val searchQueryState = remember { mutableStateOf("") }
    val userLocationState = remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
        val fineLoc = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLoc == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            userLocationState.value = locationHelper.getCurrentLocation()
        }
    }

    val filteredCustomers by remember(customers, searchQueryState.value, userLocationState.value) {
        derivedStateOf {
            val query = searchQueryState.value
            val baseList = if (query.isBlank()) customers 
                          else customers.filter { it.name?.contains(query, true) == true || it.registrationNumber?.contains(query, true) == true }
            
            val currentLoc = userLocationState.value
            if (currentLoc != null) {
                val sortedList = baseList.sortedBy { customer ->
                    if (customer.latitude != null && customer.longitude != null) {
                        locationHelper.calculateDistance(currentLoc.latitude, currentLoc.longitude, customer.latitude, customer.longitude)
                    } else Float.MAX_VALUE
                }
                if (query.isBlank()) sortedList.take(10) else sortedList
            } else baseList.take(20)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Clientes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshCustomers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                    GlobalActionMenu(
                        isDarkTheme = isDarkTheme,
                        isAdmin = userProfile?.cargo == "administrador",
                        onToggleTheme = onToggleTheme,
                        onLogout = onLogout,
                        onNavigateToUserRegistration = onNavigateToUserRegistration,
                        tint = MaterialTheme.colorScheme.onSurface
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
