package com.example.oaplicativo.ui.screens.economy_update

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.EconomyUpdate
import com.example.oaplicativo.ui.components.AppTextField
import com.example.oaplicativo.ui.components.AppButton
import com.example.oaplicativo.ui.components.AppCard
import com.example.oaplicativo.util.LocationHelper
import kotlinx.coroutines.launch

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
    
    var latitude by remember(existingItem) { mutableStateOf(existingItem?.latitude) }
    var longitude by remember(existingItem) { mutableStateOf(existingItem?.longitude) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    val state by viewModel.state.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch {
                val loc = locationHelper.getCurrentLocation()
                latitude = loc?.latitude
                longitude = loc?.longitude
            }
        }
    }

    LaunchedEffect(state) {
        if (state is EconomyUpdateState.Success) {
            onSaveSuccess()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GRANDES EMPREENDIMENTOS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state is EconomyUpdateState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (state as EconomyUpdateState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            AppCard(title = "Empreendimento", icon = Icons.Default.Business) {
                AppTextField(
                    value = buildingName, 
                    onValueChange = { buildingName = it }, 
                    label = "Nome do Edifício / Condomínio",
                    leadingIcon = Icons.Default.Apartment
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = constructionCompany, 
                    onValueChange = { constructionCompany = it }, 
                    label = "Empresa Construtora",
                    leadingIcon = Icons.Default.CorporateFare
                )
            }

            AppCard(title = "Técnico", icon = Icons.Default.SettingsInputComponent) {
                AppTextField(
                    value = hdNumber, 
                    onValueChange = { hdNumber = it }, 
                    label = "Nº Hidrômetro (HD)",
                    leadingIcon = Icons.Default.WaterDrop,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text) // SÊNIOR FIX: Aceita Letras e Números
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = electricityMeter, 
                    onValueChange = { electricityMeter = it }, 
                    label = "Nº Medidor de Energia",
                    leadingIcon = Icons.Default.ElectricBolt
                )
            }

            AppCard(title = "Quantitativos", icon = Icons.Default.Analytics) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        AppTextField(
                            value = economiesCount, 
                            onValueChange = { if (it.all { c -> c.isDigit() }) economiesCount = it }, 
                            label = "Econ. Ativas",
                            leadingIcon = Icons.Default.HomeWork,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        AppTextField(
                            value = floorsCount, 
                            onValueChange = { if (it.all { c -> c.isDigit() }) floorsCount = it }, 
                            label = "Pavimentos",
                            leadingIcon = Icons.Default.Layers,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (latitude != null) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (latitude != null) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = if (latitude != null) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (latitude != null) "Localização Vinculada" else "GPS Pendente",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (latitude != null) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            if (fineLoc == PackageManager.PERMISSION_GRANTED) {
                                scope.launch {
                                    val loc = locationHelper.getCurrentLocation()
                                    latitude = loc?.latitude
                                    longitude = loc?.longitude
                                }
                            } else {
                                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = if (latitude != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Capturar GPS do Edifício")
                    }

                    // SÊNIOR UX: PAINEL DE INTELIGÊNCIA GEOGRÁFICA (MESMO PADRÃO DO RECADASTRO)
                    val suggestedGroup = remember(latitude, longitude) {
                        com.example.oaplicativo.util.GeoFencingHelper.findSuggestedGroup("Itapoá", latitude, longitude)
                    }
                    val suggestedRoute = remember(latitude, longitude) {
                        com.example.oaplicativo.util.GeoFencingHelper.findSuggestedRoute("Itapoá", latitude, longitude)
                    }

                    if (suggestedGroup != null || suggestedRoute != null) {
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF10B981).copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "📍 SETOR DETECTADO (OFICIAL):",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            suggestedGroup?.let { g ->
                                Surface(
                                    color = Color(0xFFECFDF5),
                                    shape = MaterialTheme.shapes.medium,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("GRUPO", style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
                                        Text(g, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF064E3B))
                                    }
                                }
                            }
                            suggestedRoute?.let { r ->
                                Surface(
                                    color = Color(0xFFECFDF5),
                                    shape = MaterialTheme.shapes.medium,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ROTA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
                                        Text(r, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF064E3B))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            AppButton(
                text = "SALVAR ATUALIZAÇÃO",
                onClick = {
                    scope.launch {
                        if (latitude == null) {
                            val loc = locationHelper.getCurrentLocation()
                            latitude = loc?.latitude
                            longitude = loc?.longitude
                        }
                        
                        // LÓGICA "INDSTRUTÍVEL": Todos os campos são opcionais.
                        // Aplicamos trim() e tratamos nulos/vazios.
                        val itemToSave = EconomyUpdate(
                            id = existingItem?.id,
                            hdNumber = hdNumber.trim().ifBlank { null }, 
                            buildingName = buildingName.trim().ifBlank { null },
                            constructionCompany = constructionCompany.trim().ifBlank { null },
                            economiesCount = economiesCount.toIntOrNull(),
                            floorsCount = floorsCount.toIntOrNull(),
                            electricityMeterNumber = electricityMeter.trim().ifBlank { null },
                            latitude = latitude,
                            longitude = longitude
                        )
                        
                        Log.d("EconomyScreen", "🟢 Botão Salvar clicado. Enviando para o ViewModel: Edifício: ${itemToSave.buildingName}")
                        viewModel.saveEconomyUpdate(itemToSave)
                    }
                },
                isLoading = state is EconomyUpdateState.Loading,
                enabled = true // SÊNIOR FIX: Nada bloqueia o salvamento
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
