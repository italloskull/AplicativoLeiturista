package com.example.oaplicativo.ui.screens.user_registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.oaplicativo.data.repository.AuthRepositoryImpl
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.Cidade
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserRegistrationScreen(
    onRegistrationSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: UserRegistrationViewModel = viewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("usuário") }
    
    // SÊNIOR REATIVITY FIX: Usar Set para garantir que o Compose perceba a mudança de ESTADO do conjunto todo
    var selectedCidadesSet by remember { mutableStateOf(setOf<String>()) }

    // SÊNIOR CONSISTENCY FIX: Se mudar de Admin para Leiturista, limpa as cidades para evitar multi-seleção indevida
    LaunchedEffect(cargo) {
        if (cargo == "usuário" && selectedCidadesSet.size > 1) {
            selectedCidadesSet = emptySet()
        }
    }

    val cidades by viewModel.cidades.collectAsState()
    val registrationState by viewModel.registrationState.collectAsState()

    val isFormValid = remember(fullName, username, password, selectedCidadesSet) {
        fullName.trim().length >= 3 &&
                username.trim().length >= 3 &&
                password.length >= 6 &&
                selectedCidadesSet.isNotEmpty()
    }

    LaunchedEffect(Unit) { viewModel.loadCidades() }

    LaunchedEffect(registrationState) {
        if (registrationState is RegistrationState.Success) onRegistrationSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Usuário", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Cadastre o acesso para o colaborador de campo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nome Completo") },
                placeholder = { Text("Ex: João da Silva") },
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                supportingText = {
                    val isOk = fullName.trim().length >= 3
                    if (fullName.isNotEmpty()) {
                        Text(
                            if (isOk) "✅ Nome válido" else "❌ Mínimo 3 caracteres",
                            color = if (isOk) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it.lowercase().replace(Regex("[^a-z0-9]"), "")
                },
                label = { Text("Nome de Usuário (Login)") },
                placeholder = { Text("Apenas letras e números") },
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                supportingText = {
                    val isOk = username.trim().length >= 3
                    if (username.isNotEmpty()) {
                        Text(
                            if (isOk) "✅ Usuário disponível" else "❌ Mínimo 3 caracteres (letras/números)",
                            color = if (isOk) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha de Acesso") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading,
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                supportingText = {
                    val isOk = password.length >= 6
                    if (password.isNotEmpty()) {
                        Text(
                            if (isOk) "✅ Senha forte" else "❌ Mínimo 6 caracteres",
                            color = if (isOk) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            // Nível de permissão
            val userProfileState = AuthRepositoryImpl.getInstance().currentUserProfile.collectAsState()
            val isDeveloper = userProfileState.value?.cargo?.lowercase() == "desenvolvedor"

            if (isDeveloper) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Nível de Permissão:", style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = cargo == "usuário", onClick = { cargo = "usuário" })
                            Text("Equipe de Campo")
                            Spacer(modifier = Modifier.width(20.dp))
                            RadioButton(selected = cargo == "administrador", onClick = { cargo = "administrador" })
                            Text("Admin")
                        }
                    }
                }
            } else {
                SideEffect { if (cargo != "usuário") cargo = "usuário" }
            }

            // --- SÊNIOR UX: SELEÇÃO DE CIDADES VIA CHECKBOX ---
            Text(
                "Cidades Autorizadas",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                fontWeight = FontWeight.Bold
            )
            
            if (selectedCidadesSet.isEmpty()) {
                Text(
                    "❌ Selecione pelo menos uma cidade",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    "✅ ${selectedCidadesSet.size} cidade(s) selecionada(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF10B981),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(8.dp)) {
                    if (cidades.isEmpty()) {
                        Text("Carregando cidades...", modifier = Modifier.padding(16.dp))
                    }
                    cidades.forEach { cidade ->
                        val isSelected = selectedCidadesSet.contains(cidade.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        val newSet = selectedCidadesSet.toMutableSet()
                                        if (isSelected) {
                                            newSet.remove(cidade.id)
                                        } else {
                                            if (cargo == "usuário") newSet.clear()
                                            newSet.add(cidade.id)
                                        }
                                        selectedCidadesSet = newSet
                                    }
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    val newSet = selectedCidadesSet.toMutableSet()
                                    if (it) {
                                        if (cargo == "usuário") newSet.clear()
                                        newSet.add(cidade.id)
                                    } else {
                                        newSet.remove(cidade.id)
                                    }
                                    selectedCidadesSet = newSet
                                }
                            )
                            Text(cidade.nome, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (registrationState is RegistrationState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(
                        text = (registrationState as RegistrationState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    val cleanUsername = username.lowercase().trim()
                    val autoEmail = "${cleanUsername}@equipedecampo.app"
                    viewModel.register(
                        name = fullName.trim(),
                        email = autoEmail,
                        pass = password,
                        user = cleanUsername,
                        role = cargo,
                        cidades = selectedCidadesSet.toList()
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isFormValid && registrationState !is RegistrationState.Loading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (registrationState is RegistrationState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("CRIAR ACESSO IMEDIATO")
                }
            }
        }
    }
}
