package com.example.oaplicativo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
