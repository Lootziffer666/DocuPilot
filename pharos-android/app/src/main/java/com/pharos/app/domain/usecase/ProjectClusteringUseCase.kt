package com.pharos.app.domain.usecase

import com.pharos.app.data.db.entity.ProjectEntity
import com.pharos.app.data.repository.AnalysisRepository
import com.pharos.app.data.repository.FileRepository
import com.pharos.app.data.repository.ProjectRepository
import com.pharos.app.util.JsonParser
import java.util.UUID

data class ClusteringResult(
    val projectsCreated: Int,
    val projectsUpdated: Int,
    val filesAssigned: Int
)

class ProjectClusteringUseCase(
    private val analysisRepository: AnalysisRepository,
    private val projectRepository: ProjectRepository,
    private val fileRepository: FileRepository
) {

    /**
     * Run MVP clustering: derive projects from project_suggestions and frequent topics.
     * This is a local operation - no API calls.
     */
    suspend fun clusterProjects(): ClusteringResult {
        val allAnalyses = analysisRepository.getAllAnalyses()
        if (allAnalyses.isEmpty()) {
            return ClusteringResult(0, 0, 0)
        }

        // Count project suggestions across all analyses
        val suggestionCounts = mutableMapOf<String, MutableList<String>>() // suggestion -> [fileIds]
        val topicCounts = mutableMapOf<String, MutableList<String>>() // topic -> [fileIds]

        allAnalyses.forEach { analysis ->
            val suggestions = JsonParser.fromJsonArray(analysis.projectSuggestions)
            val topics = JsonParser.fromJsonArray(analysis.topics)

            suggestions.forEach { suggestion ->
                val normalized = normalizeProjectName(suggestion)
                if (normalized.isNotBlank()) {
                    suggestionCounts.getOrPut(normalized) { mutableListOf() }.add(analysis.fileId)
                }
            }

            topics.forEach { topic ->
                val normalized = normalizeProjectName(topic)
                if (normalized.isNotBlank()) {
                    topicCounts.getOrPut(normalized) { mutableListOf() }.add(analysis.fileId)
                }
            }
        }

        // Select top projects: suggestions that appear 1+ times, or topics that appear 2+ times
        val projectCandidates = mutableMapOf<String, MutableSet<String>>() // name -> fileIds

        suggestionCounts.forEach { (name, fileIds) ->
            projectCandidates.getOrPut(name) { mutableSetOf() }.addAll(fileIds)
        }

        topicCounts.filter { it.value.size >= 2 }.forEach { (name, fileIds) ->
            projectCandidates.getOrPut(name) { mutableSetOf() }.addAll(fileIds)
        }

        // Limit to top N projects by file count
        val topProjects = projectCandidates.entries
            .sortedByDescending { it.value.size }
            .take(MAX_PROJECTS)

        // Clear old cross-refs and update
        projectRepository.clearAllCrossRefs()

        var created = 0
        var updated = 0
        var filesAssigned = 0

        topProjects.forEach { (projectName, fileIds) ->
            val existing = projectRepository.getProjectByName(projectName)
            val projectId: String

            if (existing != null) {
                projectId = existing.id
                projectRepository.updateProject(existing.copy(
                    updatedAt = System.currentTimeMillis()
                ))
                updated++
            } else {
                projectId = UUID.randomUUID().toString()
                projectRepository.insertProject(ProjectEntity(
                    id = projectId,
                    name = projectName,
                    description = buildProjectDescription(projectName, fileIds.size),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                ))
                created++
            }

            projectRepository.clearProjectFiles(projectId)
            fileIds.forEach { fileId ->
                projectRepository.addFileToProject(projectId, fileId)
                filesAssigned++
            }
        }

        return ClusteringResult(
            projectsCreated = created,
            projectsUpdated = updated,
            filesAssigned = filesAssigned
        )
    }

    companion object {
        private const val MAX_PROJECTS = 20

        fun normalizeProjectName(name: String): String {
            return name.trim()
                .lowercase()
                .replaceFirstChar { it.uppercaseChar() }
        }

        private fun buildProjectDescription(name: String, fileCount: Int): String {
            return "Project \"$name\" - derived from document analysis ($fileCount files)"
        }
    }
}
