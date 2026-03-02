package com.pharos.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FileStatus {
    NEVER,
    UP_TO_DATE,
    STALE,
    FAILED,
    UNSUPPORTED
}

@Entity(
    tableName = "files",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("folderId"), Index("documentUri", unique = true)]
)
data class FileEntity(
    @PrimaryKey
    val id: String, // UUID as string
    val folderId: String,
    val documentUri: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val contentHash: String? = null,
    val mimeType: String,
    val status: FileStatus = FileStatus.NEVER,
    val lastAnalyzedAt: Long? = null,
    val failReason: String? = null
)
