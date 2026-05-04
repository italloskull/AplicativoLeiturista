package com.example.oaplicativo.ui.screens.customer_form

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.presentation.components.CensoredDataField
import com.example.oaplicativo.ui.components.BooleanOption
import com.example.oaplicativo.ui.components.SpinnerOption
import com.example.oaplicativo.ui.components.FormSectionCard
import com.example.oaplicativo.util.LocationHelper
import com.example.oaplicativo.util.privacy.PrivacyUtils
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormScreen(
    customerId: String? = null,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: CustomerFormViewModel = viewModel(factory = androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current?.let {
        androidx.lifecycle.viewmodel.viewModelFactory {
            addInitializer(CustomerFormViewModel::class) {
                CustomerFormViewModel(application = (this[androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as android.app.Application))
            }
        }
    } ?: androidx.lifecycle.viewmodel.viewModelFactory { })
) {
    val customer = remember(customerId) { viewModel.getCustomer(customerId) }
    val userProfile by viewModel.currentUserProfile.collectAsState()

    var nome by remember(customer) { mutableStateOf(customer?.name ?: "") }
    var matricula by remember(customer) { mutableStateOf(customer?.registrationNumber ?: "") }
    var digitoMatricula by remember(customer) { mutableStateOf(customer?.registrationDigit ?: "") }
    var email by remember(customer) { mutableStateOf(customer?.email ?: "") }
    var telefoneFixo by remember(customer) { mutableStateOf(customer?.landline ?: "") }
    var celular by remember(customer) { mutableStateOf(customer?.cellPhone ?: "") }
    var isCaixaPadrao by remember(customer) { mutableStateOf<Boolean?>(customer?.isStandardMeasurementBox) }
    var isLacresPadronizados by remember(customer) { mutableStateOf<Boolean?>(customer?.isStandardizedSeals) }
    var isHdAcessivel by remember(customer) { mutableStateOf<Boolean?>(customer?.isHdAccessible) }
    var isVeranista by remember(customer) { mutableStateOf<Boolean?>(customer?.isVacationer) }
    var situacaoLocal by remember(customer) { mutableStateOf(customer?.locationStatus) }
    var qtdEconomias by remember(customer) { mutableStateOf(customer?.economiesCount) }
    var latitude by remember(customer) { mutableStateOf(customer?.latitude) }
    var longitude by remember(customer) { mutableStateOf(customer?.longitude) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }

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

    val formState by viewModel.state.collectAsState()
    val isMatriculaValid by remember { derivedStateOf { matricula.isNotEmpty() } }
    val isDataCensoredInitial = remember(customer) { PrivacyUtils.shouldMaskSensitiveData(customer?.createdAt) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(formState) {
        if (formState is CustomerFormState.Success) onSaveSuccess()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(if (customer == null) "Novo Recadastro" else "Editar Cliente", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    if (customer != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Excluir Registro") },
                text = { Text("Esta ação removerá o cadastro permanentemente. Deseja continuar?") },
                confirmButton = {
                    Button(onClick = { customer?.id?.let { viewModel.deleteCustomer(it) }; showDeleteDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Confirmar Exclusão")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormSectionCard(title = "Identificação") {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { if (it.length <= 100) nome = it },
                    label = { Text("Nome Completo do Titular") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = MaterialTheme.shapes.large
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = matricula,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 30) matricula = it },
                        label = { Text("Nº Matrícula") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.large
                    )
                    OutlinedTextField(
                        value = digitoMatricula,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 2) digitoMatricula = it },
                        label = { Text("Díg.") },
                        modifier = Modifier.width(90.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.large
                    )
                }
            }

            FormSectionCard(title = "Informações de Contato") {
                CensoredDataField(
                    label = "E-mail Principal",
                    value = email,
                    onValueChange = { email = it },
                    isCensoredInitial = isDataCensoredInitial,
                    censoredValue = PrivacyUtils.applyPartialEmailCensorship(email),
                    isAdmin = userProfile?.isAdmin ?: false,
                    leadingIcon = Icons.Default.AlternateEmail,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(modifier = Modifier.height(12.dp))
                CensoredDataField(
                    label = "Telefone para Contato",
                    value = celular,
                    onValueChange = { celular = it },
                    isCensoredInitial = isDataCensoredInitial,
                    censoredValue = PrivacyUtils.applyPartialPhoneCensorship(celular),
                    isAdmin = userProfile?.isAdmin ?: false,
                    leadingIcon = Icons.Default.PhoneIphone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            FormSectionCard(title = "Hidrometria e Instalação") {
                BooleanOption(label = "Caixa de medição padrão?", checked = isCaixaPadrao) { isCaixaPadrao = it }
                BooleanOption(label = "Lacres em bom estado?", checked = isLacresPadronizados) { isLacresPadronizados = it }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                SpinnerOption(
                    label = "Ocorrência no Local",
                    options = listOf("Residencial", "Comercial", "Lote Vazio", "Demolido", "Edifício"),
                    selectedOption = situacaoLocal,
                    onOptionSelected = { situacaoLocal = it }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (latitude == null) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) 
                                     else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                border = if (latitude == null) BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (latitude != null) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = if (latitude != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Coleta de Geometria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    AnimatedContent(targetState = latitude != null, label = "GPS_Status") { hasLat ->
                        if (hasLat) {
                            Text("Localização vinculada com sucesso ao imóvel.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Atenção: A captura do GPS é obrigatória para validar a visita.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atualizar Coordenadas")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (isMatriculaValid) {
                        scope.launch {
                            var finalLat = latitude
                            var finalLong = longitude

                            if (finalLat == null || finalLong == null) {
                                val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                if (fineLoc == PackageManager.PERMISSION_GRANTED) {
                                    val loc = locationHelper.getCurrentLocation()
                                    finalLat = loc?.latitude
                                    finalLong = loc?.longitude
                                } else {
                                    locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                                    return@launch 
                                }
                            }

                            val fullNow = ZonedDateTime.now()
                            viewModel.saveCustomer(
                                Customer(
                                    id = customer?.id,
                                    name = nome.trim(),
                                    registrationNumber = matricula.trim(),
                                    registrationDigit = digitoMatricula.trim(),
                                    email = email.trim().lowercase(),
                                    landline = telefoneFixo.trim(),
                                    cellPhone = celular.trim(),
                                    isStandardMeasurementBox = isCaixaPadrao,
                                    isStandardizedSeals = isLacresPadronizados,
                                    isHdAccessible = isHdAcessivel,
                                    isVacationer = isVeranista,
                                    locationStatus = situacaoLocal,
                                    economiesCount = qtdEconomias,
                                    latitude = finalLat,
                                    longitude = finalLong,
                                    addedBy = userProfile?.fullName ?: userProfile?.username ?: "Usuário",
                                    capturedAt = fullNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                    createdAt = fullNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                                    date = fullNow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                enabled = formState !is CustomerFormState.Loading,
                shape = MaterialTheme.shapes.large,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                if (formState is CustomerFormState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("FINALIZAR RECADASTRO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
