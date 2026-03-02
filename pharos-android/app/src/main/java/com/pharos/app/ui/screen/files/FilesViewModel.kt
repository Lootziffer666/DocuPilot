package com.pharos.app.ui.screen.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharos.app.data.db.entity.AnalysisEntity
import com.pharos.app.data.db.entity.FileEntity
import com.pharos.app.data.repository.AnalysisRepository
import com.pharos.app.data.repository.FileRepository
import com.pharos.app.domain.usecase.AnalysisUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FileDetailState(
    val file: FileEntity? = null,
    val analysis: AnalysisEntity? = null,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class FilesViewModel(
    private val fileRepository: FileRepository,
    private val analysisRepository: AnalysisRepository,
    private val analysisUseCase: AnalysisUseCase
) : ViewModel() {

    val files: StateFlow<List<FileEntity>> = fileRepository.getAllFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _detailState = MutableStateFlow(FileDetailState())
    val detailState: StateFlow<FileDetailState> = _detailState.asStateFlow()

    fun loadFileDetail(fileId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true)

            val file = fileRepository.getFileById(fileId)
            val analysis = if (file != null) {
                analysisRepository.getLatestAnalysisForFile(file.id)
            } else null

            _detailState.value = FileDetailState(
                file = file,
                analysis = analysis,
                isLoading = false
            )
        }
    }

    fun analyzeSingleFile(fileId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isAnalyzing = true)

            try {
                val success = analysisUseCase.analyzeSingleFile(fileId)
                if (success) {
                    loadFileDetail(fileId)
                    _detailState.value = _detailState.value.copy(
                        message = "Analysis complete",
                        isError = false,
                        isAnalyzing = false
                    )
                } else {
                    _detailState.value = _detailState.value.copy(
                        message = "Analysis failed - text could not be extracted",
                        isError = true,
                        isAnalyzing = false
                    )
                }
            } catch (e: Exception) {
                _detailState.value = _detailState.value.copy(
                    message = "Error: ${e.message}",
                    isError = true,
                    isAnalyzing = false
                )
            }
        }
    }

    fun clearMessage() {
        _detailState.value = _detailState.value.copy(message = null)
    }
}
