package pl.hexmind.mindshaper.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.hexmind.mindshaper.database.AppDatabase.Companion.DB_VERSION
import pl.hexmind.mindshaper.database.mappers.CommonTypesConverters
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.IconEntity
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.database.repositories.DomainDAO
import pl.hexmind.mindshaper.database.repositories.IconDAO
import pl.hexmind.mindshaper.database.repositories.ThoughtsDAO

@Database(
    entities = [
        ThoughtEntity::class,
        DomainEntity::class,
        IconEntity::class
    ],
    version = DB_VERSION,
    exportSchema = true
)
@TypeConverters(CommonTypesConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun thoughtsDao(): ThoughtsDAO
    abstract fun domainDAO(): DomainDAO

    abstract fun iconDAO(): IconDAO

    companion object {
        const val DB_VERSION = 2
    }
}