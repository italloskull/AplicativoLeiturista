package com.example.oaplicativo.ui.screens.recadastro

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.ui.components.*
import com.example.oaplicativo.ui.screens.recadastro.viewmodel.RecadastroViewModel
import com.example.oaplicativo.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecadastroFormScreen(
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: RecadastroViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    var isCapturingGpsOnSave by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            scope.launch {
                val loc = locationHelper.getCurrentLocation()
                viewModel.latitude = loc?.latitude
                viewModel.longitude = loc?.longitude
                if (loc != null) {
                    viewModel.fetchAddressFromLocation(loc.latitude, loc.longitude)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = { Text("NOVO RECADASTRO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                )
                
                val progress by animateFloatAsState(
                    targetValue = viewModel.registrationProgress,
                    animationSpec = tween(600),
                    label = "QualityProgress"
                )
                
                val barColor = when {
                    progress < 0.4f -> Color(0xFFF44336) // Ruim
                    progress < 0.75f -> Color(0xFFFFC107) // Regular
                    else -> Color(0xFF4CAF50) // Boa
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(horizontal = 20.dp)
                        .clip(CircleShape),
                    color = barColor,
                    trackColor = barColor.copy(alpha = 0.1f)
                )
                
                Text(
                    text = if (progress >= 0.75f) "CADASTRO DE ALTA QUALIDADE ✨" else "COMPLETE OS CAMPOS PARA SUBIR A QUALIDADE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = barColor,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp)
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 12.dp, 
                shadowElevation = 12.dp,
                modifier = Modifier.navigationBarsPadding() 
            ) {
                Box(modifier = Modifier.padding(20.dp)) {
                    AppButton(
                        text = if (isCapturingGpsOnSave) "Capturando GPS..." else "Salvar Recadastro",
                        icon = if (isCapturingGpsOnSave) null else Icons.Default.CloudUpload,
                        isLoading = isCapturingGpsOnSave,
                        onClick = {
                            scope.launch {
                                if (viewModel.latitude == null || viewModel.longitude == null) {
                                    isCapturingGpsOnSave = true
                                    val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                    if (fineLoc == PackageManager.PERMISSION_GRANTED) {
                                        val loc = locationHelper.getCurrentLocation()
                                        viewModel.latitude = loc?.latitude
                                        viewModel.longitude = loc?.longitude
                                    } else {
                                        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                                        isCapturingGpsOnSave = false
                                        return@launch
                                    }
                                    isCapturingGpsOnSave = false
                                }
                                
                                viewModel.saveRecadastro(
                                    onSuccess = onSaveSuccess,
                                    onError = { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 🏷️ FASE 1: VISÃO DE RUA (Automático/Visual)
            
            item {
                AppCard(title = "Localização do Imóvel", icon = Icons.Default.Map) {
                    AppTextField(
                        value = viewModel.matricula, 
                        onValueChange = { viewModel.matricula = it }, 
                        label = "Matrícula do Imóvel",
                        leadingIcon = Icons.Default.Numbers,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            AppTextField(
                                value = viewModel.setor, 
                                onValueChange = { viewModel.setor = it }, 
                                label = "Setor", 
                                leadingIcon = Icons.Default.Map, 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            AppTextField(
                                value = viewModel.quadra, 
                                onValueChange = { viewModel.quadra = it }, 
                                label = "Quadra", 
                                leadingIcon = Icons.Default.GridOn, 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    GpsStatusCard(
                        latitude = viewModel.latitude,
                        longitude = viewModel.longitude,
                        onUpdateClick = {
                            val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            if (fineLoc == PackageManager.PERMISSION_GRANTED) {
                                scope.launch {
                                    val loc = locationHelper.getCurrentLocation()
                                    viewModel.latitude = loc?.latitude
                                    viewModel.longitude = loc?.longitude
                                    if (loc != null) viewModel.fetchAddressFromLocation(loc.latitude, loc.longitude)
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        visualTransformation = CepVisualTransformation(), 
                        error = if (viewModel.cepError) "CEP inválido ou não encontrado" else null, 
                        trailingIcon = if (viewModel.isCepLoading) { { CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) } } else null
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
                    BooleanOption(label = "Rede de abastecimento ativa?", checked = viewModel.existeRedeAgua) { viewModel.existeRedeAgua = it }
                    BooleanOption(label = "Beneficiário de Tarifa Social?", checked = viewModel.beneficiarioSocial) { viewModel.beneficiarioSocial = it ?: false }
                    BooleanOption(label = "Consome água de vizinho?", checked = viewModel.usaAguaVizinho) { viewModel.usaAguaVizinho = it ?: false }
                    BooleanOption(label = "O imóvel possui piscina?", checked = viewModel.possuiPiscina) { viewModel.possuiPiscina = it }
                    Spacer(Modifier.height(8.dp))
                    SpinnerOption(label = "Tipo de Pavimento da Rua", options = listOf("Asfalto", "Paralelepípedo", "Terra", "Outro"), selectedOption = viewModel.pavimentoRua, onOptionSelected = { viewModel.pavimentoRua = it })
                    SpinnerOption(label = "Tipo de Pavimento da Calçada", options = listOf("Asfalto", "Paver", "Concreto", "Terra", "Outro"), selectedOption = viewModel.pavimentoCalcada, onOptionSelected = { viewModel.pavimentoCalcada = it })
                }
            }

            item {
                AppCard(title = "Hidrometria Técnica", icon = Icons.Default.WaterDrop) {
                    BooleanOption(label = "Medição via Hidrômetro?", checked = viewModel.possuiHidrometro) { viewModel.possuiHidrometro = it }
                    if (viewModel.possuiHidrometro == true) {
                        Spacer(Modifier.height(12.dp))
                        AppTextField(value = viewModel.numeroHidrometro, onValueChange = { viewModel.numeroHidrometro = it }, label = "Nº de Série do Hidrômetro", leadingIcon = Icons.Default.Pin)
                    }
                    Spacer(Modifier.height(12.dp))
                    SpinnerOption(label = "Local da Instalação", options = listOf("CAIXA PADRÃO", "INTERNO", "CAVALETE EXTERNO"), selectedOption = viewModel.localInstalacao, onOptionSelected = { viewModel.localInstalacao = it })
                    SpinnerOption(label = "Condição de Acessibilidade", options = listOf("Facil Acesso", "Dificil Acesso"), selectedOption = viewModel.acessibilidade, onOptionSelected = { viewModel.acessibilidade = it })
                }
            }

            // 👥 FASE 2: ENTREVISTA (Interação Humana)

            item {
                AppCard(title = "Responsável pelo Imóvel (Fatura)", icon = Icons.Default.Badge) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        val roles = listOf("Entrevistado", "Proprietario", "Locatario")
                        roles.forEachIndexed { index, role ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = roles.size),
                                onClick = { viewModel.currentRole = role },
                                selected = viewModel.currentRole == role
                            ) {
                                val label = when(role) {
                                    "Proprietario" -> "Proprietário"
                                    "Locatario" -> "Locatário"
                                    else -> role
                                }
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (viewModel.currentRole == "Entrevistado") {
                        Text("Vínculo do Responsável:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)) {
                            val options = listOf("Proprietário", "Locatário")
                            options.forEachIndexed { index, opt ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    onClick = { viewModel.entrevistadoVinculo = opt },
                                    selected = viewModel.entrevistadoVinculo == opt
                                ) {
                                    Text(opt, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    val activeData = viewModel.activeRoleData
                    val isLocked = viewModel.isCurrentRoleLocked

                    if (isLocked) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Dados espelhados para segurança.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    AppTextField(
                        value = activeData.nomeCompleto, 
                        onValueChange = { activeData.nomeCompleto = it }, 
                        label = "Nome Completo", 
                        leadingIcon = Icons.Default.Person, 
                        enabled = !isLocked
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = activeData.cpfCnpj, 
                        onValueChange = { if (it.length <= 14) activeData.cpfCnpj = it }, 
                        label = "CPF / CNPJ", 
                        leadingIcon = Icons.Default.Badge, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        visualTransformation = CpfCnpjVisualTransformation(), 
                        enabled = !isLocked,
                        error = if (!activeData.isDocValid) "Documento inválido" else null,
                        isValid = activeData.cpfCnpj.isNotBlank() && activeData.isDocValid
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = activeData.nomeMae, 
                        onValueChange = { activeData.nomeMae = it }, 
                        label = "Nome da Mãe", 
                        leadingIcon = Icons.Default.EscalatorWarning, 
                        enabled = !isLocked
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = activeData.dataNascimento, 
                        onValueChange = { if (it.length <= 8) activeData.dataNascimento = it }, 
                        label = "Data de Nascimento", 
                        leadingIcon = Icons.Default.CalendarToday, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                        visualTransformation = DateVisualTransformation(), 
                        enabled = !isLocked,
                        error = if (!activeData.isBirthDateValid) "Data inválida ou fora da faixa" else null,
                        isValid = activeData.dataNascimento.isNotBlank() && activeData.isBirthDateValid
                    )
                    SpinnerOption(label = "Sexo", options = listOf("Masculino", "Feminino", "Outro"), selectedOption = activeData.sexo, onOptionSelected = { activeData.sexo = it })
                    
                    Spacer(Modifier.height(12.dp))
                    BooleanOption(
                        label = "Apresentou documento oficial?", 
                        checked = activeData.apresentouDoc, 
                        onCheckedChange = { activeData.apresentouDoc = it }
                    )
                    
                    if (activeData.apresentouDoc == true) {
                        Spacer(Modifier.height(12.dp))
                        AppTextField(value = activeData.qualDoc, onValueChange = { activeData.qualDoc = it }, label = "Tipo do Documento", leadingIcon = Icons.Default.Description, enabled = !isLocked)
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Text("Canais de Notificações", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = viewModel.email, 
                        onValueChange = { viewModel.email = it.trim() }, 
                        label = "E-mail Principal", 
                        leadingIcon = Icons.Default.Email, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        error = if (!viewModel.isEmailValid) "Formato de e-mail inválido" else null,
                        isValid = viewModel.email.isNotBlank() && viewModel.isEmailValid
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(
                        value = viewModel.celular1, 
                        onValueChange = { if (it.length <= 11) viewModel.celular1 = it }, 
                        label = "Celular (WhatsApp)", 
                        leadingIcon = Icons.Default.PhoneIphone, 
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), 
                        visualTransformation = PhoneVisualTransformation(),
                        error = if (!viewModel.isCelular1Valid) "Número deve ter 10 ou 11 dígitos" else null,
                        isValid = viewModel.celular1.isNotBlank() && viewModel.isCelular1Valid
                    )
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = viewModel.celular2, onValueChange = { if (it.length <= 11) viewModel.celular2 = it }, label = "Telefone Recado", leadingIcon = Icons.Default.ContactPhone, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), visualTransformation = PhoneVisualTransformation())
                }
            }

            item {
                AppCard(title = "Observações", icon = Icons.Default.NoteAlt) {
                    AppTextField(value = viewModel.observacao, onValueChange = { viewModel.observacao = it }, label = "Notas de Campo", leadingIcon = Icons.AutoMirrored.Filled.Comment)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.medium) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("ANEXAR EVIDÊNCIA FOTOGRÁFICA")
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
