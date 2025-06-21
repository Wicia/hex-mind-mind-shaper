package com.example.fastnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fastnote.data.mappers.CommonTypesConverters
import com.example.fastnote.data.models.ContextEntity
import com.example.fastnote.data.models.ThoughtEntity
import com.example.fastnote.data.repositories.ContextsDAO
import com.example.fastnote.data.repositories.ThoughtsDAO

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