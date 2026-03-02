package com.pharos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pharos.app.data.db.entity.FileEntity
import com.pharos.app.data.db.entity.FileStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    @Query("SELECT * FROM files WHERE folderId = :folderId ORDER BY name ASC")
    fun getFilesByFolder(folderId: String): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY name ASC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files ORDER BY name ASC")
    suspend fun getAllFilesList(): List<FileEntity>

    @Query("SELECT * FROM files WHERE folderId = :folderId ORDER BY name ASC")
    suspend fun getFilesByFolderList(folderId: String): List<FileEntity>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: String): FileEntity?

    @Query("SELECT * FROM files WHERE documentUri = :uri")
    suspend fun getFileByUri(uri: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Update
    suspend fun updateFile(file: FileEntity)

    @Query("UPDATE files SET status = :status WHERE id = :id")
    suspend fun updateFileStatus(id: String, status: FileStatus)

    @Query("UPDATE files SET status = :status, lastAnalyzedAt = :analyzedAt WHERE id = :id")
    suspend fun updateFileAnalyzed(id: String, status: FileStatus, analyzedAt: Long)

    @Query("UPDATE files SET status = :status, failReason = :reason WHERE id = :id")
    suspend fun updateFileFailed(id: String, status: FileStatus, reason: String)

    @Query("SELECT * FROM files WHERE status IN (:statuses)")
    suspend fun getFilesByStatuses(statuses: List<FileStatus>): List<FileEntity>

    @Query("SELECT COUNT(*) FROM files")
    fun getTotalFileCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM files WHERE status IN ('NEVER', 'STALE')")
    fun getChangedFileCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM files WHERE status = 'UP_TO_DATE'")
    fun getAnalyzedFileCount(): Flow<Int>

    @Query("DELETE FROM files WHERE folderId = :folderId AND documentUri NOT IN (:uris)")
    suspend fun deleteFilesNotInUris(folderId: String, uris: List<String>)

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFile(id: String)
}
