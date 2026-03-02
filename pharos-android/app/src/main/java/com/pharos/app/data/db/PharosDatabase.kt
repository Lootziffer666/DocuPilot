package com.pharos.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pharos.app.data.db.converter.Converters
import com.pharos.app.data.db.dao.AnalysisDao
import com.pharos.app.data.db.dao.FileDao
import com.pharos.app.data.db.dao.FolderDao
import com.pharos.app.data.db.dao.ProjectDao
import com.pharos.app.data.db.dao.ProjectFileCrossRefDao
import com.pharos.app.data.db.entity.AnalysisEntity
import com.pharos.app.data.db.entity.FileEntity
import com.pharos.app.data.db.entity.FolderEntity
import com.pharos.app.data.db.entity.ProjectEntity
import com.pharos.app.data.db.entity.ProjectFileCrossRef

@Database(
    entities = [
        FolderEntity::class,
        FileEntity::class,
        AnalysisEntity::class,
        ProjectEntity::class,
        ProjectFileCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PharosDatabase : RoomDatabase() {

    abstract fun folderDao(): FolderDao
    abstract fun fileDao(): FileDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun projectDao(): ProjectDao
    abstract fun projectFileCrossRefDao(): ProjectFileCrossRefDao

    companion object {
        @Volatile
        private var INSTANCE: PharosDatabase? = null

        fun getInstance(context: Context): PharosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PharosDatabase::class.java,
                    "pharos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
