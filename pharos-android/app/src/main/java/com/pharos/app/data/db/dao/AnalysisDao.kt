package com.pharos.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pharos.app.data.db.entity.AnalysisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {

    @Query("SELECT * FROM analyses WHERE fileId = :fileId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestAnalysisForFile(fileId: String): AnalysisEntity?

    @Query("SELECT * FROM analyses WHERE fileId = :fileId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestAnalysisForFileFlow(fileId: String): Flow<AnalysisEntity?>

    @Query("SELECT * FROM analyses ORDER BY createdAt DESC")
    suspend fun getAllAnalyses(): List<AnalysisEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AnalysisEntity)

    @Query("DELETE FROM analyses WHERE fileId = :fileId")
    suspend fun deleteAnalysesForFile(fileId: String)
}
