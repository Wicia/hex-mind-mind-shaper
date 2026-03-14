package pl.hexmind.mindshaper.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.hexmind.mindshaper.database.initialization.Migrations
import pl.hexmind.mindshaper.database.repositories.DomainDAO
import pl.hexmind.mindshaper.database.repositories.GoalDAO
import pl.hexmind.mindshaper.database.repositories.GuidelineDAO
import pl.hexmind.mindshaper.database.repositories.IconDAO
import pl.hexmind.mindshaper.database.repositories.ThoughtsDAO
import javax.inject.Singleton

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
            .addMigrations(Migrations.MIGRATION_1_TO_2)
            .addMigrations(Migrations.MIGRATION_2_TO_3)
            .addMigrations(Migrations.MIGRATION_3_TO_4)
            .addMigrations(Migrations.MIGRATION_4_TO_5)
            .addMigrations(Migrations.MIGRATION_5_TO_6)
            .addMigrations(Migrations.MIGRATION_6_TO_7)
            //.fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideThoughtsDao(database: AppDatabase): ThoughtsDAO = database.thoughtsDao()

    @Provides
    fun provideDomainDao(database: AppDatabase): DomainDAO = database.domainDAO()

    @Provides
    fun provideIconDao(database: AppDatabase): IconDAO = database.iconDAO()

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDAO = database.goalDao()

    @Provides
    fun provideGuidelineDao(database: AppDatabase): GuidelineDAO = database.guidelineDao()
}