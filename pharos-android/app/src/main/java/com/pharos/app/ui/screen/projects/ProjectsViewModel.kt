package com.pharos.app.ui.screen.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharos.app.data.db.entity.AnalysisEntity
import com.pharos.app.data.db.entity.FileEntity
import com.pharos.app.data.db.entity.ProjectEntity
import com.pharos.app.data.repository.AnalysisRepository
import com.pharos.app.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectDetailState(
    val project: ProjectEntity? = null,
    val files: List<FileEntity> = emptyList(),
    val analyses: Map<String, AnalysisEntity> = emptyMap(), // fileId -> analysis
    val isLoading: Boolean = false
)

class ProjectsViewModel(
    private val projectRepository: ProjectRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> = projectRepository.getAllProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _projectFileCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val projectFileCounts: StateFlow<Map<String, Int>> = _projectFileCounts.asStateFlow()

    private val _detailState = MutableStateFlow(ProjectDetailState())
    val detailState: StateFlow<ProjectDetailState> = _detailState.asStateFlow()

    fun loadFileCounts(projectIds: List<String>) {
        viewModelScope.launch {
            val counts = mutableMapOf<String, Int>()
            projectIds.forEach { id ->
                counts[id] = projectRepository.getFileCountForProject(id)
            }
            _projectFileCounts.value = counts
        }
    }

    fun loadProjectDetail(projectId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true)

            val project = projectRepository.getProjectById(projectId)
            val files = projectRepository.getFilesForProjectList(projectId)
            val analyses = mutableMapOf<String, AnalysisEntity>()

            files.forEach { file ->
                val analysis = analysisRepository.getLatestAnalysisForFile(file.id)
                if (analysis != null) {
                    analyses[file.id] = analysis
                }
            }

            _detailState.value = ProjectDetailState(
                project = project,
                files = files,
                analyses = analyses,
                isLoading = false
            )
        }
    }
}
