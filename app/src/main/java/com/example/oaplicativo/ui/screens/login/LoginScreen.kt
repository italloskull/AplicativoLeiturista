package com.example.oaplicativo.ui.screens.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.data.AppUpdateInfo
import com.example.oaplicativo.data.UpdateManager
import com.example.oaplicativo.presentation.components.AppButton
import com.example.oaplicativo.presentation.components.AppTextField
import com.example.oaplicativo.util.SecurityUtils
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    var identifier by remember { mutableStateOf(SecurityUtils.getRememberedIdentifier(context) ?: "") }
    var password by remember { mutableStateOf(SecurityUtils.getRememberedPassword(context) ?: "") }
    var rememberMe by remember { mutableStateOf(SecurityUtils.isRememberMeEnabled(context)) }
    val loginState by viewModel.loginState.collectAsState()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "Login Recadastre.IA", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(32.dp))
                AppTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = "E-mail ou Usuário",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "login_field",
                    leadingIcon = Icons.Filled.Email
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Senha",
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "password_field",
                    leadingIcon = Icons.Filled.Lock,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        enabled = loginState !is LoginState.Loading
                    )
                    Text(
                        text = "Lembrar-me",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                if (loginState is LoginState.Error) {
                    Text(
                        text = (loginState as LoginState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                AppButton(
                    text = "Entrar",
                    onClick = { viewModel.login(context, identifier, password, rememberMe) },
                    isLoading = loginState is LoginState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

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

        Text(
            text = "Versão: ${com.example.oaplicativo.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}