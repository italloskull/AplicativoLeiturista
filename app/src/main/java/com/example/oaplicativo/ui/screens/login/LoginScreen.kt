package com.example.oaplicativo.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.data.AppUpdateInfo
import com.example.oaplicativo.data.UpdateManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val loginState by viewModel.loginState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager(context) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showNoUpdateToast by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    fun checkUpdates(manual: Boolean = false) {
        scope.launch {
            if (manual) isCheckingUpdate = true
            val info = updateManager.checkForUpdates()
            if (manual) isCheckingUpdate = false
            
            if (info != null) {
                updateInfo = info
                showUpdateDialog = true
            } else if (manual) {
                showNoUpdateToast = true
            }
        }
    }

    LaunchedEffect(Unit) {
        checkUpdates(manual = false)
    }

    if (showNoUpdateToast) {
        LaunchedEffect(showNoUpdateToast) {
            android.widget.Toast.makeText(context, "O aplicativo já está atualizado", android.widget.Toast.LENGTH_SHORT).show()
            showNoUpdateToast = false
        }
    }

    if (showUpdateDialog && updateInfo != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Atualização Disponível") },
            text = { Text("Uma nova versão do aplicativo está disponível.\n\nNovidades:\n${updateInfo?.changelog}") },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    updateInfo?.let { info ->
                        showDownloadDialog = true
                        scope.launch {
                            try {
                                android.util.Log.d("LoginScreen", "Iniciando download via botão: ${info.apk_url}")
                                updateManager.downloadAndInstallApk(info.apk_url) { progress ->
                                    downloadProgress = progress
                                }
                            } catch (e: Exception) {
                                showDownloadDialog = false
                                downloadProgress = null
                                android.util.Log.e("LoginScreen", "Erro ao processar download", e)
                                android.widget.Toast.makeText(context, "Erro ao baixar: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) {
                    Text("Baixar e Instalar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Agora não")
                }
            }
        )
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { /* Bloqueia fechar durante download */ },
            title = { Text("Baixando Atualização") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val progress = downloadProgress ?: 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    Text("${(progress * 100).toInt()}%")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Por favor, aguarde enquanto baixamos a nova versão...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {} // Sem botões, fecha ao terminar
        )
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Login Saneamento", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is LoginState.Loading
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is LoginState.Loading
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (loginState is LoginState.Error) {
            Text(
                text = (loginState as LoginState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = { viewModel.login(username, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is LoginState.Loading
        ) {
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Entrar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { checkUpdates(manual = true) },
            enabled = !isCheckingUpdate
        ) {
            if (isCheckingUpdate) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Verificar se há atualizações")
        }
    }
}