package com.example.oaplicativo.ui.screens.login

import android.content.Context
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
        checkUpdates(context, scope)
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
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
                        onClick = { viewModel.login(context, email, password, rememberMe) },
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
}

private fun checkUpdates(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    scope.launch {
        val updateManager = UpdateManager(context)
        val updateInfo = updateManager.checkForUpdates()
        if (updateInfo != null) { 
            Log.d("UpdateManager", "Nova atualização disponível: ${updateInfo.version_name}")
        }
    }
}
