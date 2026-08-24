package com.example.oaplicativo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Container that manages Loading, Empty and Error states for lists with Pull-to-Refresh support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AsyncDataContainer(
    items: List<T>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    emptyMessage: String = "Nenhum registro encontrado.",
    content: @Composable (List<T>) -> Unit
) {
    if (onRefresh != null) {
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncContent(items, isLoading, error, onRetry, emptyMessage, content)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncContent(items, isLoading, error, onRetry, emptyMessage, content)
        }
    }
}

@Composable
private fun <T> AsyncContent(
    items: List<T>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    emptyMessage: String,
    content: @Composable (List<T>) -> Unit
) {
    when {
        isLoading && items.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Erro: $error", color = MaterialTheme.colorScheme.error)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Tentar Novamente")
                    }
                }
            }
        }
        items.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
        else -> {
            content(items)
        }
    }
}
