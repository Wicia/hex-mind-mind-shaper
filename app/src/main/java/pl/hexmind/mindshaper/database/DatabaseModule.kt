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
import pl.hexmind.mindshaper.database.repositories.StepDAO
import pl.hexmind.mindshaper.database.repositories.IconDAO
import pl.hexmind.mindshaper.database.repositories.PathDAO
import pl.hexmind.mindshaper.database.repositories.PathStepDAO
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
            .addMigrations(Migrations.MIGRATION_7_TO_8)
            .addMigrations(Migrations.MIGRATION_8_TO_9)
            .addMigrations(Migrations.MIGRATION_9_TO_10)
            .addMigrations(Migrations.MIGRATION_10_TO_11)
            .addMigrations(Migrations.MIGRATION_11_TO_12)
            .addMigrations(Migrations.MIGRATION_12_TO_13)
            .addMigrations(Migrations.MIGRATION_13_TO_14)
            //.fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideThoughtsDao(db: AppDatabase): ThoughtsDAO  = db.thoughtsDao()
    @Provides fun provideDomainDao(db: AppDatabase): DomainDAO = db.domainDAO()
    @Provides fun provideIconDao(db: AppDatabase): IconDAO = db.iconDAO()
    @Provides fun provideGoalDao(db: AppDatabase): GoalDAO = db.goalDao()
    @Provides fun provideStepDao(db: AppDatabase): StepDAO = db.stepDao()
    @Provides fun providePathDao(db: AppDatabase): PathDAO = db.pathDao()
    @Provides fun providePathStepDao(db: AppDatabase): PathStepDAO = db.pathStepDao()
}