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

        val MIGRATION_2_TO_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE thoughts ADD COLUMN main_content_type TEXT NOT NULL DEFAULT 'U'"
                )

                db.execSQL("""
                    UPDATE THOUGHTS 
                    SET main_content_type = CASE
                        WHEN audio_data IS NOT NULL THEN 'R'
                        WHEN rich_text IS NOT NULL AND rich_text != '' THEN 'T'
                        ELSE 'U'
                    END
                """)
            }
        }
    }
}