package pl.hexmind.fastnote.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.hexmind.fastnote.database.AppDatabase.Companion.DB_VERSION
import pl.hexmind.fastnote.database.mappers.CommonTypesConverters
import pl.hexmind.fastnote.database.models.DomainEntity
import pl.hexmind.fastnote.database.models.ThoughtEntity
import pl.hexmind.fastnote.database.repositories.DomainDAO
import pl.hexmind.fastnote.database.repositories.ThoughtsDAO

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