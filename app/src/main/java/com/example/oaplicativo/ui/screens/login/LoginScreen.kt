package com.example.oaplicativo.ui.screens.login

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.example.oaplicativo.data.UpdateManager
import com.example.oaplicativo.util.SecurityUtils
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

    LaunchedEffect(Unit) {
        if (SecurityUtils.isRememberMeEnabled(context)) {
            email = SecurityUtils.getRememberedIdentifier(context) ?: ""
            password = SecurityUtils.getRememberedPassword(context) ?: ""
            rememberMe = true
        }
        checkUpdates(context, scope, silent = true)
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.WaterDrop, null, modifier = Modifier.size(48.dp), tint = Color.White) }
            }

            Spacer(Modifier.height(24.dp))
            Text("RECADASTRE.IA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp)
            Text("Informações Atualizadas", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(48.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Acesso ao Sistema", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))

                    // SOLUÇÃO: Login via Nome de Usuário
                    AppTextField(
                        value = email,
                        onValueChange = { email = it.lowercase().replace(Regex("[^a-z0-9]"), "") },
                        label = "Nome de Usuário",
                        leadingIcon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AppTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Senha",
                        leadingIcon = Icons.Default.Lock,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                        Text("Lembrar de mim", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    AppButton(
                        text = "Entrar",
                        onClick = { viewModel.login(context, email, password, rememberMe) },
                        isLoading = loginState is LoginState.Loading
                    )

                    if (loginState is LoginState.Error) {
                        Text(text = (loginState as LoginState.Error).message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}

private fun checkUpdates(context: Context, scope: kotlinx.coroutines.CoroutineScope, silent: Boolean) {
    scope.launch {
        val updateManager = UpdateManager(context)
        val updateInfo = updateManager.checkForUpdates()
        if (updateInfo != null) { /* showDialog */ }
    }
}
