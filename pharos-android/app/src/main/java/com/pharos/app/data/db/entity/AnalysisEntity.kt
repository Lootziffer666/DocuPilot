package com.pharos.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analyses",
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fileId")]
)
data class AnalysisEntity(
    @PrimaryKey
    val id: String, // UUID as string
    val fileId: String,
    val summary: String,
    val topics: String, // JSON array string
    val projectSuggestions: String, // JSON array string
    val actionItems: String, // JSON array string
    val confidence: Double,
    val rawResponse: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
