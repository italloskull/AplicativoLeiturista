package com.example.oaplicativo.ui.screens.login

import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawBehind
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.R
import com.example.oaplicativo.ui.components.AppButton
import com.example.oaplicativo.ui.components.AppTextField
import com.example.oaplicativo.data.UpdateManager
import com.example.oaplicativo.data.AppUpdateInfo
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()
    val scope = rememberCoroutineScope()

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    // --- RESTAURAÇÃO DE CREDENCIAIS (SÊNIOR FIX) ---
    LaunchedEffect(Unit) {
        val savedUser = com.example.oaplicativo.util.SecurityUtils.getRememberedIdentifier(context)
        val savedPass = com.example.oaplicativo.util.SecurityUtils.getRememberedPassword(context)
        val isRememberEnabled = com.example.oaplicativo.util.SecurityUtils.isRememberMeEnabled(context)
        
        if (isRememberEnabled && savedUser != null) {
            identifier = savedUser
            password = savedPass ?: ""
            rememberMe = true
        }
    }

    // --- LÓGICA DE ATUALIZAÇÃO ---
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val manager = UpdateManager(context)
        val info = manager.checkForUpdates()
        if (info != null) {
            updateInfo = info
        }
    }

    val appVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (_: Exception) { "0.0.0" }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(60.dp))
                
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(150.dp)
                )

                Text(
                    "Recadastre.IA", 
                    style = MaterialTheme.typography.headlineMedium, 
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E293B)
                )
                Text(
                    "Gestão de Saneamento", 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = Color(0xFF64748B)
                )

                Spacer(Modifier.height(48.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        AppTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = "Usuário ou E-mail",
                            leadingIcon = Icons.Default.Person,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        AppTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Senha de Acesso",
                            leadingIcon = Icons.Default.Lock,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )

                        Spacer(Modifier.height(24.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                            Text("Lembrar meu acesso", style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(24.dp))

                        AppButton(
                            text = if (loginState is LoginState.Loading) "Entrando..." else "Entrar no Sistema",
                            isLoading = loginState is LoginState.Loading,
                            onClick = { 
                                if (identifier.isNotBlank() && password.isNotBlank()) {
                                    viewModel.login(context, identifier, password, rememberMe)
                                } else {
                                    Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        if (loginState is LoginState.Error) {
                            Spacer(Modifier.height(16.dp))
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
                
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Versão $appVersion - Recadastre.IA", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = Color.Black, 
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { },
                            onLongClick = {
                                // SÊNIOR UX: Feedback tátil via sistema
                                val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                updateInfo = AppUpdateInfo(
                                    version_name = "DEMO",
                                    apk_url = "",
                                    changelog = "🚀 Teste da nova animação de download.\n📊 Interface fluida e moderna.\n🌊 Efeito balde d'água ativo.",
                                    fileSize = 1000000
                                )
                                scope.launch {
                                    isDownloading = true
                                    downloadProgress = 0f
                                    while(downloadProgress < 1f) {
                                        kotlinx.coroutines.delay(50)
                                        downloadProgress += 0.01f
                                    }
                                    isDownloading = false
                                }
                            }
                        )
                )
            }
        }
    }

    // --- RE-DESIGN DE ATUALIZAÇÃO SÊNIOR (UX ELITE) ---
    if (updateInfo != null) {
        // SÊNIOR MOTION: Inundação GRADUAL que se expande do balde
        val floodRadius = remember { Animatable(0f) }
        
        LaunchedEffect(downloadProgress) {
            if (downloadProgress >= 1f) {
                floodRadius.animateTo(
                    targetValue = 2000f, // Raio para cobrir a tela toda
                    animationSpec = tween(2500, easing = LinearOutSlowInEasing)
                )
            } else {
                floodRadius.snapTo(0f)
            }
        }

        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .drawBehind {
                        // Desenha a "água escorrendo" que se expande do centro
                        if (floodRadius.value > 0f) {
                            drawCircle(
                                color = Color(0xFF38BDF8),
                                radius = floodRadius.value,
                                center = center
                            )
                        }
                    }
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = if (downloadProgress >= 1f) Color.White.copy(alpha = 0.9f) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (downloadProgress < 1f) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudDownload, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        } else {
                            // Ícone de Sucesso pós-transbordamento
                            Icon(
                                Icons.Default.CheckCircle, 
                                contentDescription = null, 
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(80.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Text(
                            if (downloadProgress >= 1f) "Pronto para evoluir!" else "O App evoluiu!",
                            style = MaterialTheme.typography.headlineSmall, 
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1E293B)
                        )
                        
                        Text(
                            "Versão v${updateInfo!!.version_name}", 
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(24.dp))

                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "O QUE HÁ DE NOVO:", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(Modifier.height(8.dp))
                                
                                val cleanLog = updateInfo!!.changelog
                                    .replace(Regex("https?://\\S+"), "")
                                    .replace("**Full Changelog**:", "")
                                    .trim()
                                
                                Text(
                                    cleanLog.ifBlank { "Melhorias de performance e estabilidade para a equipe de campo." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF334155),
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        if (isDownloading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                com.example.oaplicativo.ui.animations.WaterBucketLoader(
                                    progress = downloadProgress,
                                    modifier = Modifier.size(120.dp, 160.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Preenchendo o balde: ${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isDownloading = true
                                            val manager = UpdateManager(context)
                                            manager.downloadAndInstallApk(updateInfo!!.apk_url) { p ->
                                                downloadProgress = p
                                            }
                                            isDownloading = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.RocketLaunch, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("ATUALIZAR AGORA", fontWeight = FontWeight.Black)
                                }

                                TextButton(
                                    onClick = { updateInfo = null },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Depois", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
