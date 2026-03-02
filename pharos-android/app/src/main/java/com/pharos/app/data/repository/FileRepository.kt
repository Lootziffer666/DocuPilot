package com.pharos.app.data.repository

import com.pharos.app.data.db.dao.FileDao
import com.pharos.app.data.db.entity.FileEntity
import com.pharos.app.data.db.entity.FileStatus
import kotlinx.coroutines.flow.Flow

class FileRepository(private val fileDao: FileDao) {

    fun getFilesByFolder(folderId: String): Flow<List<FileEntity>> =
        fileDao.getFilesByFolder(folderId)

    fun getAllFiles(): Flow<List<FileEntity>> = fileDao.getAllFiles()

    suspend fun getAllFilesList(): List<FileEntity> = fileDao.getAllFilesList()

    suspend fun getFilesByFolderList(folderId: String): List<FileEntity> =
        fileDao.getFilesByFolderList(folderId)

    suspend fun getFileById(id: String): FileEntity? = fileDao.getFileById(id)

    suspend fun getFileByUri(uri: String): FileEntity? = fileDao.getFileByUri(uri)

    suspend fun insertFile(file: FileEntity) = fileDao.insertFile(file)

    suspend fun updateFile(file: FileEntity) = fileDao.updateFile(file)

    suspend fun updateFileStatus(id: String, status: FileStatus) =
        fileDao.updateFileStatus(id, status)

    suspend fun updateFileAnalyzed(id: String, status: FileStatus, analyzedAt: Long) =
        fileDao.updateFileAnalyzed(id, status, analyzedAt)

    suspend fun updateFileFailed(id: String, status: FileStatus, reason: String) =
        fileDao.updateFileFailed(id, status, reason)

    suspend fun getFilesByStatuses(statuses: List<FileStatus>): List<FileEntity> =
        fileDao.getFilesByStatuses(statuses)

    fun getTotalFileCount(): Flow<Int> = fileDao.getTotalFileCount()

    fun getChangedFileCount(): Flow<Int> = fileDao.getChangedFileCount()

    fun getAnalyzedFileCount(): Flow<Int> = fileDao.getAnalyzedFileCount()

    suspend fun deleteFilesNotInUris(folderId: String, uris: List<String>) =
        fileDao.deleteFilesNotInUris(folderId, uris)
}
