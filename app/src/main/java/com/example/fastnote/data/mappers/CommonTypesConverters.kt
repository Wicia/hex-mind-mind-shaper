package com.example.fastnote.data.mappers

import androidx.room.TypeConverter
import com.example.fastnote.data.models.AreaIdentifier
import java.util.Date

class CommonTypesConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromAreaIdentifier(areaIdentifier: AreaIdentifier): Int {
        return areaIdentifier.value
    }

    @TypeConverter
    fun toAreaIdentifier(areaIdentifier: Int): AreaIdentifier {
        return AreaIdentifier.fromInt(areaIdentifier) // Will throw exception if invalid
        // Or use: return Priority.fromIntOrDefault(value) // Safe with default
    }
}