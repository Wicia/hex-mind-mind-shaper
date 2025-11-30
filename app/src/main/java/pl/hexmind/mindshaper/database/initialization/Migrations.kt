package pl.hexmind.mindshaper.database.initialization

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migrations {

    companion object {
        val MIGRATION_1_TO_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                ALTER TABLE THOUGHTS 
                ADD COLUMN audio_data BLOB DEFAULT NULL
                """.trimIndent()
                )

                db.execSQL(
                    """
                ALTER TABLE THOUGHTS 
                ADD COLUMN audio_duration_ms INTEGER DEFAULT NULL
                """.trimIndent()
                )
            }
        }
    }
}