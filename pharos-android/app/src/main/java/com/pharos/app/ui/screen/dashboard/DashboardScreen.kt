package com.pharos.app.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val totalFiles by viewModel.totalFiles.collectAsState()
    val changedFiles by viewModel.changedFiles.collectAsState()
    val analyzedFiles by viewModel.analyzedFiles.collectAsState()
    val folders by viewModel.folders.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Pharos",
            style = MaterialTheme.typography.headlineMedium
        )

        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Indexed",
                value = totalFiles.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "New/Changed",
                value = changedFiles.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Analyzed",
                value = analyzedFiles.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        // No API Key Warning
        if (!uiState.hasApiKey) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "No API key configured. Go to Settings to add your key before running analysis.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // No folder warning
        if (folders.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    text = "No folder selected. Go to Folders to add a document folder.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // Action Buttons
        val isIdle = uiState.currentAction == DashboardAction.IDLE

        Button(
            onClick = { viewModel.startScan() },
            modifier = Modifier.fillMaxWidth(),
            enabled = isIdle && folders.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp)
            )
            Text("Scan Folder")
        }

        Button(
            onClick = { viewModel.prepareAnalysis() },
            modifier = Modifier.fillMaxWidth(),
            enabled = isIdle && uiState.hasApiKey && totalFiles > 0
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp)
            )
            Text("Start Analysis")
        }

        OutlinedButton(
            onClick = { viewModel.updateMasterfiles() },
            modifier = Modifier.fillMaxWidth(),
            enabled = isIdle && analyzedFiles > 0 && folders.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp)
            )
            Text("Update Masterfiles")
        }

        // Progress Section
        when (uiState.currentAction) {
            DashboardAction.SCANNING -> {
                ProgressCard(
                    title = "Scanning...",
                    progress = uiState.scanProgress?.let {
                        it.current.toFloat() / it.total.coerceAtLeast(1)
                    },
                    detail = uiState.scanProgress?.let {
                        "${it.current}/${it.total} - ${it.currentFileName}"
                    } ?: "Starting scan...",
                    onCancel = viewModel::cancelCurrentAction
                )
            }
            DashboardAction.ANALYZING -> {
                ProgressCard(
                    title = "Analyzing...",
                    progress = uiState.analysisProgress?.let {
                        it.current.toFloat() / it.total.coerceAtLeast(1)
                    },
                    detail = uiState.analysisProgress?.let {
                        "${it.current}/${it.total} - ${it.currentFileName}\n" +
                                "Succeeded: ${it.succeeded} | Failed: ${it.failed}"
                    } ?: "Starting analysis...",
                    onCancel = viewModel::cancelCurrentAction
                )
            }
            DashboardAction.UPDATING_MASTERFILES -> {
                ProgressCard(
                    title = "Updating Masterfiles...",
                    progress = uiState.masterfileProgress?.let {
                        it.current.toFloat() / it.total.coerceAtLeast(1)
                    },
                    detail = uiState.masterfileProgress?.let {
                        "${it.current}/${it.total} - ${it.currentProjectName}"
                    } ?: "Starting update...",
                    onCancel = viewModel::cancelCurrentAction
                )
            }
            DashboardAction.IDLE -> {}
        }

        // Messages
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        uiState.message?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Analysis confirmation dialog
    if (uiState.showAnalysisDialog) {
        AnalysisConfirmDialog(
            preview = uiState.analysisPreview,
            analyzeAll = uiState.analyzeAll,
            onAnalyzeAllChanged = viewModel::toggleAnalyzeAll,
            onConfirm = viewModel::startAnalysis,
            onDismiss = viewModel::dismissAnalysisDialog
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressCard(
    title: String,
    progress: Float?,
    detail: String,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            OutlinedButton(
                onClick = onCancel,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp)
                )
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun AnalysisConfirmDialog(
    preview: com.pharos.app.domain.usecase.AnalysisPreview?,
    analyzeAll: Boolean,
    onAnalyzeAllChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Analysis") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (preview != null) {
                    Text("Files to analyze: ${preview.fileCount}")
                    Text("Estimated tokens: ~${preview.estimatedTokens}")
                    Text(
                        text = "This will use your API key and consume tokens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text("Loading preview...")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = analyzeAll,
                        onCheckedChange = onAnalyzeAllChanged
                    )
                    Text("Re-analyze all files (not just changed)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = preview != null && preview.fileCount > 0
            ) {
                Text("Analyze Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
