package com.pharos.app.ui.screen.folder

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharos.app.data.db.entity.FolderEntity
import com.pharos.app.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class FolderUiState(
    val message: String? = null,
    val isError: Boolean = false
)

class FolderViewModel(
    private val folderRepository: FolderRepository
) : ViewModel() {

    val folders: StateFlow<List<FolderEntity>> = folderRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    fun addFolder(uri: Uri, displayName: String) {
        viewModelScope.launch {
            try {
                folderRepository.insertFolder(FolderEntity(
                    id = UUID.randomUUID().toString(),
                    treeUri = uri.toString(),
                    displayName = displayName,
                    createdAt = System.currentTimeMillis()
                ))
                _uiState.value = _uiState.value.copy(
                    message = "Folder added: $displayName",
                    isError = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Error adding folder: ${e.message}",
                    isError = true
                )
            }
        }
    }

    fun removeFolder(folderId: String) {
        viewModelScope.launch {
            try {
                folderRepository.deleteFolder(folderId)
                _uiState.value = _uiState.value.copy(
                    message = "Folder removed",
                    isError = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = "Error removing folder: ${e.message}",
                    isError = true
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
