package pl.hexmind.fastnote.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.hexmind.fastnote.data.AppDatabase.Companion.DB_VERSION
import pl.hexmind.fastnote.data.mappers.CommonTypesConverters
import pl.hexmind.fastnote.data.models.DomainEntity
import pl.hexmind.fastnote.data.models.ThoughtEntity
import pl.hexmind.fastnote.data.repositories.DomainDAO
import pl.hexmind.fastnote.data.repositories.ThoughtsDAO

@Database(
    entities = [
        ThoughtEntity::class,
        DomainEntity::class
    ],
    version = DB_VERSION,
    exportSchema = true
)
@TypeConverters(CommonTypesConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun thoughtsDao(): ThoughtsDAO
    abstract fun domainDAO(): DomainDAO

    companion object {
        const val DB_VERSION = 11
    }
}