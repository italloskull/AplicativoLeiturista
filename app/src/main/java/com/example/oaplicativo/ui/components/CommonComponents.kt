package com.example.oaplicativo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BooleanOption(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
                checked = checked,
                onCheckedChange = { if (it) onCheckedChange(true) }
            )
            Text(text = "Sim", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Checkbox(
                checked = !checked,
                onCheckedChange = { if (it) onCheckedChange(false) }
            )
            Text(text = "Não", style = MaterialTheme.typography.bodySmall)
        }
    }
}