package pl.hexmind.fastnote.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.hexmind.fastnote.data.AppDatabase
import pl.hexmind.fastnote.data.repositories.DomainDAO
import pl.hexmind.fastnote.data.repositories.ThoughtsDAO
import javax.inject.Singleton

// Module providing database and DAO instances
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideThoughtsDao(database: AppDatabase): ThoughtsDAO {
        return database.thoughtsDao()
    }

    @Provides
    fun provideDomainDao(database: AppDatabase): DomainDAO {
        return database.domainDAO()
    }
}