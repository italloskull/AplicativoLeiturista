package com.example.oaplicativo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Reusable and accessible Text Field for forms.
 * Encapsulates error handling and length limiting.
 */
@Composable
fun AppFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    maxLength: Int = 100,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= maxLength) onValueChange(it) },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null,
            enabled = enabled,
            readOnly = readOnly,
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
            trailingIcon = if (error != null) {
                { Icon(Icons.Default.Error, contentDescription = "Erro de validação", tint = MaterialTheme.colorScheme.error) }
            } else null,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            singleLine = true
        )
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * Button that handles loading state internally to prevent multiple clicks.
 */
@Composable
fun LoadingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Container that manages Loading, Empty and Error states for lists.
 */
@Composable
fun <T> AsyncDataContainer(
    items: List<T>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    emptyMessage: String = "Nenhum registro encontrado.",
    content: @Composable (List<T>) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            isLoading && items.isEmpty() -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Erro: $error", color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Tentar Novamente")
                    }
                }
            }
            items.isEmpty() -> {
                Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                content(items)
            }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            OutlinedTextField(
                value = selectedOption?.let { optionToString(it) } ?: "Selecione uma opção",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 250.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("Nenhum") },
                    onClick = {
                        onOptionSelected(null)
                        expanded = false
                    }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(text = optionToString(option)) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BooleanOption(label: String, checked: Boolean?, onCheckedChange: (Boolean?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Checkbox(
                checked = checked == true,
                onCheckedChange = { if (it) onCheckedChange(true) else onCheckedChange(null) }
            )
            Text(text = "Sim", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Checkbox(
                checked = checked == false,
                onCheckedChange = { if (it) onCheckedChange(false) else onCheckedChange(null) }
            )
            Text(text = "Não", style = MaterialTheme.typography.bodySmall)
        }
    }
}