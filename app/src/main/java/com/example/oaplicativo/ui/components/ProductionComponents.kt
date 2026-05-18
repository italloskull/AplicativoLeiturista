package com.example.oaplicativo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 🚀 COMPONENTES PADRONIZADOS - TEMA DINÂMICO MATERIAL 3
 */

@Composable
fun GlobalActionMenu(
    isDarkTheme: Boolean,
    isAdmin: Boolean = false,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToUserRegistration: (() -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Menu de Opções",
                tint = tint
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // OPÇÃO DE TEMA
            DropdownMenuItem(
                text = { Text(if (isDarkTheme) "Modo Claro" else "Modo Escuro") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onToggleTheme()
                }
            )

            // OPÇÃO ADMINISTRATIVA (Apenas para ADM)
            if (isAdmin && onNavigateToUserRegistration != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                DropdownMenuItem(
                    text = { Text("Gerenciar Usuários") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        expanded = false
                        onNavigateToUserRegistration()
                    }
                )
            }

            // OPÇÃO DE SAIR
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
            DropdownMenuItem(
                text = { Text("Sair do Sistema", color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    expanded = false
                    onLogout()
                }
            )
        }
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        if (isLoading) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = contentColor, strokeWidth = 2.dp)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(icon, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Text(text.uppercase(), fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    error: String? = null,
    isValid: Boolean = false,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle? = null,
    colors: TextFieldColors? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            isError = error != null,
            textStyle = textStyle ?: LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium),
            shape = MaterialTheme.shapes.medium,
            leadingIcon = leadingIcon?.let { { Icon(it, null) } },
            trailingIcon = when {
                error != null -> { { Icon(Icons.Default.Error, "Erro", tint = MaterialTheme.colorScheme.error) } }
                isValid -> { { Icon(Icons.Default.CheckCircle, "OK", tint = Color(0xFF4CAF50)) } }
                else -> trailingIcon
            },
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            singleLine = true,
            colors = colors ?: OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

@Composable
fun AppCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title.uppercase(), 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun BooleanOption(label: String, checked: Boolean?, onCheckedChange: (Boolean?) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onCheckedChange(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (checked == true) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (checked == true) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text("Sim", fontWeight = FontWeight.Bold) }
            
            Button(
                onClick = { onCheckedChange(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (checked == false) Color(0xFFF44336) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (checked == false) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text("Não", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun GpsStatusCard(latitude: Double?, longitude: Double?, onUpdateClick: () -> Unit, isLoading: Boolean = false) {
    val hasLoc = latitude != null
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasLoc) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (hasLoc) Color(0xFF4CAF50) else Color(0xFFF44336))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (hasLoc) "GPS Vinculado" else "GPS Necessário", 
                fontWeight = FontWeight.Black, 
                color = if (hasLoc) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            if (hasLoc) Text("Lat: $latitude / Lon: $longitude", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Button(onClick = onUpdateClick, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                else Text("Atualizar Localização", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AppStatusBadge(status: String?, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        "Boa" -> Color(0xFF4CAF50) to "Boa"
        "Regular" -> Color(0xFFFFC107) to "Regular"
        "Ruim" -> Color(0xFFF44336) to "Ruim"
        else -> MaterialTheme.colorScheme.outline to "???"
    }
    Surface(color = color.copy(alpha = 0.1f), contentColor = color, shape = CircleShape, border = BorderStroke(0.5.dp, color), modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
fun <T> SpinnerOption(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    optionToString: (T) -> String = { it.toString() }
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(
                onClick = { expanded = true }, 
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedOption?.let { optionToString(it) } ?: "Selecione...")
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Nenhum") }, onClick = { onOptionSelected(null); expanded = false })
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(optionToString(opt)) }, 
                        onClick = { onOptionSelected(opt); expanded = false }
                    )
                }
            }
        }
    }
}
