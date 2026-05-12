package com.example.oaplicativo.ui.screens.recadastro

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.ui.components.AppTextField
import com.example.oaplicativo.ui.components.AppButton
import com.example.oaplicativo.ui.components.AppCard
import com.example.oaplicativo.ui.components.SpinnerOption
import com.example.oaplicativo.ui.components.BooleanOption
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
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NOVO RECADASTRO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
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
                                // OBRIGATORIEDADE DE GPS: Se não tiver capturado, captura agora antes de salvar.
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
                                        return@launch // Aguarda permissão
                                    }
                                    isCapturingGpsOnSave = false
                                }
                                
                                // Prossegue com o salvamento blindado
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
            // 🏷️ IDENTIFICAÇÃO E LOCALIZAÇÃO
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
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (viewModel.latitude != null) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (viewModel.latitude != null) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                                    contentDescription = null,
                                    tint = if (viewModel.latitude != null) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = if (viewModel.latitude != null) "GPS Vinculado" else "GPS Necessário",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewModel.latitude != null) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                            if (viewModel.latitude != null) {
                                Text(
                                    "Lat: ${viewModel.latitude} / Long: ${viewModel.longitude}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Button(
                                onClick = { 
                                    val fineLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                    if (fineLoc == PackageManager.PERMISSION_GRANTED) {
                                        scope.launch {
                                            val loc = locationHelper.getCurrentLocation()
                                            viewModel.latitude = loc?.latitude
                                            viewModel.longitude = loc?.longitude
                                        }
                                    } else {
                                        locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                    }
                                },
                                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.latitude != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Atualizar Localização")
                            }
                        }
                    }
                }
            }

            // 👤 DADOS PESSOAIS
            item {
                AppCard(title = "Dados Pessoais", icon = Icons.Default.Badge) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        val roles = listOf("Entrevistado", "Proprietario", "Locatario")
                        roles.forEachIndexed { index, role ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = roles.size),
                                onClick = { viewModel.currentRole = role },
                                selected = viewModel.currentRole == role
                            ) {
                                Text(role, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (viewModel.currentRole == "Entrevistado") {
                        Text("Vínculo do Entrevistado:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)) {
                            val options = listOf("Proprietário", "Locatário", "Outro")
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
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("Dados espelhados para segurança.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    AppTextField(value = activeData.nomeCompleto, onValueChange = { activeData.nomeCompleto = it }, label = "Nome Completo", leadingIcon = Icons.Default.Person, enabled = !isLocked)
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = activeData.cpfCnpj, onValueChange = { if (it.length <= 14) activeData.cpfCnpj = it }, label = "CPF / CNPJ", leadingIcon = Icons.Default.Badge, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = CpfCnpjVisualTransformation(), enabled = !isLocked)
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = activeData.nomeMae, onValueChange = { activeData.nomeMae = it }, label = "Nome da Mãe", leadingIcon = Icons.Default.EscalatorWarning, enabled = !isLocked)
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = activeData.dataNascimento, onValueChange = { if (it.length <= 8) activeData.dataNascimento = it }, label = "Data de Nascimento", leadingIcon = Icons.Default.CalendarToday, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = DateVisualTransformation(), enabled = !isLocked)
                    
                    Spacer(Modifier.height(12.dp))
                    SpinnerOption(label = "Sexo", options = listOf("Masculino", "Feminino", "Outro"), selectedOption = activeData.sexo, onOptionSelected = { activeData.sexo = it })

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = activeData.apresentouDoc, onCheckedChange = { activeData.apresentouDoc = it }, enabled = !isLocked)
                        Spacer(Modifier.width(16.dp))
                        Text("Apresentou documento oficial?", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (activeData.apresentouDoc) {
                        Spacer(Modifier.height(12.dp))
                        AppTextField(value = activeData.qualDoc, onValueChange = { activeData.qualDoc = it }, label = "Tipo do Documento", leadingIcon = Icons.Default.Description, enabled = !isLocked)
                    }
                }
            }

            // 📞 CONTATO
            item {
                AppCard(title = "Canais de Contato do Morador", icon = Icons.Default.ContactPhone) {
                    AppTextField(value = viewModel.email, onValueChange = { viewModel.email = it }, label = "E-mail Principal do Morador", leadingIcon = Icons.Default.Email, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = viewModel.telefone, onValueChange = { viewModel.telefone = it }, label = "Telefone Fixo", leadingIcon = Icons.Default.Phone)
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = viewModel.celular1, onValueChange = { if (it.length <= 11) viewModel.celular1 = it }, label = "Celular Principal (WhatsApp)", leadingIcon = Icons.Default.PhoneIphone, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), visualTransformation = PhoneVisualTransformation())
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = viewModel.celular2, onValueChange = { if (it.length <= 11) viewModel.celular2 = it }, label = "Telefone Secundário / Recado", leadingIcon = Icons.Default.ContactPhone, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), visualTransformation = PhoneVisualTransformation())
                }
            }

            // 🏠 ENDEREÇO
            item {
                AppCard(title = "Endereço Residencial", icon = Icons.Default.Home) {
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

            // 🏗️ INFRAESTRUTURA
            item {
                AppCard(title = "Infraestrutura e Consumo", icon = Icons.Default.Foundation) {
                    BooleanOption(label = "Rede de abastecimento ativa?", checked = viewModel.existeRedeAgua) { viewModel.existeRedeAgua = it }
                    BooleanOption(label = "O imóvel possui piscina?", checked = viewModel.possuiPiscina) { viewModel.possuiPiscina = it }
                    Spacer(Modifier.height(8.dp))
                    SpinnerOption(label = "Possui reservatório (Caixa)?", options = listOf("Sim", "Não", "Não Visível"), selectedOption = viewModel.possuiCaixaAgua, onOptionSelected = { viewModel.possuiCaixaAgua = it })
                    Spacer(Modifier.height(16.dp))
                    AppTextField(value = viewModel.numeroMoradores, onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.numeroMoradores = it }, label = "Quantidade de Moradores", leadingIcon = Icons.Default.Group, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(Modifier.height(12.dp))
                    SpinnerOption(label = "Tipo de Pavimento da Rua", options = listOf("Asfalto", "Paralelepípedo", "Terra", "Outro"), selectedOption = viewModel.pavimentoRua, onOptionSelected = { viewModel.pavimentoRua = it })
                    Spacer(Modifier.height(12.dp))
                    SpinnerOption(label = "Tipo de Pavimento da Calçada", options = listOf("Asfalto", "Paver", "Concreto", "Terra", "Outro"), selectedOption = viewModel.pavimentoCalcada, onOptionSelected = { viewModel.pavimentoCalcada = it })
                }
            }

            // 💧 HIDROMETRIA
            item {
                AppCard(title = "Hidrometria Técnica", icon = Icons.Default.WaterDrop) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = viewModel.possuiHidrometro, onCheckedChange = { viewModel.possuiHidrometro = it })
                        Spacer(Modifier.width(16.dp))
                        Text("Medição via Hidrômetro?", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (viewModel.possuiHidrometro) {
                        Spacer(Modifier.height(12.dp))
                        AppTextField(value = viewModel.numeroHidrometro, onValueChange = { viewModel.numeroHidrometro = it }, label = "Nº de Série do Hidrômetro", leadingIcon = Icons.Default.Pin)
                    }
                    Spacer(Modifier.height(12.dp))
                    SpinnerOption(label = "Local da Instalação", options = listOf("CAIXA PADRÃO", "INTERNO", "CAVALETE EXTERNO"), selectedOption = viewModel.localInstalacao, onOptionSelected = { viewModel.localInstalacao = it })
                    Spacer(Modifier.height(12.dp))
                    SpinnerOption(label = "Condição de Acessibilidade", options = listOf("Facil Acesso", "Dificil Acesso"), selectedOption = viewModel.acessibilidade, onOptionSelected = { viewModel.acessibilidade = it })
                    Spacer(Modifier.height(12.dp))
                    AppTextField(value = viewModel.economias, onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.economias = it }, label = "Nº de Economias Vinculadas", leadingIcon = Icons.Default.MapsHomeWork, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number) )
                }
            }

            // 📝 FINALIZAÇÃO
            item {
                AppCard(title = "Observações de Campo", icon = Icons.Default.NoteAlt) {
                    AppTextField(
                        value = viewModel.observacao, 
                        onValueChange = { viewModel.observacao = it }, 
                        label = "Notas adicionais", 
                        leadingIcon = Icons.AutoMirrored.Filled.Comment,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { /* Abrir Câmera */ }, 
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
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
