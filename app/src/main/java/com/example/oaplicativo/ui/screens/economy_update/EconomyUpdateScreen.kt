package com.example.oaplicativo.ui.screens.economy_update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.ui.components.AppFormTextField
import com.example.oaplicativo.ui.components.LoadingActionButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EconomyUpdateScreen(
    itemId: String? = null,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: EconomyUpdateViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val existingItem = remember(itemId, items) { itemId?.let { id -> items.find { it.id == id } } }

    var hdNumber by remember(existingItem) { mutableStateOf(existingItem?.hdNumber ?: "") }
    var buildingName by remember(existingItem) { mutableStateOf(existingItem?.buildingName ?: "") }
    var constructionCompany by remember(existingItem) { mutableStateOf(existingItem?.constructionCompany ?: "") }
    var economiesCount by remember(existingItem) { mutableStateOf(existingItem?.economiesCount?.toString() ?: "") }
    var floorsCount by remember(existingItem) { mutableStateOf(existingItem?.floorsCount?.toString() ?: "") }
    var electricityMeter by remember(existingItem) { mutableStateOf(existingItem?.electricityMeterNumber ?: "") }

    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is EconomyUpdateState.Success) {
            onSaveSuccess()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null) "Nova Atualização Predial" else "Editar Atualização Predial") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AppFormTextField(
                value = hdNumber,
                onValueChange = { hdNumber = it },
                label = "Número HD"
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFormTextField(
                value = buildingName,
                onValueChange = { buildingName = it },
                label = "Nome Edifício"
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFormTextField(
                value = constructionCompany,
                onValueChange = { constructionCompany = it },
                label = "Nome da Construtora"
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFormTextField(
                value = economiesCount,
                onValueChange = { if (it.all { c -> c.isDigit() }) economiesCount = it },
                label = "Economias Ativas",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFormTextField(
                value = floorsCount,
                onValueChange = { if (it.all { c -> c.isDigit() }) floorsCount = it },
                label = "Número de Pavimentos",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFormTextField(
                value = electricityMeter,
                onValueChange = { electricityMeter = it },
                label = "Quantidade de padrões de luz"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (state is EconomyUpdateState.Error) {
                Text(
                    text = (state as EconomyUpdateState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            LoadingActionButton(
                text = "Salvar",
                onClick = {
                    val economies = economiesCount.toIntOrNull() ?: 0
                    val floors = floorsCount.toIntOrNull() ?: 0
                    viewModel.saveEconomyUpdate(
                        EconomyUpdate(
                            id = existingItem?.id,
                            hdNumber = hdNumber,
                            buildingName = buildingName,
                            constructionCompany = constructionCompany,
                            economiesCount = economies,
                            floorsCount = floors,
                            electricityMeterNumber = electricityMeter
                        )
                    )
                },
                isLoading = state is EconomyUpdateState.Loading,
                enabled = hdNumber.isNotEmpty()
            )
        }
    }
}