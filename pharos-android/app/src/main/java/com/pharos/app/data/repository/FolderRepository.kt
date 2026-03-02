package com.pharos.app.data.repository

import com.pharos.app.data.db.dao.FolderDao
import com.pharos.app.data.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

class FolderRepository(private val folderDao: FolderDao) {

    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFolders()

    suspend fun getAllFoldersList(): List<FolderEntity> = folderDao.getAllFoldersList()

    suspend fun getFolderById(id: String): FolderEntity? = folderDao.getFolderById(id)

    suspend fun insertFolder(folder: FolderEntity) = folderDao.insertFolder(folder)

    suspend fun deleteFolder(id: String) = folderDao.deleteFolder(id)
}
