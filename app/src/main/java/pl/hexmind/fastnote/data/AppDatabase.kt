package pl.hexmind.fastnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import pl.hexmind.fastnote.data.mappers.CommonTypesConverters
import pl.hexmind.fastnote.data.models.ContextEntity
import pl.hexmind.fastnote.data.models.ThoughtEntity
import pl.hexmind.fastnote.data.repositories.ContextsDAO
import pl.hexmind.fastnote.data.repositories.ThoughtsDAO

@Database(
    entities = [ThoughtEntity::class, ContextEntity::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(CommonTypesConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun thoughtsDao(): ThoughtsDAO
    abstract fun contextsDAO(): ContextsDAO

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration() // remove old db when migration
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}