package com.example.oaplicativo.ui.screens.customer_form

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.Customer
import com.example.oaplicativo.presentation.components.CensoredDataField
import com.example.oaplicativo.ui.components.BooleanOption
import com.example.oaplicativo.ui.components.SpinnerOption
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
    val isAdmin = userProfile?.isAdmin == true

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
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
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
        if (formState is CustomerFormState.Success) {
            onSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (customer == null) "Cadastrar Cliente" else "Atualizar Cliente") },
                actions = {
                    if (customer != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir Cliente", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Excluir Cliente") },
                text = { Text("Tem certeza que deseja excluir este cliente? Esta ação não pode ser desfeita e é um direito garantido pela LGPD.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            customer?.id?.let { viewModel.deleteCustomer(it) }
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = nome,
                onValueChange = { if (it.length <= 100) nome = it },
                label = { Text("Nome (Opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = matricula,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 30) matricula = it },
                    label = { Text("Matrícula") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = digitoMatricula,
                    onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) digitoMatricula = it },
                    label = { Text("Díg.") },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            CensoredDataField(
                label = "E-mail (Opcional)",
                value = email,
                onValueChange = { email = it },
                isCensoredInitial = isDataCensoredInitial,
                censoredValue = PrivacyUtils.applyPartialEmailCensorship(email),
                isAdmin = isAdmin,
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            CensoredDataField(
                label = "Telefone Fixo (Opcional)",
                value = telefoneFixo,
                onValueChange = { telefoneFixo = it },
                isCensoredInitial = isDataCensoredInitial,
                censoredValue = PrivacyUtils.applyPartialPhoneCensorship(telefoneFixo),
                isAdmin = isAdmin,
                leadingIcon = Icons.Default.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            CensoredDataField(
                label = "Celular (Opcional)",
                value = celular,
                onValueChange = { celular = it },
                isCensoredInitial = isDataCensoredInitial,
                censoredValue = PrivacyUtils.applyPartialPhoneCensorship(celular),
                isAdmin = isAdmin,
                leadingIcon = Icons.Default.Smartphone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(16.dp))

            BooleanOption(label = "Caixa de medição é padrão?", checked = isCaixaPadrao) { isCaixaPadrao = it }
            BooleanOption(label = "Lacres padronizados?", checked = isLacresPadronizados) { isLacresPadronizados = it }
            BooleanOption(label = "HD acessível?", checked = isHdAcessivel) { isHdAcessivel = it }
            BooleanOption(label = "Veranista?", checked = isVeranista) { isVeranista = it }

            Spacer(modifier = Modifier.height(16.dp))

            SpinnerOption(
                label = "Situação do Local",
                options = listOf("Residencial", "Comercial", "Lote Vazio", "Demolido", "Edifício"),
                selectedOption = situacaoLocal,
                onOptionSelected = { situacaoLocal = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SpinnerOption(
                label = "Quantidade de Economias",
                options = (1..50).map { it.toString() },
                selectedOption = qtdEconomias?.toString(),
                onOptionSelected = { it?.let { qtdEconomias = it.toIntOrNull() } }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Localização GPS", style = MaterialTheme.typography.titleMedium)
                    if (latitude != null && longitude != null) {
                        Text("Lat: $latitude", style = MaterialTheme.typography.bodyMedium)
                        Text("Long: $longitude", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Nenhuma localização capturada", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A localização é coletada apenas para fins de registro da visita técnica conforme a LGPD.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            val coarseLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            
                            if (fineLoc == PackageManager.PERMISSION_GRANTED || coarseLoc == PackageManager.PERMISSION_GRANTED) {
                                scope.launch {
                                    val loc = locationHelper.getCurrentLocation()
                                    latitude = loc?.latitude
                                    longitude = loc?.longitude
                                }
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capturar Localização")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (formState is CustomerFormState.Error) {
                Text(
                    text = (formState as CustomerFormState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (isMatriculaValid) {
                        scope.launch {
                            // AUTOMAÇÃO DE GPS PRECISO: 
                            var finalLat = latitude
                            var finalLong = longitude

                            if (finalLat == null || finalLong == null) {
                                val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                
                                if (fineLoc == PackageManager.PERMISSION_GRANTED) {
                                    // Temos permissão, captura agora com alta precisão
                                    val loc = locationHelper.getCurrentLocation()
                                    finalLat = loc?.latitude
                                    finalLong = loc?.longitude
                                } else {
                                    // NÃO temos permissão, pedimos agora
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                    // Após o pedido, o usuário precisará clicar em Salvar de novo 
                                    // para garantir que a captura ocorra com a nova permissão.
                                    return@launch
                                }
                            }

                            val fullNow = ZonedDateTime.now()
                            val timestampIso = fullNow.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            val simplifiedDate = fullNow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))

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
                                    
                                    // CAMPOS DE AUDITORIA
                                    addedBy = userProfile?.fullName ?: userProfile?.username ?: "Usuário Desconhecido",
                                    capturedAt = timestampIso,
                                    createdAt = timestampIso,
                                    date = simplifiedDate
                                )
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = formState !is CustomerFormState.Loading
            ) {
                if (formState is CustomerFormState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Salvar")
                }
            }
            TextButton(
                onClick = { onBack() },
                modifier = Modifier.fillMaxWidth(),
                enabled = formState !is CustomerFormState.Loading
            ) {
                Text("Voltar")
            }
        }
    }
}