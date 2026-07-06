package com.example.oaplicativo.ui.screens.user_management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oaplicativo.model.UserProfile
import com.example.oaplicativo.ui.components.AppButton
import com.example.oaplicativo.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    viewModel: UserManagementViewModel = viewModel()
) {
    val users by viewModel.users.collectAsState()
    val cidades by viewModel.cidades.collectAsState()
    val state by viewModel.state.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GESTÃO DE EQUIPE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state is UserManagementState.Loading && users.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    UserCard(user) {
                        viewModel.startEditing(user)
                        showEditDialog = true
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        UserEditDialog(
            user = viewModel.editingUser,
            allCidades = cidades,
            selectedCidades = viewModel.selectedCidades,
            onDismiss = { showEditDialog = false },
            onSave = {
                viewModel.saveUserPermissions {
                    showEditDialog = false
                }
            },
            onCargoChange = { newCargo ->
                viewModel.editingUser = viewModel.editingUser?.copy(cargo = newCargo)
            }
        )
    }
}

@Composable
fun UserCard(user: UserProfile, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (user.cargo.lowercase() == "leiturista" || user.cargo.lowercase() == "usuário") 
                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (user.cargo.lowercase().contains("admin")) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                        null,
                        tint = if (user.cargo.lowercase().contains("admin")) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName ?: user.username ?: "Sem Nome", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(user.cargo.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar Acesso", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserEditDialog(
    user: UserProfile?,
    allCidades: List<com.example.oaplicativo.model.Cidade>,
    selectedCidades: MutableList<String>,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onCargoChange: (String) -> Unit
) {
    if (user == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Acesso: ${user.username}", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 1. Cargo
                Text("Nível de Permissão", style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = user.cargo == "usuário" || user.cargo == "leiturista", onClick = { onCargoChange("usuário") })
                    Text("Leiturista")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = user.cargo == "administrador", onClick = { onCargoChange("administrador") })
                    Text("Admin")
                }

                HorizontalDivider()

                // 2. Cidades (Checkboxes)
                Text("Cidades Autorizadas", style = MaterialTheme.typography.labelMedium)
                allCidades.forEach { cidade ->
                    val isSelected = selectedCidades.contains(cidade.id)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    // Regra: se não for admin, limpa os outros (seleção única)
                                    if (!user.cargo.lowercase().contains("admin")) {
                                        selectedCidades.clear()
                                    }
                                    selectedCidades.add(cidade.id)
                                } else {
                                    selectedCidades.remove(cidade.id)
                                }
                            }
                        )
                        Text(cidade.nome, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("SALVAR ALTERAÇÕES", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}
