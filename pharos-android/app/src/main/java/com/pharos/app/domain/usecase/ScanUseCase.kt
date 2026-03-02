package com.pharos.app.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pharos.app.data.db.entity.FileEntity
import com.pharos.app.data.db.entity.FileStatus
import com.pharos.app.data.repository.FileRepository
import com.pharos.app.data.repository.FolderRepository
import com.pharos.app.util.PdfTextExtractor
import com.pharos.app.util.TextExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.UUID

data class ScanProgress(
    val current: Int,
    val total: Int,
    val currentFileName: String = "",
    val newFiles: Int = 0,
    val updatedFiles: Int = 0,
    val unchangedFiles: Int = 0,
    val unsupportedFiles: Int = 0
)

data class ScanResult(
    val totalScanned: Int,
    val newFiles: Int,
    val updatedFiles: Int,
    val unchangedFiles: Int,
    val unsupportedFiles: Int,
    val removedFiles: Int
)

class ScanUseCase(
    private val context: Context,
    private val folderRepository: FolderRepository,
    private val fileRepository: FileRepository,
    private val textExtractor: TextExtractor,
    private val pdfTextExtractor: PdfTextExtractor
) {

    private val supportedMimeTypes = setOf(
        "text/plain",
        "text/markdown",
        "text/x-markdown",
        "application/pdf"
    )

    private val supportedExtensions = setOf("txt", "md", "markdown", "pdf")

    suspend fun scanFolder(
        folderId: String,
        treeUri: Uri,
        onProgress: suspend (ScanProgress) -> Unit
    ): ScanResult = withContext(Dispatchers.IO) {
        val documentFile = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalStateException("Cannot access folder")

        val files = collectFiles(documentFile)
        val totalFiles = files.size
        var newFiles = 0
        var updatedFiles = 0
        var unchangedFiles = 0
        var unsupportedFiles = 0
        val seenUris = mutableListOf<String>()

        files.forEachIndexed { index, file ->
            ensureActive()

            val uri = file.uri
            val uriString = uri.toString()
            seenUris.add(uriString)

            val fileName = file.name ?: "unknown"
            onProgress(ScanProgress(
                current = index + 1,
                total = totalFiles,
                currentFileName = fileName,
                newFiles = newFiles,
                updatedFiles = updatedFiles,
                unchangedFiles = unchangedFiles,
                unsupportedFiles = unsupportedFiles
            ))

            val mimeType = file.type ?: guessMimeType(fileName)
            val isSupported = isSupportedFile(mimeType, fileName)

            val existingFile = fileRepository.getFileByUri(uriString)

            if (existingFile != null) {
                // File already in DB - check if changed
                val sizeChanged = existingFile.size != file.length()
                val modifiedChanged = existingFile.lastModified != file.lastModified()

                if (!isSupported) {
                    if (existingFile.status != FileStatus.UNSUPPORTED) {
                        fileRepository.updateFileStatus(existingFile.id, FileStatus.UNSUPPORTED)
                    }
                    unsupportedFiles++
                } else if (sizeChanged || modifiedChanged) {
                    // Check hash for text files
                    val hashChanged = if (isTextBased(mimeType, fileName)) {
                        val newHash = textExtractor.computeHash(uri)
                        newHash != existingFile.contentHash
                    } else {
                        true
                    }

                    if (hashChanged) {
                        val newHash = if (isTextBased(mimeType, fileName)) {
                            textExtractor.computeHash(uri)
                        } else null

                        fileRepository.updateFile(existingFile.copy(
                            size = file.length(),
                            lastModified = file.lastModified(),
                            contentHash = newHash,
                            status = if (existingFile.status == FileStatus.NEVER) FileStatus.NEVER else FileStatus.STALE
                        ))
                        updatedFiles++
                    } else {
                        fileRepository.updateFile(existingFile.copy(
                            size = file.length(),
                            lastModified = file.lastModified()
                        ))
                        unchangedFiles++
                    }
                } else {
                    unchangedFiles++
                }
            } else {
                // New file
                if (!isSupported) {
                    unsupportedFiles++
                } else {
                    val hash = if (isTextBased(mimeType, fileName)) {
                        textExtractor.computeHash(uri)
                    } else null

                    fileRepository.insertFile(FileEntity(
                        id = UUID.randomUUID().toString(),
                        folderId = folderId,
                        documentUri = uriString,
                        name = fileName,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        contentHash = hash,
                        mimeType = mimeType ?: "application/octet-stream",
                        status = FileStatus.NEVER
                    ))
                    newFiles++
                }
            }
        }

        // Remove files that are no longer in the folder
        val removedCount = if (seenUris.isNotEmpty()) {
            val existingFiles = fileRepository.getFilesByFolderList(folderId)
            val toRemove = existingFiles.filter { it.documentUri !in seenUris }
            // Delete in batches to avoid query size limits
            seenUris.chunked(500).forEach { chunk ->
                fileRepository.deleteFilesNotInUris(folderId, chunk)
            }
            if (seenUris.size <= 500) {
                fileRepository.deleteFilesNotInUris(folderId, seenUris)
            }
            toRemove.size
        } else {
            0
        }

        ScanResult(
            totalScanned = totalFiles,
            newFiles = newFiles,
            updatedFiles = updatedFiles,
            unchangedFiles = unchangedFiles,
            unsupportedFiles = unsupportedFiles,
            removedFiles = removedCount
        )
    }

    private fun collectFiles(directory: DocumentFile): List<DocumentFile> {
        val result = mutableListOf<DocumentFile>()
        val queue = ArrayDeque<DocumentFile>()
        queue.add(directory)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            current.listFiles().forEach { file ->
                if (file.isDirectory) {
                    queue.add(file)
                } else if (file.isFile) {
                    result.add(file)
                }
            }
        }
        return result
    }

    private fun isSupportedFile(mimeType: String?, fileName: String): Boolean {
        if (mimeType != null && mimeType in supportedMimeTypes) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in supportedExtensions
    }

    private fun isTextBased(mimeType: String?, fileName: String): Boolean {
        if (mimeType != null && mimeType.startsWith("text/")) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in setOf("txt", "md", "markdown")
    }

    private fun guessMimeType(fileName: String): String? {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "txt" -> "text/plain"
            "md", "markdown" -> "text/markdown"
            "pdf" -> "application/pdf"
            else -> null
        }
    }
}
