package com.example.oaplicativo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 250.dp) // Limita a altura para aproximadamente 5 itens
            ) {
                // Opção nula/limpar
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