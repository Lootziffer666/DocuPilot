package com.pharos.app.data.repository

import com.pharos.app.data.db.dao.ProjectDao
import com.pharos.app.data.db.dao.ProjectFileCrossRefDao
import com.pharos.app.data.db.entity.FileEntity
import com.pharos.app.data.db.entity.ProjectEntity
import com.pharos.app.data.db.entity.ProjectFileCrossRef
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val crossRefDao: ProjectFileCrossRefDao
) {

    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getAllProjectsList(): List<ProjectEntity> = projectDao.getAllProjectsList()

    suspend fun getProjectById(id: String): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun getProjectByName(name: String): ProjectEntity? = projectDao.getProjectByName(name)

    suspend fun insertProject(project: ProjectEntity) = projectDao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun deleteAllProjects() = projectDao.deleteAllProjects()

    fun getFilesForProject(projectId: String): Flow<List<FileEntity>> =
        crossRefDao.getFilesForProject(projectId)

    suspend fun getFilesForProjectList(projectId: String): List<FileEntity> =
        crossRefDao.getFilesForProjectList(projectId)

    suspend fun getFileCountForProject(projectId: String): Int =
        crossRefDao.getFileCountForProject(projectId)

    suspend fun addFileToProject(projectId: String, fileId: String) =
        crossRefDao.insert(ProjectFileCrossRef(projectId, fileId))

    suspend fun clearProjectFiles(projectId: String) =
        crossRefDao.deleteAllForProject(projectId)

    suspend fun clearAllCrossRefs() = crossRefDao.deleteAll()
}
