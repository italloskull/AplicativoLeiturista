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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.Cidade

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedCidadeId by remember { mutableStateOf("") }
    var selectedCidadeNome by remember { mutableStateOf("Selecionar cidade...") }
    var showCidadeMenu by remember { mutableStateOf(false) }

    val cidades by viewModel.cidades.collectAsState()
    val registrationState by viewModel.registrationState.collectAsState()

    val isFormValid = remember(fullName, username, password, selectedCidadeId) {
        fullName.trim().length >= 3 &&
                username.trim().length >= 3 &&
                password.length >= 6 &&
                selectedCidadeId.isNotBlank()
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
                    Text(
                        if (fullName.trim().length < 3) "Mínimo 3 caracteres" else "Nome válido ✅",
                        color = if (fullName.trim().length < 3) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF10B981)
                    )
                },
                isError = fullName.isNotEmpty() && fullName.trim().length < 3
            )

            // --- SELETOR DE CIDADE ---
            ExposedDropdownMenuBox(
                expanded = showCidadeMenu,
                onExpandedChange = { if (registrationState !is RegistrationState.Loading) showCidadeMenu = !showCidadeMenu }
            ) {
                OutlinedTextField(
                    value = selectedCidadeNome,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cidade de Atuação") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCidadeMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    shape = MaterialTheme.shapes.medium,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    supportingText = {
                        if (selectedCidadeId.isBlank()) {
                            Text("Campo obrigatório *", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Cidade selecionada ✅", color = Color(0xFF10B981))
                        }
                    }
                )
                ExposedDropdownMenu(
                    expanded = showCidadeMenu,
                    onDismissRequest = { showCidadeMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (cidades.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Nenhuma cidade disponível", color = MaterialTheme.colorScheme.error) },
                            onClick = { showCidadeMenu = false }
                        )
                    } else {
                        cidades.forEach { cidade ->
                            DropdownMenuItem(
                                text = { Text(cidade.nome) },
                                onClick = {
                                    selectedCidadeId = cidade.id
                                    selectedCidadeNome = cidade.nome
                                    showCidadeMenu = false
                                }
                            )
                        }
                    }
                }
            }

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
                    Text(
                        if (username.length < 3) "Mínimo 3 caracteres (letras/números)" else "Usuário disponível ✅",
                        color = if (username.length < 3) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF10B981)
                    )
                },
                isError = username.isNotEmpty() && username.length < 3
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
                    Text(
                        if (password.length < 6) "Mínimo 6 caracteres" else "Senha forte ✅",
                        color = if (password.length < 6) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF10B981)
                    )
                },
                isError = password.isNotEmpty() && password.length < 6
            )

            // Nível de permissão (SÊNIOR FIX: Hierarquia de criação)
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
                // Se for administrador comum, força o cargo "usuário" (Equipe de Campo)
                SideEffect {
                    if (cargo != "usuário") cargo = "usuário"
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Tipo de Acesso: Equipe de Campo",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
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
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Button(
                onClick = {
                    val cleanUsername = username.lowercase().trim()
                    val autoEmail = "${cleanUsername}@equipedecampo.app"
                    // SÊNIOR FIX: Ordem dos parâmetros corrigida para bater com o ViewModel
                    // De: (name, user, email, pass, role, cidadeId)
                    // Para: (name, email, pass, user, role, cidadeId)
                    viewModel.register(
                        name = fullName.trim(),
                        email = autoEmail,
                        pass = password,
                        user = cleanUsername,
                        role = cargo,
                        cidadeId = selectedCidadeId
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
