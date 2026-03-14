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

        // Removing unused feature & column (carousel legacy)
        val MIGRATION_5_TO_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            CREATE TABLE THOUGHTS_NEW (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                domain_id INTEGER,
                thread TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                soul_mate TEXT,
                project TEXT,
                value INTEGER NOT NULL DEFAULT 1,
                rich_text TEXT,
                audio_data BLOB,
                audio_duration_ms INTEGER,
                photo_data BLOB,
                photo_file_size INTEGER,
                FOREIGN KEY (domain_id) REFERENCES DOMAINS(id) ON DELETE SET NULL
            )
        """)
                db.execSQL("""
            INSERT INTO THOUGHTS_NEW
            SELECT id, domain_id, thread, created_at, updated_at,
                   soul_mate, project, value, rich_text,
                   audio_data, audio_duration_ms, photo_data, photo_file_size
            FROM THOUGHTS
        """)
                db.execSQL("DROP TABLE THOUGHTS")
                db.execSQL("ALTER TABLE THOUGHTS_NEW RENAME TO THOUGHTS")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_THOUGHTS_domain_id ON THOUGHTS(domain_id)")
            }
        }

        // Workshop: Goals and Guidelines tables
        val MIGRATION_6_TO_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE GOALS (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        description TEXT NOT NULL,
                        priority INTEGER NOT NULL DEFAULT 3,
                        last_modified_at INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE GOAL_GUIDELINES (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goal_id INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        is_done INTEGER NOT NULL DEFAULT 0,
                        position INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (goal_id) REFERENCES GOALS(id) ON DELETE CASCADE
                    )
                """)

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_GOAL_GUIDELINES_goal_id ON GOAL_GUIDELINES(goal_id)"
                )
            }
        }
    }
}
