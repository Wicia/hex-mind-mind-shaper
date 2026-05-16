package pl.hexmind.mindshaper.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.hexmind.mindshaper.database.AppDatabase.Companion.DB_VERSION
import pl.hexmind.mindshaper.database.mappers.CommonTypesConverters
import pl.hexmind.mindshaper.database.models.DomainEntity
import pl.hexmind.mindshaper.database.models.GoalEntity
import pl.hexmind.mindshaper.database.models.GuidelineEntity
import pl.hexmind.mindshaper.database.models.IconEntity
import pl.hexmind.mindshaper.database.models.PathEntity
import pl.hexmind.mindshaper.database.models.PathStepEntity
import pl.hexmind.mindshaper.database.models.ThoughtEntity
import pl.hexmind.mindshaper.database.repositories.DomainDAO
import pl.hexmind.mindshaper.database.repositories.GoalDAO
import pl.hexmind.mindshaper.database.repositories.GuidelineDAO
import pl.hexmind.mindshaper.database.repositories.IconDAO
import pl.hexmind.mindshaper.database.repositories.PathDAO
import pl.hexmind.mindshaper.database.repositories.PathStepDAO
import pl.hexmind.mindshaper.database.repositories.ThoughtsDAO

@Database(
    entities = [
        ThoughtEntity::class,
        DomainEntity::class,
        IconEntity::class,
        GoalEntity::class,
        GuidelineEntity::class,
        PathEntity::class,
        PathStepEntity::class
    ],
    version = DB_VERSION,
    exportSchema = true
)
@TypeConverters(CommonTypesConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun thoughtsDao(): ThoughtsDAO
    abstract fun domainDAO(): DomainDAO
    abstract fun iconDAO(): IconDAO
    abstract fun goalDao(): GoalDAO
    abstract fun guidelineDao(): GuidelineDAO
    abstract fun pathDao(): PathDAO
    abstract fun pathStepDao(): PathStepDAO

    companion object {
        const val DB_VERSION = 10
    }
}