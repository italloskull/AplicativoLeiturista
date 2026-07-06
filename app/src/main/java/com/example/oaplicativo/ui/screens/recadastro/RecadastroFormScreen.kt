@file:Suppress("SpellCheckingInspection")
package com.example.oaplicativo.ui.screens.recadastro

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.ui.components.*
import com.example.oaplicativo.presentation.components.CensoredDataField
import com.example.oaplicativo.ui.screens.recadastro.viewmodel.RecadastroViewModel
import com.example.oaplicativo.util.CepVisualTransformation
import com.example.oaplicativo.util.CpfCnpjVisualTransformation
import com.example.oaplicativo.util.DateVisualTransformation
import com.example.oaplicativo.util.PhoneVisualTransformation
import com.example.oaplicativo.util.privacy.PrivacyUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecadastroFormScreen(
    customerId: String? = null,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecadastroViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { com.example.oaplicativo.util.LocationHelper(context) }
    val userProfile by viewModel.currentUserProfile.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch {
                viewModel.isCapturingLocation = true
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    viewModel.latitude = loc.latitude
                    viewModel.longitude = loc.longitude
                    viewModel.fetchAddressFromLocation(loc.latitude, loc.longitude)
                }
                viewModel.isCapturingLocation = false
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadCustomerForEdit(customerId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (customerId == null) "NOVO RECADASTRO" else "EDITAR RECADASTRO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    GlobalActionMenu(
                        isDarkTheme = isDarkTheme,
                        isAdmin = userProfile?.isAdmin ?: false,
                        onToggleTheme = onToggleTheme,
                        onLogout = onLogout
                    )
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding() 
            ) {
                Box(modifier = Modifier.padding(24.dp)) {
                    AppButton(
                        text = if (viewModel.isCapturingGpsOnSave) "Capturando GPS..." else "Salvar Recadastro",
                        icon = if (viewModel.isCapturingGpsOnSave) null else Icons.Default.CloudUpload,
                        isLoading = viewModel.isCapturingGpsOnSave,
                        onClick = {
                            scope.launch {
                                if (viewModel.latitude == null || viewModel.longitude == null) {
                                    viewModel.isCapturingGpsOnSave = true
                                    val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                    if (fineLoc == PackageManager.PERMISSION_GRANTED) {
                                        val loc = locationHelper.getCurrentLocation()
                                        if (loc != null) {
                                            viewModel.latitude = loc.latitude
                                            viewModel.longitude = loc.longitude
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                                        viewModel.isCapturingGpsOnSave = false
                                        return@launch
                                    }
                                    viewModel.isCapturingGpsOnSave = false
                                }
                                
                                viewModel.saveRecadastro(
                                    onSuccess = onSaveSuccess,
                                    onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                                )
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                val authorizedCities by viewModel.authorizedCities.collectAsState()
                val isPowerUser = userProfile?.cargo?.lowercase()?.let { it == "administrador" || it == "desenvolvedor" } ?: false
                
                if (isPowerUser && authorizedCities.size > 1) {
                    AppCard(title = "Cidade do Registro", icon = Icons.Default.LocationCity) {
                        SpinnerOption(
                            label = "Selecione o Município",
                            options = authorizedCities,
                            selectedOption = viewModel.selectedCidadeForRegistry,
                            onOptionSelected = { viewModel.selectedCidadeForRegistry = it },
                            labelProvider = { it.nome.uppercase() }
                        )
                        
                        if (viewModel.selectedCidadeForRegistry == null) {
                            Text(
                                "❌ Seleção obrigatória para faturamento", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            item {
                AppCard(title = "Localização e Hidrometria", icon = Icons.Default.Map) {
                    BooleanOption(label = "Medição via Hidrômetro?", selectedOption = viewModel.possuiHidrometro) { viewModel.possuiHidrometro = it }
                    if (viewModel.possuiHidrometro == "Sim") {
                        Spacer(Modifier.height(12.dp))
                        AppTextField(value = viewModel.numeroHidrometro, onValueChange = { viewModel.numeroHidrometro = it }, label = "Nº de Série do Hidrômetro", leadingIcon = Icons.Default.Pin)
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(0.75f)) {
                            AppTextField(
                                value = viewModel.matricula, 
                                onValueChange = { viewModel.matricula = it }, 
                                label = "Matrícula",
                                leadingIcon = Icons.Default.Numbers,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                            )
                        }
                        
                        Text("-", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Box(Modifier.weight(0.25f)) {
                            AppTextField(
                                value = viewModel.registrationDigit, 
                                onValueChange = { if (it.length <= 1) viewModel.registrationDigit = it }, 
                                label = "DV",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            AppTextField(value = viewModel.setor, onValueChange = { viewModel.setor = it }, label = "Setor", leadingIcon = Icons.Default.Map)
                        }
                        Box(Modifier.weight(1f)) {
                            AppTextField(value = viewModel.quadra, onValueChange = { viewModel.quadra = it }, label = "Quadra", leadingIcon = Icons.Default.GridOn)
                        }
                    }

                    val sugGrupo = viewModel.grupoSugerido
                    val sugRota = viewModel.rotaSugerida

                    if (sugGrupo != null || sugRota != null) {
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF10B981).copy(alpha = 0.3f))
                        Spacer(Modifier.height(20.dp))

                        Text(
                            "📍 SETOR DETECTADO (OFICIAL):",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            sugGrupo?.let { g ->
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
                            sugRota?.let { r ->
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
                    
                    Spacer(Modifier.height(24.dp))
                    
                    GpsStatusCard(
                        latitude = viewModel.latitude,
                        longitude = viewModel.longitude,
                        onUpdateClick = {
                            val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            if (fineLoc == PackageManager.PERMISSION_GRANTED) {
                                scope.launch {
                                    Log.d("GeoDebug", "Botão 'Atualizar Localização' clicado.")
                                    viewModel.isCapturingLocation = true
                                    val loc = locationHelper.getCurrentLocation()
                                    if (loc != null) {
                                        Log.d("GeoDebug", "GPS obtido: ${loc.latitude}, ${loc.longitude}")
                                        viewModel.latitude = loc.latitude
                                        viewModel.longitude = loc.longitude
                                        viewModel.fetchAddressFromLocation(loc.latitude, loc.longitude)
                                    } else {
                                        Log.e("GeoDebug", "Falha ao obter localização do LocationHelper.")
                                        Toast.makeText(context, "Sinal de GPS fraco ou inexistente.", Toast.LENGTH_SHORT).show()
                                    }
                                    viewModel.isCapturingLocation = false
                                }
                            } else {
                                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        },
                        isLoading = viewModel.isCapturingLocation
                    )
                }
            }

            item {
                AppCard(title = "Endereço", icon = Icons.Default.Home) {
                    AppTextField(
                        value = viewModel.cep, 
                        onValueChange = { viewModel.onCepChange(it) }, 
                        label = "CEP", 
                        leadingIcon = Icons.Default.Map,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        visualTransformation = CepVisualTransformation()
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = viewModel.logradouro, onValueChange = { viewModel.logradouro = it }, label = "Rua / Avenida", leadingIcon = Icons.Default.Signpost)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) { AppTextField(value = viewModel.numero, onValueChange = { viewModel.numero = it }, label = "Número", leadingIcon = Icons.Default.HomeWork) }
                        Box(Modifier.weight(1.5f)) { AppTextField(value = viewModel.complemento, onValueChange = { viewModel.complemento = it }, label = "Complemento", leadingIcon = Icons.Default.AddHome) }
                    }
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = viewModel.bairro, onValueChange = { viewModel.bairro = it }, label = "Bairro", leadingIcon = Icons.Default.HolidayVillage)
                }
            }

            item {
                AppCard(title = "Características Físicas", icon = Icons.Default.Foundation) {
                    BooleanOption(label = "Rede de abastecimento ativa?", selectedOption = viewModel.existeRedeAgua) { viewModel.existeRedeAgua = it }
                    BooleanOption(label = "Beneficiário de Tarifa Social?", selectedOption = viewModel.beneficiarioSocial) { viewModel.beneficiarioSocial = it }
                    BooleanOption(label = "Consome água de vizinho?", selectedOption = viewModel.usaAguaVizinho) { viewModel.usaAguaVizinho = it }
                    BooleanOption(label = "O imóvel possui piscina?", selectedOption = viewModel.possuiPiscina) { viewModel.possuiPiscina = it }
                    BooleanOption(label = "É imóvel de veranista?", selectedOption = viewModel.isVacationer) { viewModel.isVacationer = it }
                    
                    Spacer(Modifier.height(8.dp))
                    SpinnerOption(label = "Situação do Local", options = listOf("Terreno Baldio", "Construção", "Residencial", "Comércio", "Outros"), selectedOption = viewModel.locationStatus, onOptionSelected = { viewModel.locationStatus = it })
                    SpinnerOption(label = "Tipo de Pavimento da Rua", options = listOf("Asfalto", "Paralelepípedo", "Terra", "Outro"), selectedOption = viewModel.pavimentoRua, onOptionSelected = { viewModel.pavimentoRua = it })
                    SpinnerOption(label = "Tipo de Pavimento da Calçada", options = listOf("Asfalto", "Paver", "Concreto", "Terra", "Outro"), selectedOption = viewModel.pavimentoCalcada, onOptionSelected = { viewModel.pavimentoCalcada = it })
                }
            }

            item {
                AppCard(title = "Hidrometria Técnica", icon = Icons.Default.WaterDrop) {
                    BooleanOption(label = "Possui Caixa Padrão?", selectedOption = viewModel.isStandardMeasurementBox) { viewModel.isStandardMeasurementBox = it }
                    BooleanOption(label = "Lacres Padronizados?", selectedOption = viewModel.isStandardizedSeals) { viewModel.isStandardizedSeals = it }
                    BooleanOption(label = "Hidrômetro Acessível?", selectedOption = viewModel.isHdAccessible) { viewModel.isHdAccessible = it }
                    
                    Spacer(Modifier.height(12.dp))
                    SpinnerOption(label = "Possui reservatório (Caixa)?", options = listOf("Sim", "Não", "Não Visível"), selectedOption = viewModel.possuiCaixaAgua, onOptionSelected = { viewModel.possuiCaixaAgua = it })
                    AppTextField(value = viewModel.economias, onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.economias = it }, label = "Nº de Economias", leadingIcon = Icons.Default.MapsHomeWork, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(Modifier.height(12.dp))
                    SpinnerOption(label = "Local da Instalação", options = listOf("CAIXA PADRÃO", "INTERNO", "CAVALETE EXTERNO"), selectedOption = viewModel.localInstalacao, onOptionSelected = { viewModel.localInstalacao = it })
                    SpinnerOption(label = "Condição de Acessibilidade", options = listOf("Facil Acesso", "Dificil Acesso"), selectedOption = viewModel.acessibilidade, onOptionSelected = { viewModel.acessibilidade = it })
                }
            }

            item {
                AppCard(title = "Responsável pela Fatura", icon = Icons.Default.Badge) {
                    Text("O responsável é o:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)) {
                        val roles = listOf("Proprietário", "Locatário")
                        roles.forEachIndexed { index, role ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = roles.size),
                                onClick = { viewModel.responsavelTipo = role },
                                selected = viewModel.responsavelTipo == role
                            ) {
                                Text(role, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    val activeData = viewModel.responsavelData

                    val displayName = if (viewModel.isDataCensoredInitial) {
                        activeData.nomeCompleto.split(" ").firstOrNull() ?: ""
                    } else activeData.nomeCompleto

                    AppTextField(
                        value = displayName, 
                        onValueChange = { if (!viewModel.isDataCensoredInitial) activeData.nomeCompleto = it }, 
                        label = "Nome Completo do Responsável", 
                        leadingIcon = Icons.Default.Person, 
                        enabled = !viewModel.isDataCensoredInitial
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    CensoredDataField(
                        label = "CPF / CNPJ",
                        value = activeData.cpfCnpj,
                        onValueChange = { if (it.length <= 14) activeData.cpfCnpj = it },
                        isCensoredInitial = viewModel.isDataCensoredInitial,
                        censoredValue = PrivacyUtils.maskCpfCnpj(activeData.cpfCnpj),
                        isAdmin = userProfile?.isAdmin ?: false,
                        leadingIcon = Icons.Default.Badge,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = CpfCnpjVisualTransformation()
                    )

                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = activeData.nomeMae, 
                        onValueChange = { activeData.nomeMae = it }, 
                        label = "Nome da Mãe", 
                        leadingIcon = Icons.Default.EscalatorWarning, 
                        enabled = !viewModel.isDataCensoredInitial
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = activeData.dataNascimento, 
                        onValueChange = { if (it.length <= 8) activeData.dataNascimento = it }, 
                        label = "Data de Nascimento", 
                        leadingIcon = Icons.Default.CalendarToday, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        visualTransformation = DateVisualTransformation(), 
                        enabled = !viewModel.isDataCensoredInitial
                    )
                    SpinnerOption(label = "Sexo", options = listOf("Masculino", "Feminino", "Outro"), selectedOption = activeData.sexo, onOptionSelected = { activeData.sexo = it })
                    
                    Spacer(Modifier.height(20.dp))
                    Text("Contatos do Responsável", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    
                    CensoredDataField(
                        label = "E-mail Principal",
                        value = viewModel.email,
                        onValueChange = { viewModel.email = it.trim() },
                        isCensoredInitial = viewModel.isDataCensoredInitial,
                        censoredValue = PrivacyUtils.applyPartialEmailCensorship(viewModel.email),
                        isAdmin = userProfile?.isAdmin ?: false,
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(Modifier.height(12.dp))
                    
                    CensoredDataField(
                        label = "Celular (WhatsApp)",
                        value = viewModel.celular1,
                        onValueChange = { if (it.length <= 11) viewModel.celular1 = it },
                        isCensoredInitial = viewModel.isDataCensoredInitial,
                        censoredValue = PrivacyUtils.applyPartialPhoneCensorship(viewModel.celular1),
                        isAdmin = userProfile?.isAdmin ?: false,
                        leadingIcon = Icons.Default.PhoneIphone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        visualTransformation = PhoneVisualTransformation()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    
                    BooleanOption(label = "Apresentou Documentação?", selectedOption = activeData.apresentouDoc) { activeData.apresentouDoc = it }
                    
                    if (activeData.apresentouDoc == "Sim") {
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = activeData.qualDoc,
                            onValueChange = { activeData.qualDoc = it },
                            label = "Qual Documento?",
                            leadingIcon = Icons.Default.Description
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    Text("Sobre a Entrevista:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))

                    BooleanOption(
                        label = "O entrevistado é o próprio responsável?", 
                        selectedOption = viewModel.entrevistadoEhOResponsavel
                    ) { 
                        viewModel.entrevistadoEhOResponsavel = it ?: "Sim"
                    }

                    if (viewModel.entrevistadoEhOResponsavel == "Não") {
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = viewModel.entrevistadoNomeApenas,
                            onValueChange = { viewModel.entrevistadoNomeApenas = it },
                            label = "Nome de quem atendeu (Entrevistado)",
                            leadingIcon = Icons.Default.RecordVoiceOver
                        )
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = viewModel.entrevistadoEmailApenas,
                            onValueChange = { viewModel.entrevistadoEmailApenas = it },
                            label = "E-mail do Entrevistado (Opcional)",
                            leadingIcon = Icons.Default.Email
                        )
                        Spacer(Modifier.height(12.dp))
                        AppTextField(
                            value = viewModel.entrevistadoCelularApenas,
                            onValueChange = { if (it.length <= 11) viewModel.entrevistadoCelularApenas = it },
                            label = "WhatsApp do Entrevistado (Opcional)",
                            leadingIcon = Icons.Default.PhoneIphone,
                            visualTransformation = PhoneVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            item {
                AppCard(title = "Observações", icon = Icons.Default.NoteAlt) {
                    AppTextField(value = viewModel.observacao, onValueChange = { viewModel.observacao = it }, label = "Notas de Campo", leadingIcon = Icons.AutoMirrored.Filled.Comment)
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
