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

        // Add PHOTO related columns
        val MIGRATION_3_TO_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("ALTER TABLE THOUGHTS ADD COLUMN photo_data BLOB DEFAULT NULL")
                db.execSQL("ALTER TABLE THOUGHTS ADD COLUMN photo_file_size INTEGER DEFAULT NULL")
            }
        }

        // CREATED AT feature
        val MIGRATION_4_TO_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite DEFAULT can't reference other columns, so we add with 0 and then backfill
                db.execSQL("ALTER TABLE THOUGHTS ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE THOUGHTS SET updated_at = created_at")
            }
        }
    }
}