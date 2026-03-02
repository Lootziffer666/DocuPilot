package com.pharos.app.ui.screen.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pharos.app.data.db.entity.FileEntity
import com.pharos.app.data.db.entity.FileStatus

@Composable
fun FilesScreen(
    viewModel: FilesViewModel,
    onFileClick: (String) -> Unit
) {
    val files by viewModel.files.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Files",
            style = MaterialTheme.typography.headlineMedium
        )

        if (files.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No files indexed",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Scan a folder from the Dashboard to index files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(files) { file ->
                    FileItem(
                        file = file,
                        onClick = { onFileClick(file.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileItem(
    file: FileEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusIcon(
                status = file.status,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${file.mimeType} | ${formatSize(file.size)} | ${file.status.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatusIcon(status: FileStatus, modifier: Modifier = Modifier) {
    when (status) {
        FileStatus.NEVER -> Icon(
            Icons.Default.FiberNew, "Never analyzed",
            tint = MaterialTheme.colorScheme.tertiary, modifier = modifier
        )
        FileStatus.UP_TO_DATE -> Icon(
            Icons.Default.CheckCircle, "Up to date",
            tint = Color(0xFF4CAF50), modifier = modifier
        )
        FileStatus.STALE -> Icon(
            Icons.Default.Update, "Stale",
            tint = Color(0xFFFF9800), modifier = modifier
        )
        FileStatus.FAILED -> Icon(
            Icons.Default.Error, "Failed",
            tint = MaterialTheme.colorScheme.error, modifier = modifier
        )
        FileStatus.UNSUPPORTED -> Icon(
            Icons.Default.HelpOutline, "Unsupported",
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier
        )
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    }
}
