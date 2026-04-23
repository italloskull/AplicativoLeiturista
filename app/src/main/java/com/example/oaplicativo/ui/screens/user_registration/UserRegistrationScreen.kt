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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("usuário") }
    val registrationState by viewModel.registrationState.collectAsState()

    LaunchedEffect(registrationState) {
        if (registrationState is RegistrationState.Success) {
            onRegistrationSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastrar Novo Usuário") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Nome Completo") },
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nome de Usuário") },
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Papel do Usuário:")
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = cargo == "usuário",
                    onClick = { cargo = "usuário" },
                    enabled = registrationState !is RegistrationState.Loading
                )
                Text("Usuário")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = cargo == "administrador",
                    onClick = { cargo = "administrador" },
                    enabled = registrationState !is RegistrationState.Loading
                )
                Text("Administrador")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (registrationState is RegistrationState.Error) {
                Text(
                    text = (registrationState as RegistrationState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = { viewModel.register(fullName, username, email, password, cargo) },
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegistrationState.Loading
            ) {
                if (registrationState is RegistrationState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Cadastrar")
                }
            }
        }
    }
}