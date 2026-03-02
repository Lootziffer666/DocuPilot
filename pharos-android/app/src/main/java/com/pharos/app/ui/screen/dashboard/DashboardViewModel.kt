package com.pharos.app.ui.screen.dashboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharos.app.data.db.entity.FolderEntity
import com.pharos.app.data.repository.FileRepository
import com.pharos.app.data.repository.FolderRepository
import com.pharos.app.data.repository.SettingsRepository
import com.pharos.app.domain.usecase.AnalysisPreview
import com.pharos.app.domain.usecase.AnalysisProgress
import com.pharos.app.domain.usecase.AnalysisResult
import com.pharos.app.domain.usecase.AnalysisUseCase
import com.pharos.app.domain.usecase.ClusteringResult
import com.pharos.app.domain.usecase.MasterfileProgress
import com.pharos.app.domain.usecase.MasterfileResult
import com.pharos.app.domain.usecase.MasterfileUseCase
import com.pharos.app.domain.usecase.ProjectClusteringUseCase
import com.pharos.app.domain.usecase.ScanProgress
import com.pharos.app.domain.usecase.ScanResult
import com.pharos.app.domain.usecase.ScanUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DashboardAction {
    IDLE, SCANNING, ANALYZING, UPDATING_MASTERFILES
}

data class DashboardUiState(
    val currentAction: DashboardAction = DashboardAction.IDLE,
    val scanProgress: ScanProgress? = null,
    val analysisProgress: AnalysisProgress? = null,
    val masterfileProgress: MasterfileProgress? = null,
    val lastScanResult: ScanResult? = null,
    val lastAnalysisResult: AnalysisResult? = null,
    val lastMasterfileResult: MasterfileResult? = null,
    val analysisPreview: AnalysisPreview? = null,
    val showAnalysisDialog: Boolean = false,
    val analyzeAll: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val hasApiKey: Boolean = false
)

class DashboardViewModel(
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository,
    private val scanUseCase: ScanUseCase,
    private val analysisUseCase: AnalysisUseCase,
    private val projectClusteringUseCase: ProjectClusteringUseCase,
    private val masterfileUseCase: MasterfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val totalFiles: StateFlow<Int> = fileRepository.getTotalFileCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val changedFiles: StateFlow<Int> = fileRepository.getChangedFileCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val analyzedFiles: StateFlow<Int> = fileRepository.getAnalyzedFileCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val folders: StateFlow<List<FolderEntity>> = folderRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentJob: Job? = null

    init {
        refreshApiKeyStatus()
    }

    fun refreshApiKeyStatus() {
        _uiState.value = _uiState.value.copy(
            hasApiKey = settingsRepository.getApiKey() != null
        )
    }

    fun startScan() {
        if (_uiState.value.currentAction != DashboardAction.IDLE) return

        currentJob = viewModelScope.launch {
            try {
                val folders = folderRepository.getAllFoldersList()
                if (folders.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        error = "No folder selected. Please add a folder first."
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.SCANNING,
                    error = null
                )

                val folder = folders.first()
                val result = scanUseCase.scanFolder(
                    folderId = folder.id,
                    treeUri = Uri.parse(folder.treeUri)
                ) { progress ->
                    _uiState.value = _uiState.value.copy(scanProgress = progress)
                }

                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.IDLE,
                    scanProgress = null,
                    lastScanResult = result,
                    message = "Scan complete: ${result.newFiles} new, ${result.updatedFiles} updated, ${result.unchangedFiles} unchanged"
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.IDLE,
                    scanProgress = null,
                    message = "Scan cancelled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.IDLE,
                    scanProgress = null,
                    error = "Scan error: ${e.message}"
                )
            }
        }
    }

    fun prepareAnalysis() {
        viewModelScope.launch {
            try {
                val preview = analysisUseCase.getAnalysisPreview(
                    analyzeAll = _uiState.value.analyzeAll
                )
                _uiState.value = _uiState.value.copy(
                    analysisPreview = preview,
                    showAnalysisDialog = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun toggleAnalyzeAll(value: Boolean) {
        _uiState.value = _uiState.value.copy(analyzeAll = value)
    }

    fun dismissAnalysisDialog() {
        _uiState.value = _uiState.value.copy(showAnalysisDialog = false)
    }

    fun startAnalysis() {
        if (_uiState.value.currentAction != DashboardAction.IDLE) return

        _uiState.value = _uiState.value.copy(showAnalysisDialog = false)

        currentJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.ANALYZING,
                    error = null
                )

                val result = analysisUseCase.analyzeFiles(
                    analyzeAll = _uiState.value.analyzeAll
                ) { progress ->
                    _uiState.value = _uiState.value.copy(analysisProgress = progress)
                }

                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.IDLE,
                    analysisProgress = null,
                    lastAnalysisResult = result,
                    message = "Analysis complete: ${result.succeeded} succeeded, ${result.failed} failed"
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.IDLE,
                    analysisProgress = null,
                    message = "Analysis cancelled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.IDLE,
                    analysisProgress = null,
                    error = "Analysis error: ${e.message}"
                )
            }
        }
    }

    fun updateMasterfiles() {
        if (_uiState.value.currentAction != DashboardAction.IDLE) return

        currentJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.UPDATING_MASTERFILES,
                    error = null
                )

                // First cluster projects
                projectClusteringUseCase.clusterProjects()

                // Then write masterfiles
                val result = masterfileUseCase.updateMasterfiles { progress ->
                    _uiState.value = _uiState.value.copy(masterfileProgress = progress)
                }

                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.IDLE,
                    masterfileProgress = null,
                    lastMasterfileResult = result,
                    message = "Masterfiles updated: ${result.filesWritten} written"
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.IDLE,
                    masterfileProgress = null,
                    message = "Update cancelled"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    currentAction = DashboardAction.IDLE,
                    masterfileProgress = null,
                    error = "Masterfile error: ${e.message}"
                )
            }
        }
    }

    fun cancelCurrentAction() {
        currentJob?.cancel()
        currentJob = null
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
