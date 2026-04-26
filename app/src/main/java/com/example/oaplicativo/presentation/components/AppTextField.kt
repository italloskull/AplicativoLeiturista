package com.example.oaplicativo.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Standardized TextField with integrated error state and accessibility.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    error: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    val isError = error != null

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            leadingIcon = leadingIcon?.let {
                { Icon(imageVector = it, contentDescription = null) }
            },
            isError = isError,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            visualTransformation = if (keyboardOptions.keyboardType == KeyboardType.Password) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            shape = MaterialTheme.shapes.medium,
            supportingText = if (isError && error != null) {
                {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else null
        )
    }
}