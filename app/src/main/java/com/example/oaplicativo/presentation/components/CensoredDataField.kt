package com.example.oaplicativo.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Advanced Input Field that encapsulates censorship logic and re-edit actions.
 */
@Composable
fun CensoredDataField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isCensored: Boolean,
    censoredValue: String,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = if (isCensored) censoredValue else value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = !isCensored,
        leadingIcon = leadingIcon?.let {
            { Icon(imageVector = it, contentDescription = null) }
        },
        trailingIcon = if (isCensored) {
            {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Desbloquear para edição"
                    )
                }
            }
        } else null,
        keyboardOptions = keyboardOptions,
        singleLine = true
    )
}