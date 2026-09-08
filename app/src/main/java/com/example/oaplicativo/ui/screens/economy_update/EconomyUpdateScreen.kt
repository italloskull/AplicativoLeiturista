package com.example.oaplicativo.ui.screens.economy_update

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.oaplicativo.ui.components.SignatureDialog
import com.example.oaplicativo.ui.components.SignaturePreviewDialog
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
    var blockCount by remember(existingItem) { mutableStateOf(existingItem?.blockCount?.toString() ?: "") }
    var electricityMeter by remember(existingItem) { mutableStateOf(existingItem?.electricityMeterNumber ?: "") }
    var tilEsgoto by remember(existingItem) { mutableStateOf(existingItem?.tilEsgoto) }
    var accessibilityReading by remember(existingItem) { mutableStateOf(existingItem?.accessibilityReading) }
    var possuiPiscina by remember(existingItem) { mutableStateOf(existingItem?.possuiPiscina) }
    var observacao by remember(existingItem) { mutableStateOf(existingItem?.observacao ?: "") }
    var setor by remember(existingItem) { mutableStateOf(existingItem?.setor ?: "") }
    var quadra by remember(existingItem) { mutableStateOf(existingItem?.quadra ?: "") }
    var lote by remember(existingItem) { mutableStateOf(existingItem?.lote ?: "") }
    
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

    LaunchedEffect(itemId) {
        // SÊNIOR RESET & LOAD: Carrega assinatura do item se for edição
        viewModel.resetState()
        viewModel.loadItemForEdit(itemId)
    }

    LaunchedEffect(state) {
        if (state is EconomyUpdateState.SaveSuccess) { // SÊNIOR FIX: Fecha apenas no sucesso de salvamento
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
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding() 
            ) {
                var showSignatureDialog by remember { mutableStateOf(false) }
                var showPreviewDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val localBitmap = viewModel.signatureBitmap
                    val remoteUrl = viewModel.assinaturaUrl
                    val remoteBitmap = viewModel.remoteSignatureBitmap
                    val isDownloading = viewModel.isDownloadingSignature
                    val hasSignature = localBitmap != null || remoteBitmap != null || !remoteUrl.isNullOrBlank()

                    Button(
                        onClick = {
                            if (hasSignature) {
                                if (!remoteUrl.isNullOrBlank() && remoteBitmap == null && localBitmap == null) {
                                    viewModel.loadRemoteSignature()
                                }
                                showPreviewDialog = true
                            } else {
                                showSignatureDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasSignature) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (hasSignature) Color(0xFF15803D) else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (hasSignature) Icons.Default.CheckCircle else Icons.Default.Draw,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (hasSignature) Color(0xFF15803D) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = when {
                                localBitmap != null || remoteBitmap != null -> "Ver Assinatura Coletada ✓"
                                !remoteUrl.isNullOrBlank() -> if (isDownloading) "Baixando Assinatura..." else "Ver Assinatura na Nuvem"
                                else -> "Coletar Assinatura do Entrevistado"
                            },
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    AppButton(
                        text = "SALVAR ATUALIZAÇÃO",
                        icon = Icons.Default.CloudUpload,
                        isLoading = state is EconomyUpdateState.Loading,
                        onClick = {
                            scope.launch {
                                if (latitude == null) {
                                    val loc = locationHelper.getCurrentLocation()
                                    latitude = loc?.latitude
                                    longitude = loc?.longitude
                                }
                                
                                val itemToSave = EconomyUpdate(
                                    id = existingItem?.id,
                                    hdNumber = hdNumber.trim().ifBlank { null }, 
                                    buildingName = buildingName.trim().ifBlank { null },
                                    constructionCompany = constructionCompany.trim().ifBlank { null },
                                    economiesCount = economiesCount.toIntOrNull(),
                                    floorsCount = floorsCount.toIntOrNull(),
                                    blockCount = blockCount.toIntOrNull(),
                                    electricityMeterNumber = electricityMeter.trim().ifBlank { null },
                                    tilEsgoto = tilEsgoto,
                                    accessibilityReading = accessibilityReading,
                                    possuiPiscina = possuiPiscina,
                                    observacao = observacao.trim().ifBlank { null },
                                    setor = setor.trim().ifBlank { null },
                                    quadra = quadra.trim().ifBlank { null },
                                    lote = lote.trim().ifBlank { null },
                                    latitude = latitude,
                                    longitude = longitude
                                )
                                
                                Log.d("EconomyScreen", "🟢 Botão Salvar clicado. Enviando para o ViewModel: Edifício: ${itemToSave.buildingName}")
                                viewModel.saveEconomyUpdate(itemToSave)
                            }
                        }
                    )

                    if (showSignatureDialog) {
                        val buildingLabel = buildingName.ifBlank { "GE EMPREENDIMENTO" }
                        val userProfile = com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance().currentUserProfile.collectAsState().value
                        val leituristaLabel = userProfile?.fullName ?: userProfile?.username ?: "Equipe de Campo"
                        val gpsLabel = if (latitude != null && longitude != null) "$latitude, $longitude" else "GPS PENDENTE"

                        SignatureDialog(
                            entrevistadoInfo = buildingLabel,
                            leituristaInfo = leituristaLabel,
                            gpsInfo = gpsLabel,
                            onDismiss = { showSignatureDialog = false },
                            onSignatureConfirmed = { bitmap, lines ->
                                viewModel.onSignatureCaptured(bitmap, lines)
                            }
                        )
                    }

                    if (showPreviewDialog) {
                        SignaturePreviewDialog(
                            bitmap = viewModel.generateLatestStampedSignature(buildingName, latitude, longitude) ?: remoteBitmap,
                            onDismiss = { showPreviewDialog = false },
                            onRedo = {
                                viewModel.clearSignature()
                                showSignatureDialog = true
                            }
                        )
                    }
                }
            }
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
            // SÊNIOR UX FIX: Seletor só aparece para Admins com múltiplas cidades
            val authorizedCities by viewModel.authorizedCities.collectAsState()
            val userProfileState = com.example.oaplicativo.data.repository.AuthRepositoryImpl.getInstance().currentUserProfile.collectAsState()
            val isPowerUser = userProfileState.value?.cargo?.lowercase()?.let { it == "administrador" || it == "desenvolvedor" } ?: false
            
            if (isPowerUser && authorizedCities.size > 1) {
                AppCard(title = "Cidade do Registro", icon = Icons.Default.LocationCity) {
                    com.example.oaplicativo.ui.components.SpinnerOption(
                        label = "Selecione o Município",
                        options = authorizedCities,
                        selectedOption = viewModel.selectedCidadeForRegistry,
                        onOptionSelected = { viewModel.selectedCidadeForRegistry = it },
                        labelProvider = { it.nome.uppercase() }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

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

            AppCard(title = "Localização Cadastral", icon = Icons.Default.Map) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        AppTextField(value = setor, onValueChange = { setor = it }, label = "Setor")
                    }
                    Box(Modifier.weight(1f)) {
                        AppTextField(value = quadra, onValueChange = { quadra = it }, label = "Quadra")
                    }
                    Box(Modifier.weight(1f)) {
                        AppTextField(value = lote, onValueChange = { lote = it }, label = "Lote")
                    }
                }
            }

            AppCard(title = "Técnico", icon = Icons.Default.SettingsInputComponent) {
                AppTextField(
                    value = hdNumber, 
                    onValueChange = { hdNumber = it }, 
                    label = "Nº Hidrômetro (HD)",
                    leadingIcon = Icons.Default.WaterDrop,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = electricityMeter, 
                    onValueChange = { electricityMeter = it }, 
                    label = "Nº Medidor de Energia",
                    leadingIcon = Icons.Default.ElectricBolt
                )
                
                Spacer(Modifier.height(12.dp))
                
                // SÊNIOR UI INJECTION: Campo TIL para Prédios
                com.example.oaplicativo.ui.components.BooleanOption(
                    label = "Possui conexão de esgoto (Til)?", 
                    selectedOption = tilEsgoto
                ) { 
                    tilEsgoto = it 
                }

                Spacer(Modifier.height(12.dp))

                com.example.oaplicativo.ui.components.BooleanOption(
                    label = "Possui piscina?", 
                    selectedOption = possuiPiscina
                ) { 
                    possuiPiscina = it 
                }

                Spacer(Modifier.height(12.dp))
                
                com.example.oaplicativo.ui.components.SpinnerOption(
                    label = "Acessibilidade para leitura mensal", 
                    options = listOf("Facil Acesso", "Dificil Acesso"), 
                    selectedOption = accessibilityReading, 
                    onOptionSelected = { accessibilityReading = it }
                )
            }

            AppCard(title = "Quantitativos", icon = Icons.Default.Analytics) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        AppTextField(
                            value = economiesCount, 
                            onValueChange = { if (it.all { c -> c.isDigit() }) economiesCount = it }, 
                            label = "Economias",
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
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = blockCount, 
                    onValueChange = { if (it.all { c -> c.isDigit() }) blockCount = it }, 
                    label = "Quantidade de Blocos",
                    leadingIcon = Icons.Default.Domain,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            AppCard(title = "Observações", icon = Icons.Default.NoteAlt) {
                AppTextField(
                    value = observacao, 
                    onValueChange = { observacao = it }, 
                    label = "Notas de Campo / Observações", 
                    leadingIcon = Icons.AutoMirrored.Filled.Comment
                )
            }

            AppCard(title = "Assinatura do Entrevistado / Responsável", icon = Icons.Default.Draw) {
                val localBitmap = viewModel.signatureBitmap
                val remoteUrl = viewModel.assinaturaUrl
                val remoteBitmap = viewModel.remoteSignatureBitmap
                val isDownloading = viewModel.isDownloadingSignature
                val activeBitmap = viewModel.generateLatestStampedSignature(buildingName, latitude, longitude) ?: localBitmap ?: remoteBitmap

                when {
                    activeBitmap != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                bitmap = activeBitmap.asImageBitmap(),
                                contentDescription = "Assinatura Capturada",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.White, shape = MaterialTheme.shapes.medium)
                                    .padding(8.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { viewModel.clearSignature() }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Refazer Assinatura", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    remoteBitmap != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                bitmap = remoteBitmap.asImageBitmap(),
                                contentDescription = "Assinatura Remota",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.White, shape = MaterialTheme.shapes.medium)
                                    .padding(8.dp)
                            )
                        }
                    }

                    !remoteUrl.isNullOrBlank() -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Assinatura registrada na nuvem",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            AppButton(
                                text = if (isDownloading) "Baixando..." else "Ver Assinatura",
                                onClick = { viewModel.loadRemoteSignature() },
                                isLoading = isDownloading,
                                icon = Icons.Default.Visibility
                            )
                        }
                    }

                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Nenhuma assinatura coletada.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
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
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
