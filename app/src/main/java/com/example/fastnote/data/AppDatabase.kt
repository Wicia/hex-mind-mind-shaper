package com.example.fastnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.fastnote.data.mappers.CommonMappers
import com.example.fastnote.data.models.ThoughtEntity
import com.example.fastnote.data.repositories.ThoughtsDAO

@Database(entities = [ThoughtEntity::class], version = 2, exportSchema = true)
@TypeConverters(CommonMappers::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun thoughtsDao(): ThoughtsDAO

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