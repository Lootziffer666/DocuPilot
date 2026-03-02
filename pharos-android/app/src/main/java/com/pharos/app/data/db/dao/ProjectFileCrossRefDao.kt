package com.pharos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pharos.app.data.db.entity.FileEntity
import com.pharos.app.data.db.entity.ProjectFileCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectFileCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: ProjectFileCrossRef)

    @Query("SELECT f.* FROM files f INNER JOIN project_file_cross_ref pf ON f.id = pf.fileId WHERE pf.projectId = :projectId ORDER BY f.name ASC")
    fun getFilesForProject(projectId: String): Flow<List<FileEntity>>

    @Query("SELECT f.* FROM files f INNER JOIN project_file_cross_ref pf ON f.id = pf.fileId WHERE pf.projectId = :projectId ORDER BY f.name ASC")
    suspend fun getFilesForProjectList(projectId: String): List<FileEntity>

    @Query("SELECT COUNT(*) FROM project_file_cross_ref WHERE projectId = :projectId")
    suspend fun getFileCountForProject(projectId: String): Int

    @Query("DELETE FROM project_file_cross_ref WHERE projectId = :projectId")
    suspend fun deleteAllForProject(projectId: String)

    @Query("DELETE FROM project_file_cross_ref")
    suspend fun deleteAll()
}
