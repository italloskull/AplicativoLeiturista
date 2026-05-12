package com.example.oaplicativo.ui.screens.user_registration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

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
                title = { Text("Novo Leiturista", style = MaterialTheme.typography.titleMedium) },
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
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading,
                singleLine = true,
                shape = MaterialTheme.shapes.medium
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
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = showCidadeMenu,
                    onDismissRequest = { showCidadeMenu = false }
                ) {
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
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha de Acesso") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading,
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Nível de permissão
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
                        Text("Leiturista")
                        Spacer(modifier = Modifier.width(20.dp))
                        RadioButton(selected = cargo == "administrador", onClick = { cargo = "administrador" })
                        Text("Admin")
                    }
                }
            }

            if (registrationState is RegistrationState.Error) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
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
                    val autoEmail = "${cleanUsername}@leiturista.app"
                    viewModel.register(fullName.trim(), cleanUsername, autoEmail, password, cargo, selectedCidadeId)
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
