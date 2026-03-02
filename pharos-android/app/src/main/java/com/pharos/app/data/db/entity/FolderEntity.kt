package com.pharos.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String, // UUID as string
    val treeUri: String,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis()
)
