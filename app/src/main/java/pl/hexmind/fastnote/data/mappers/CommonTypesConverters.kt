package pl.hexmind.fastnote.data.mappers

import androidx.room.TypeConverter
import pl.hexmind.fastnote.data.models.DomainIdentifier
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
}