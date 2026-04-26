package com.example.oaplicativo.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable Sync Indicator to show the cloud status of a record.
 */
@Composable
fun SyncIndicator(
    isSynced: Boolean,
    modifier: Modifier = Modifier
) {
    if (isSynced) {
        Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = "Sincronizado com o servidor",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = modifier.size(16.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = "Pendente de sincronização",
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier.size(16.dp)
        )
    }
}