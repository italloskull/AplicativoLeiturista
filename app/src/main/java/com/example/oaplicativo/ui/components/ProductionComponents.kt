package com.example.oaplicativo.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MOTOR DE FEEDBACK HÁPTICO (SÊNIOR)
 * Centraliza as vibrações para garantir consistência e economia de bateria.
 */
object HapticFeedback {
    fun success(context: Context) {
        vibrate(context, 50)
    }

    fun error(context: Context) {
        val pattern = longArrayOf(0, 50, 100, 50)
        vibratePattern(context, pattern)
    }

    fun tick(context: Context) {
        vibrate(context, 10)
    }

    private fun vibrate(context: Context, duration: Long) {
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern(context: Context, pattern: LongArray) {
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalActionMenu(
    isDarkTheme: Boolean,
    isAdmin: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToUserRegistration: (() -> Unit)? = null,
    onForceSync: (() -> Unit)? = null, // SÊNIOR FIX: Adicionado botão de sync manual
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box {
        IconButton(onClick = { 
            HapticFeedback.tick(context)
            showMenu = true 
        }) {
            Icon(Icons.Default.MoreVert, "Menu", tint = tint)
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            DropdownMenuItem(
                text = { Text("Trocar Tema") },
                leadingIcon = { Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, null) },
                onClick = {
                    HapticFeedback.tick(context)
                    onToggleTheme()
                    showMenu = false
                }
            )

            if (isAdmin && onNavigateToUserRegistration != null) {
                DropdownMenuItem(
                    text = { Text("Gerenciar Usuários") },
                    leadingIcon = { Icon(Icons.Default.GroupAdd, null) },
                    onClick = {
                        HapticFeedback.tick(context)
                        onNavigateToUserRegistration()
                        showMenu = false
                    }
                )
            }

            if (onForceSync != null) {
                DropdownMenuItem(
                    text = { Text("Forçar Sincronização") },
                    leadingIcon = { Icon(Icons.Default.Sync, null) },
                    onClick = {
                        HapticFeedback.tick(context)
                        onForceSync()
                        showMenu = false
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            DropdownMenuItem(
                text = { Text("Sair do Sistema", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    HapticFeedback.error(context)
                    onLogout()
                    showMenu = false
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
    val context = LocalContext.current
    Button(
        onClick = {
            HapticFeedback.success(context)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.large,
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = contentColor, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
    icon: ImageVector? = null, 
    leadingIcon: ImageVector? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    textStyle: TextStyle? = null,
    colors: TextFieldColors? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val finalLeadingIcon = leadingIcon ?: icon
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontWeight = FontWeight.Medium) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        leadingIcon = finalLeadingIcon?.let { { Icon(it, null, tint = MaterialTheme.colorScheme.primary) } },
        trailingIcon = trailingIcon,
        isError = isError,
        singleLine = singleLine,
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        textStyle = textStyle ?: LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
        colors = colors ?: OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun BooleanOption(
    label: String,
    selectedOption: String? = null,
    onOptionSelected: (String?) -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Sim", "Não").forEach { option ->
                val selected = selectedOption == option
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable { 
                            HapticFeedback.tick(context)
                            onOptionSelected(option) 
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(option, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GpsStatusCard(
    lat: Double? = null,
    lng: Double? = null,
    latitude: Double? = null,
    longitude: Double? = null,
    onUpdate: () -> Unit = {},
    onUpdateClick: () -> Unit = {},
    isCapturing: Boolean = false,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val finalLat = latitude ?: lat
    val finalLng = longitude ?: lng
    val finalOnUpdate = if (latitude != null || onUpdateClick != {}) onUpdateClick else onUpdate
    val finalIsCapturing = isCapturing || isLoading
    
    val hasGps = finalLat != null && finalLng != null
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (hasGps) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (hasGps) Color(0xFFBBF7D0) else Color(0xFFFECACA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (hasGps) Icons.Default.LocationOn else Icons.Default.LocationOff,
                    null,
                    tint = if (hasGps) Color(0xFF16A34A) else Color(0xFFDC2626)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasGps) "GPS Vinculado" else "GPS Necessário",
                    fontWeight = FontWeight.Bold,
                    color = if (hasGps) Color(0xFF16A34A) else Color(0xFFDC2626)
                )
            }
            if (hasGps) {
                Text("Lat: $finalLat / Lon: $finalLng", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            AppButton(
                text = if (finalIsCapturing) "Obtendo sinal..." else "Atualizar Localização",
                onClick = {
                    HapticFeedback.tick(context)
                    finalOnUpdate()
                },
                isLoading = finalIsCapturing,
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AppStatusBadge(status: String?, modifier: Modifier = Modifier) {
    val color = when (status?.uppercase()) {
        "BOA" -> Color(0xFF10B981)
        "REGULAR" -> Color(0xFFF59E0B)
        "RUIM" -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = status?.uppercase() ?: "---",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun <T> SpinnerOption(
    label: String,
    options: List<T>,
    selected: T? = null,
    selectedOption: T? = null,
    onSelected: (T?) -> Unit = {},
    onOptionSelected: (T?) -> Unit = {},
    labelProvider: (T) -> String = { it.toString() }
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val finalSelected = selectedOption ?: selected
    val finalOnSelected = if (selectedOption != null || onOptionSelected != {}) onOptionSelected else onSelected

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(modifier = Modifier.padding(top = 8.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { 
                        HapticFeedback.tick(context)
                        expanded = true 
                    },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = finalSelected?.let { labelProvider(it) } ?: "Selecione uma opção",
                        color = if (finalSelected != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(labelProvider(option)) },
                        onClick = {
                            HapticFeedback.tick(context)
                            finalOnSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
