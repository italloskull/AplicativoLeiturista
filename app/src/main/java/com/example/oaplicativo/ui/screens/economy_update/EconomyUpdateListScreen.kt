package com.example.oaplicativo.ui.screens.economy_update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.ui.components.AsyncDataContainer

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
    val isRefreshing = state is EconomyUpdateState.Loading && items.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Economias Prediais") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchEconomyUpdates() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchEconomyUpdates() },
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            AsyncDataContainer(
                items = items,
                isLoading = state is EconomyUpdateState.Loading,
                error = (state as? EconomyUpdateState.Error)?.message,
                onRetry = { viewModel.fetchEconomyUpdates() }
            ) { data: List<EconomyUpdate> ->
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(data) { item: EconomyUpdate ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { item.id?.let { onItemClick(it) } },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Edifício: ${item.buildingName}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "HD: ${item.hdNumber}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Econ. Ativas: ${item.economiesCount}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
