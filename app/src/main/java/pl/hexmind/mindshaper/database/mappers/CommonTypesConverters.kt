package pl.hexmind.mindshaper.database.mappers

import androidx.room.TypeConverter
import pl.hexmind.mindshaper.activities.capture.models.ThoughtMainContentType
import java.time.Instant
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
    fun fromInstant(value: Instant?): Long? =
        value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? =
        value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromThoughtMainContentType(value: ThoughtMainContentType): String {
        return value.dbCode
    }

    @TypeConverter
    fun toThoughtMainContentType(value: String): ThoughtMainContentType {
        return ThoughtMainContentType.fromDbCode(value)
    }
}