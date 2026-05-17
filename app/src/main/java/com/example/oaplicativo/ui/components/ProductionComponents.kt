package com.example.oaplicativo.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 🚀 DESIGN SYSTEM DE ELITE - COMPONENTES DE NÍVEL DE PRODUÇÃO
 * Foco: Resiliência, Acessibilidade e Performance.
 */

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
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics { contentDescription = if (isLoading) "Processando" else text },
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Crossfade(targetState = isLoading, label = "BtnAnim") { loading ->
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = contentColor, strokeWidth = 2.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(text = text.uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                }
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
    trailingIcon: @Composable (() -> Unit)? = null,
    error: String? = null,
    isValid: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    readOnly: Boolean = false
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
            shape = MaterialTheme.shapes.medium,
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = when {
                error != null -> { { Icon(Icons.Default.Error, "Erro", tint = MaterialTheme.colorScheme.error) } }
                trailingIcon != null -> trailingIcon
                isValid -> { { Icon(Icons.Default.CheckCircle, "OK", tint = Color(0xFF4CAF50)) } }
                else -> null
            },
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            singleLine = true
        )
        AnimatedVisibility(visible = error != null) {
            Text(
                text = error ?: "", 
                color = MaterialTheme.colorScheme.error, 
                style = MaterialTheme.typography.bodySmall, 
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun AppCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), 
                    shape = MaterialTheme.shapes.small, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { 
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) 
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = title.uppercase(), 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun GpsStatusCard(
    latitude: Double?,
    longitude: Double?,
    onUpdateClick: () -> Unit,
    isLoading: Boolean = false
) {
    val hasLocation = latitude != null && longitude != null
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasLocation) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasLocation) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                    contentDescription = null,
                    tint = if (hasLocation) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (hasLocation) "GPS Vinculado" else "GPS Necessário",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (hasLocation) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
            if (hasLocation) {
                Text(
                    "Lat: $latitude / Long: $longitude",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Button(
                onClick = onUpdateClick,
                modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasLocation) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Atualizar Localização")
                }
            }
        }
    }
}

@Composable
fun AppStatusBadge(
    status: String?,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (status) {
        "Boa" -> Color(0xFF4CAF50) to "Boa"
        "Regular" -> Color(0xFFFFC107) to "Regular"
        "Ruim" -> Color(0xFFF44336) to "Ruim"
        else -> MaterialTheme.colorScheme.outline to "Indefinida"
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(10.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SpinnerOption(
    label: String,
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T?) -> Unit,
    optionToString: (T) -> String = { it.toString() }
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExposedDropdownMenuBox(
            expanded = expanded, 
            onExpandedChange = { expanded = !expanded }, 
            modifier = Modifier.padding(top = 4.dp)
        ) {
            OutlinedTextField(
                value = selectedOption?.let { optionToString(it) } ?: "Selecione...",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                shape = MaterialTheme.shapes.medium
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Nenhum") }, onClick = { onOptionSelected(null); expanded = false })
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(optionToString(option)) }, onClick = { onOptionSelected(option); expanded = false })
                }
            }
        }
    }
}

@Composable
fun BooleanOption(label: String, checked: Boolean?, onCheckedChange: (Boolean?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), 
        verticalAlignment = Alignment.CenterVertically, 
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row {
            FilterChip(
                selected = checked == true, 
                onClick = { onCheckedChange(if (checked == true) null else true) }, 
                label = { Text("Sim") }
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = checked == false, 
                onClick = { onCheckedChange(if (checked == false) null else false) }, 
                label = { Text("Não") }
            )
        }
    }
}
