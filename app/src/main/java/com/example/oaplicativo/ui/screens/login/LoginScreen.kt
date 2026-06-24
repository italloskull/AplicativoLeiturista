package com.example.oaplicativo.ui.screens.login

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.ui.components.AppButton
import com.example.oaplicativo.ui.components.AppTextField
import com.example.oaplicativo.util.SecurityUtils
import com.example.oaplicativo.data.UpdateManager
import com.example.oaplicativo.data.AppUpdateInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loginState by viewModel.loginState.collectAsState()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    // SÊNIOR FIX: Carregamento inicial ultra-agressivo
    LaunchedEffect(Unit) {
        val isEnabled = SecurityUtils.isRememberMeEnabled(context)
        Log.i("LoginScreen", "🚀 Inicializando App. Lembrar Me: $isEnabled")
        
        if (isEnabled) {
            val savedUser = SecurityUtils.getRememberedIdentifier(context)
            val savedPass = SecurityUtils.getRememberedPassword(context)
            
            if (!savedUser.isNullOrBlank()) email = savedUser
            if (!savedPass.isNullOrBlank()) password = savedPass
            rememberMe = true
        }
        
        // VERIFICAÇÃO DE ATUALIZAÇÃO SÊNIOR
        val manager = UpdateManager(context)
        updateInfo = manager.checkForUpdates()
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            // SÊNIOR UX FIX: Garantimos o salvamento das credenciais ANTES de mudar de tela
            val identifier = email.lowercase().trim()
            val pass = password
            val rem = rememberMe
            
            Log.d("LoginSave", "Iniciando persistência: User=$identifier, Remember=$rem")
            SecurityUtils.saveCredentials(context, identifier, pass, rem)

            onLoginSuccess()
        }
    }

    val appVersion = com.example.oaplicativo.BuildConfig.VERSION_NAME

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) 
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // --- HEADER ---
            Spacer(Modifier.height(48.dp))
            
            Surface(
                modifier = Modifier.size(160.dp),
                color = Color.Transparent
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.oaplicativo.R.drawable.app_logo),
                    contentDescription = "Logo Recadastre.IA",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "RECADASTRE.IA", 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.Black, 
                color = Color.Black, // PRETO PURO
                letterSpacing = 3.sp
            )
            
            Text(
                text = "Informações Atualizadas", 
                style = MaterialTheme.typography.bodyMedium, 
                color = Color.Black, // PRETO PURO
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(48.dp))

            // --- CARD DE ACESSO ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Acesso ao Sistema", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Black,
                        color = Color.Black // PRETO PURO
                    )

                    // INPUT USUÁRIO (Blindagem Absoluta contra Tema do Sistema)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.lowercase().replace(Regex("[^a-z0-9]"), "") },
                        label = { Text("Nome de Usuário", color = Color.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        // REGRA DE OURO: Força cor do texto via TextStyle para PRETO PURO
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.Black, 
                            fontWeight = FontWeight.Bold
                        ),
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Person, 
                                contentDescription = null,
                                tint = Color.Black
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF0052CC),
                            unfocusedBorderColor = Color.Black,
                            focusedLabelColor = Color(0xFF0052CC),
                            unfocusedLabelColor = Color.Black,
                            cursorColor = Color.Black
                        )
                    )

                    // INPUT SENHA (Preto para visibilidade absoluta)
                    // INPUT SENHA (Blindagem Absoluta contra Tema do Sistema)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha de Acesso", color = Color.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        // REGRA DE OURO: Força cor do texto via TextStyle para PRETO PURO
                        textStyle = LocalTextStyle.current.copy(
                            color = Color.Black, 
                            fontWeight = FontWeight.Bold
                        ),
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Lock, 
                                contentDescription = null,
                                tint = Color.Black
                            ) 
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF0052CC),
                            unfocusedBorderColor = Color.Black,
                            focusedLabelColor = Color(0xFF0052CC),
                            unfocusedLabelColor = Color.Black,
                            cursorColor = Color.Black
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.Black
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF0052CC), // Azul Cobalto da marca
                                uncheckedColor = Color(0xFF94A3B8), // Cinza suave inativo
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Lembrar meu acesso", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475569), // Cinza azulado elegante
                            fontWeight = FontWeight.Medium
                        )
                    }

                    AppButton(
                        text = "Entrar no Sistema",
                        onClick = { 
                            // SÊNIOR UX FIX: Capturamos o estado do checkbox no exato momento do clique
                            Log.d("LoginAction", "Clique no botão. RememberMe está: $rememberMe")
                            // SÊNIOR QA FIX: Limpamos o cache anterior para garantir que o 'v7' seja soberano
                            SecurityUtils.saveCredentials(context, email, password, rememberMe)
                            viewModel.login(context, email, password, rememberMe) 
                        },
                        isLoading = loginState is LoginState.Loading,
                        containerColor = Color(0xFF0052CC), // Azul Cobalto da logo
                        contentColor = Color.White
                    )

                    if (loginState is LoginState.Error) {
                        Text(
                            text = (loginState as LoginState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            Text(
                text = "Versão $appVersion - Recadastre.IA", 
                style = MaterialTheme.typography.labelSmall, 
                color = Color.Black, 
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    // --- DIÁLOGO DE ATUALIZAÇÃO SÊNIOR ---
    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Nova Versão Disponível 🚀", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Uma nova versão (v${updateInfo!!.version_name}) está disponível.")
                    if (updateInfo!!.changelog.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(updateInfo!!.changelog, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    if (isDownloading) {
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Baixando: ${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isDownloading) {
                            scope.launch {
                                isDownloading = true
                                val manager = UpdateManager(context)
                                manager.downloadAndInstallApk(updateInfo!!.apk_url) { progress ->
                                    downloadProgress = progress
                                }
                                isDownloading = false
                            }
                        }
                    },
                    enabled = !isDownloading
                ) {
                    Text(if (isDownloading) "Baixando..." else "ATUALIZAR AGORA")
                }
            },
            dismissButton = {
                if (!isDownloading) {
                    TextButton(onClick = { updateInfo = null }) {
                        Text("DEPOIS")
                    }
                }
            }
        )
    }
}
