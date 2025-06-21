package pl.hexmind.fastnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import pl.hexmind.fastnote.data.mappers.CommonTypesConverters
import pl.hexmind.fastnote.data.models.AreaEntity
import pl.hexmind.fastnote.data.models.AreaIdentifier
import pl.hexmind.fastnote.data.models.ThoughtEntity
import pl.hexmind.fastnote.data.repositories.AreaDAO
import pl.hexmind.fastnote.data.repositories.ThoughtsDAO

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ThoughtEntity::class,
        AreaEntity::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(CommonTypesConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun thoughtsDao(): ThoughtsDAO
    abstract fun areasDAO(): AreaDAO

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
                    .addCallback(DatabaseCallback()) // ! Enabling database initialization
                    .fallbackToDestructiveMigration() // ! remove old db when migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Init on first db creation
            initializeData()
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // This will run every time when db is being open (including after 'fallbackToDestructiveMigration')
            initializeData()
        }

        private fun initializeData() {
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database.areasDAO())
                }
            }
        }
    }
}

suspend fun populateDatabase(areaDAO: AreaDAO) {
    if (areaDAO.getCount() > 0)
        return

    val entities: MutableList<AreaEntity> = mutableListOf()
    val availableAreas = AreaIdentifier.availableAreas()
    for (area in availableAreas) {
        entities.add(AreaEntity(id = area.value, reference = "ui.area.no" + area.value))
    }

    areaDAO.insertAll(entities)
}