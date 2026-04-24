package com.example.oaplicativo.ui.screens.customer_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    onAddCustomer: () -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onNavigateToUserRegistration: () -> Unit,
    onLogout: () -> Unit,
    viewModel: CustomerListViewModel = viewModel()
) {
    val customers by viewModel.customers.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCustomers by remember {
        derivedStateOf {
            customers.filter {
                (it.name?.contains(searchQuery, ignoreCase = true) ?: false) ||
                (it.registrationNumber?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Sobre o Aplicativo") },
            text = {
                Column {
                    Text("Recadastre.IA")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Versão instalada: ${com.example.oaplicativo.BuildConfig.VERSION_NAME}")
                    Text("Desenvolvido por Mathey e Itallo")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clientes Recadastrados") },
                actions = {
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
        Column(modifier = Modifier.padding(paddingValues)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Pesquisar por nome ou matrícula") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") }
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // Espaço para o FAB não cobrir o último item
            ) {
                items(
                    items = filteredCustomers,
                    key = { it.id ?: it.registrationNumber ?: "" } // Key para melhor performance em listas
                ) { customer ->
                    ListItem(
                        headlineContent = { Text(customer.name ?: "Sem Nome") },
                        supportingContent = { Text("Matrícula: ${customer.registrationNumber ?: "N/A"}") },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.clickable { onCustomerClick(customer) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}