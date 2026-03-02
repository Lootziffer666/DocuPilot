package com.pharos.app.data.db.converter

import androidx.room.TypeConverter
import com.pharos.app.data.db.entity.FileStatus

class Converters {

    @TypeConverter
    fun fromFileStatus(status: FileStatus): String = status.name

    @TypeConverter
    fun toFileStatus(value: String): FileStatus = FileStatus.valueOf(value)
}
