package com.pharos.app.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pharos.app.data.db.entity.AnalysisEntity
import com.pharos.app.data.repository.AnalysisRepository
import com.pharos.app.data.repository.FileRepository
import com.pharos.app.data.repository.FolderRepository
import com.pharos.app.data.repository.ProjectRepository
import com.pharos.app.util.FilenameSanitizer
import com.pharos.app.util.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MasterfileProgress(
    val current: Int,
    val total: Int,
    val currentProjectName: String = ""
)

data class MasterfileResult(
    val filesWritten: Int,
    val errors: Int
)

class MasterfileUseCase(
    private val context: Context,
    private val projectRepository: ProjectRepository,
    private val fileRepository: FileRepository,
    private val analysisRepository: AnalysisRepository,
    private val folderRepository: FolderRepository
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * Generate/update masterfiles for all projects.
     * Writes PHAROS_MASTER_<name>.md into the selected folder.
     * No API calls - purely local operation.
     */
    suspend fun updateMasterfiles(
        onProgress: suspend (MasterfileProgress) -> Unit
    ): MasterfileResult = withContext(Dispatchers.IO) {
        val folders = folderRepository.getAllFoldersList()
        if (folders.isEmpty()) {
            throw IllegalStateException("No folder configured")
        }

        val folder = folders.first()
        val treeUri = Uri.parse(folder.treeUri)
        val documentFolder = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Cannot access folder")

        val projects = projectRepository.getAllProjectsList()
        var filesWritten = 0
        var errors = 0

        projects.forEachIndexed { index, project ->
            ensureActive()

            onProgress(MasterfileProgress(
                current = index + 1,
                total = projects.size,
                currentProjectName = project.name
            ))

            try {
                val files = projectRepository.getFilesForProjectList(project.id)
                val analyses = mutableListOf<Pair<String, AnalysisEntity>>()

                files.forEach { file ->
                    val analysis = analysisRepository.getLatestAnalysisForFile(file.id)
                    if (analysis != null) {
                        analyses.add(file.name to analysis)
                    }
                }

                val content = buildMasterfileContent(project.name, project.description, analyses)
                val sanitizedName = FilenameSanitizer.sanitize(project.name)
                val fileName = "PHAROS_MASTER_$sanitizedName.md"

                writeMasterfile(documentFolder, fileName, content)
                filesWritten++
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                errors++
            }
        }

        MasterfileResult(filesWritten = filesWritten, errors = errors)
    }

    private fun buildMasterfileContent(
        projectName: String,
        description: String,
        analyses: List<Pair<String, AnalysisEntity>>
    ): String {
        val sb = StringBuilder()

        sb.appendLine("# $projectName")
        sb.appendLine()
        sb.appendLine(description)
        sb.appendLine()
        sb.appendLine("**Last updated:** ${dateFormat.format(Date())}")
        sb.appendLine()

        // File list section
        sb.appendLine("## Assigned Files")
        sb.appendLine()

        if (analyses.isEmpty()) {
            sb.appendLine("_No analyzed files assigned to this project._")
        } else {
            analyses.forEach { (fileName, analysis) ->
                sb.appendLine("### $fileName")
                sb.appendLine()

                // Summary bullets
                val summaryLines = analysis.summary
                    .split(". ")
                    .filter { it.isNotBlank() }
                    .take(6)

                summaryLines.forEach { line ->
                    val cleaned = line.trim().removeSuffix(".")
                    if (cleaned.isNotBlank()) {
                        sb.appendLine("- $cleaned")
                    }
                }
                sb.appendLine()

                // Topics
                val topics = JsonParser.fromJsonArray(analysis.topics)
                if (topics.isNotEmpty()) {
                    sb.appendLine("**Topics:** ${topics.joinToString(", ")}")
                    sb.appendLine()
                }
            }
        }

        // Action items section
        val allActionItems = analyses.flatMap { (_, analysis) ->
            JsonParser.fromJsonArray(analysis.actionItems)
        }.distinct()

        if (allActionItems.isNotEmpty()) {
            sb.appendLine("## Open Items")
            sb.appendLine()
            allActionItems.forEach { item ->
                sb.appendLine("- [ ] $item")
            }
            sb.appendLine()
        }

        sb.appendLine("---")
        sb.appendLine("_Generated by Pharos_")

        return sb.toString()
    }

    private fun writeMasterfile(
        folder: DocumentFile,
        fileName: String,
        content: String
    ) {
        // Check if file already exists
        val existing = folder.findFile(fileName)
        if (existing != null) {
            // Overwrite existing file
            context.contentResolver.openOutputStream(existing.uri, "wt")?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Cannot write to $fileName")
        } else {
            // Create new file
            val newFile = folder.createFile("text/markdown", fileName)
                ?: throw IllegalStateException("Cannot create $fileName")
            context.contentResolver.openOutputStream(newFile.uri)?.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("Cannot write to $fileName")
        }
    }
}
