package com.pharos.app.ui.screen.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pharos.app.util.JsonParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailScreen(
    fileId: String,
    viewModel: FilesViewModel,
    onBack: () -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    LaunchedEffect(fileId) {
        viewModel.loadFileDetail(fileId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(detailState.file?.name ?: "File") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        if (detailState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                detailState.file?.let { file ->
                    // File info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("File Info", style = MaterialTheme.typography.titleMedium)
                            Text("Name: ${file.name}", style = MaterialTheme.typography.bodyMedium)
                            Text("Type: ${file.mimeType}", style = MaterialTheme.typography.bodyMedium)
                            Text("Size: ${formatSize(file.size)}", style = MaterialTheme.typography.bodyMedium)
                            Text("Status: ${file.status.name}", style = MaterialTheme.typography.bodyMedium)
                            file.lastAnalyzedAt?.let {
                                Text(
                                    "Last analyzed: ${dateFormat.format(Date(it))}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            file.failReason?.let {
                                Text(
                                    "Error: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Analyze button
                    Button(
                        onClick = { viewModel.analyzeSingleFile(file.id) },
                        enabled = !detailState.isAnalyzing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (detailState.isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Analyze This File")
                        }
                    }

                    // Analysis results
                    detailState.analysis?.let { analysis ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Analysis Results", style = MaterialTheme.typography.titleMedium)

                                Text("Summary", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = analysis.summary,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                val topics = JsonParser.fromJsonArray(analysis.topics)
                                if (topics.isNotEmpty()) {
                                    Text("Topics", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = topics.joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                val suggestions = JsonParser.fromJsonArray(analysis.projectSuggestions)
                                if (suggestions.isNotEmpty()) {
                                    Text("Project Suggestions", style = MaterialTheme.typography.titleSmall)
                                    suggestions.forEach { suggestion ->
                                        Text(
                                            text = "- $suggestion",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                val actionItems = JsonParser.fromJsonArray(analysis.actionItems)
                                if (actionItems.isNotEmpty()) {
                                    Text("Action Items", style = MaterialTheme.typography.titleSmall)
                                    actionItems.forEach { item ->
                                        Text(
                                            text = "- $item",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                Text(
                                    text = "Confidence: ${"%.0f".format(analysis.confidence * 100)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Analyzed: ${dateFormat.format(Date(analysis.createdAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Message
                    detailState.message?.let { message ->
                        Text(
                            text = message,
                            color = if (detailState.isError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
    }
}
