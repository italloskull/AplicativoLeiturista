package com.example.oaplicativo.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.delay

/**
 * Advanced Input Field that encapsulates censorship logic, role-based access, and timed reveals.
 */
@Composable
fun CensoredDataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isCensoredInitial: Boolean,
    censoredValue: String,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    revealDurationMs: Long = 10000L
) {
    var isRevealedByAdmin by remember { mutableStateOf(false) }
    var isUnlockedForEdit by remember { mutableStateOf(false) }
    
    // Timer para esconder novamente (apenas para Admins que revelaram o dado)
    if (isRevealedByAdmin && isCensoredInitial && isAdmin) {
        LaunchedEffect(Unit) {
            delay(revealDurationMs)
            isRevealedByAdmin = false
        }
    }

    // Lógica de exibição:
    // 1. Se for Admin e revelou: mostra original e habilita
    // 2. Se NÃO for Admin e desbloqueou para editar: mostra original (vazio ou novo valor) e habilita
    // 3. Caso contrário: mostra censurado e desabilita
    val isEffectivelyEnabled = if (isCensoredInitial) {
        if (isAdmin) isRevealedByAdmin else isUnlockedForEdit
    } else true

    // SÊNIOR BUFFER: Mantemos um estado local para a edição em andamento.
    // Isso evita que o dado original no ViewModel seja apagado por acidente.
    var hasStartedTyping by remember { mutableStateOf(false) }

    // Reseta o estado local ao fechar o cadeado ou mudar o modo
    LaunchedEffect(isUnlockedForEdit) {
        if (!isUnlockedForEdit) {
            hasStartedTyping = false
        }
    }

    val displayValue = if (isCensoredInitial && !isEffectivelyEnabled) {
        censoredValue
    } else if (isUnlockedForEdit && !isAdmin && !hasStartedTyping) {
        // Se acabou de clicar no lápis e não é admin, mostra vazio (proteção visual)
        // mas o 'value' original continua salvo no ViewModel.
        ""
    } else {
        value
    }

    OutlinedTextField(
        value = displayValue,
        onValueChange = {
            if (isEffectivelyEnabled) {
                if (isUnlockedForEdit && !isAdmin) {
                    hasStartedTyping = true
                }
                onValueChange(it)
            }
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = isEffectivelyEnabled,
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null) }
        },
        trailingIcon = if (isCensoredInitial) {
            {
                IconButton(onClick = {
                    if (isAdmin) {
                        isRevealedByAdmin = !isRevealedByAdmin
                    } else {
                        // SÊNIOR FIX: Não limpamos o ViewModel imediatamente ao clicar.
                        // Apenas entramos no modo de edição. O dado original só será
                        // sobrescrito se o usuário de fato digitar algo novo.
                        isUnlockedForEdit = !isUnlockedForEdit
                    }
                }) {
                    val icon = when {
                        isAdmin && isRevealedByAdmin -> Icons.Default.Visibility
                        isAdmin -> Icons.Default.VisibilityOff
                        isUnlockedForEdit -> Icons.Default.Edit
                        else -> Icons.Default.Edit // Ícone de edição para usuários comuns
                    }
                    val description = if (isAdmin) "Revelar/Esconder" else "Habilitar Edição"
                    Icon(imageVector = icon, contentDescription = description)
                }
            }
        } else null,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (isCensoredInitial && !isEffectivelyEnabled) VisualTransformation.None else visualTransformation,
        singleLine = true
    )
}