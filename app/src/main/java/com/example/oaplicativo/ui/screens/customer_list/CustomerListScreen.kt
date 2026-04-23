package com.example.oaplicativo.ui.screens.customer_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.Customer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    onAddCustomer: () -> Unit,
    onCustomerClick: (Customer) -> Unit,
    onNavigateToUserRegistration: () -> Unit,
    viewModel: CustomerListViewModel = viewModel()
) {
    val customers by viewModel.customers.collectAsState()
    val userProfile by viewModel.currentUserProfile.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clientes") },
                actions = {
                    if (userProfile?.isAdmin == true) {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cadastrar Usuário") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToUserRegistration()
                                }
                            )
                        }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(customers) { customer ->
                ListItem(
                    headlineContent = { Text(customer.name) },
                    supportingContent = { Text("Matrícula: ${customer.registrationNumber}") },
                    modifier = Modifier.clickable { onCustomerClick(customer) }
                )
                HorizontalDivider()
            }
        }
    }
}