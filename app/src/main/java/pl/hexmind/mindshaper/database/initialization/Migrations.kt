package pl.hexmind.mindshaper.database.initialization

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migrations {

    companion object {
        val MIGRATION_1_TO_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE THOUGHTS ADD COLUMN audio_data BLOB DEFAULT NULL")
                db.execSQL("ALTER TABLE THOUGHTS ADD COLUMN audio_duration_ms INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_TO_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE thoughts ADD COLUMN main_content_type TEXT NOT NULL DEFAULT 'U'")
                db.execSQL(
                    """
                    UPDATE THOUGHTS 
                    SET main_content_type = CASE
                        WHEN audio_data IS NOT NULL THEN 'R'
                        WHEN rich_text IS NOT NULL AND rich_text != '' THEN 'T'
                        ELSE 'U'
                    END
                """
                )
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
                db.execSQL(
                    """
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
                """
                )
                db.execSQL(
                    """
                    INSERT INTO THOUGHTS_NEW
                    SELECT id, domain_id, thread, created_at, updated_at,
                           soul_mate, project, value, rich_text,
                           audio_data, audio_duration_ms, photo_data, photo_file_size
                    FROM THOUGHTS
                """
                )
                db.execSQL("DROP TABLE THOUGHTS")
                db.execSQL("ALTER TABLE THOUGHTS_NEW RENAME TO THOUGHTS")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_THOUGHTS_domain_id ON THOUGHTS(domain_id)")
            }
        }

        // Workshop: Goals and Steps tables (originally "Guidelines", renamed in migration 12→13)
        val MIGRATION_6_TO_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE GOALS (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        description TEXT NOT NULL,
                        priority INTEGER NOT NULL DEFAULT 3,
                        last_modified_at INTEGER NOT NULL
                    )
                """
                )
                db.execSQL(
                    """
                    CREATE TABLE GOAL_GUIDELINES (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goal_id INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        is_done INTEGER NOT NULL DEFAULT 0,
                        position INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (goal_id) REFERENCES GOALS(id) ON DELETE CASCADE
                    )
                """
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_GOAL_GUIDELINES_goal_id ON GOAL_GUIDELINES(goal_id)")
            }
        }

        // Workshop: Paths and Steps
        val MIGRATION_7_TO_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
            CREATE TABLE PATHS (
                path_key           TEXT PRIMARY KEY NOT NULL,
                category           TEXT NOT NULL,
                status             TEXT NOT NULL DEFAULT 'UNSELECTED',
                current_step_index INTEGER NOT NULL DEFAULT 0,
                last_drawn_date    INTEGER DEFAULT NULL
            )
        """)

                db.execSQL("""
            CREATE TABLE PATH_STEPS (
                id       INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                path_key TEXT NOT NULL,
                position INTEGER NOT NULL,
                content  TEXT NOT NULL,
                FOREIGN KEY (path_key) REFERENCES PATHS(path_key) ON DELETE CASCADE
            )
        """)

                db.execSQL("CREATE INDEX IF NOT EXISTS index_PATH_STEPS_path_key ON PATH_STEPS(path_key)")
            }
        }
        // Rename `priority` → `importance` in GOALS + reverse scale (old 1=highest → new 3=highest)
        // Remapping: importance = 4 - priority  (1→3, 2→2, 3→1)
        val MIGRATION_8_TO_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE GOALS_NEW (
                        id               INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        description      TEXT NOT NULL,
                        importance       INTEGER NOT NULL DEFAULT 1,
                        last_modified_at INTEGER NOT NULL
                    )
                """
                )
                // Remap: old priority 1 (highest) → importance 3
                //        old priority 3 (lowest)  → importance 1
                db.execSQL(
                    """
                    INSERT INTO GOALS_NEW (id, description, importance, last_modified_at)
                    SELECT id, description, (4 - priority), last_modified_at
                    FROM GOALS
                """
                )
                db.execSQL("DROP TABLE GOALS")
                db.execSQL("ALTER TABLE GOALS_NEW RENAME TO GOALS")
            }
        }

        // Replace is_done (checkbox/single state) with current_repetitions + max_repetitions
        val MIGRATION_9_TO_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE GOAL_GUIDELINES_NEW (
                        id                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goal_id             INTEGER NOT NULL,
                        description         TEXT NOT NULL,
                        position            INTEGER NOT NULL DEFAULT 0,
                        current_repetitions INTEGER NOT NULL DEFAULT 0,
                        max_repetitions     INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY (goal_id) REFERENCES GOALS(id) ON DELETE CASCADE
                    )
                """
                )
                db.execSQL(
                    """
                    INSERT INTO GOAL_GUIDELINES_NEW (id, goal_id, description, position, current_repetitions, max_repetitions)
                    SELECT id, goal_id, description, position, is_done, 1
                    FROM GOAL_GUIDELINES
                """
                )
                db.execSQL("DROP TABLE GOAL_GUIDELINES")
                db.execSQL("ALTER TABLE GOAL_GUIDELINES_NEW RENAME TO GOAL_GUIDELINES")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_GOAL_GUIDELINES_goal_id ON GOAL_GUIDELINES(goal_id)")
            }
        }

        // Add thought_id to GOAL_GUIDELINES (FK to THOUGHTS, SET NULL on delete).
        // SQLite ALTER TABLE can't add FK + index in one go, so we recreate the table.
        val MIGRATION_10_TO_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE GOAL_GUIDELINES_NEW (
                        id                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goal_id             INTEGER NOT NULL,
                        description         TEXT NOT NULL,
                        position            INTEGER NOT NULL DEFAULT 0,
                        current_repetitions INTEGER NOT NULL DEFAULT 0,
                        max_repetitions     INTEGER NOT NULL DEFAULT 1,
                        thought_id          INTEGER DEFAULT NULL,
                        FOREIGN KEY (goal_id)    REFERENCES GOALS(id)    ON DELETE CASCADE,
                        FOREIGN KEY (thought_id) REFERENCES THOUGHTS(id) ON DELETE SET NULL
                    )
                """
                )
                db.execSQL(
                    """
                    INSERT INTO GOAL_GUIDELINES_NEW (id, goal_id, description, position, current_repetitions, max_repetitions, thought_id)
                    SELECT id, goal_id, description, position, current_repetitions, max_repetitions, NULL
                    FROM GOAL_GUIDELINES
                """
                )
                db.execSQL("DROP TABLE GOAL_GUIDELINES")
                db.execSQL("ALTER TABLE GOAL_GUIDELINES_NEW RENAME TO GOAL_GUIDELINES")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_GOAL_GUIDELINES_goal_id ON GOAL_GUIDELINES(goal_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_GOAL_GUIDELINES_thought_id ON GOAL_GUIDELINES(thought_id)")
            }
        }

        // Rename column: thread -> subject
        val MIGRATION_11_TO_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE THOUGHTS_NEW (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        domain_id INTEGER,
                        subject TEXT,
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
                """
                )
                db.execSQL(
                    """
                    INSERT INTO THOUGHTS_NEW
                    SELECT id, domain_id, thread, created_at, updated_at,
                           soul_mate, project, value, rich_text,
                           audio_data, audio_duration_ms,
                           photo_data, photo_file_size
                    FROM THOUGHTS
                """
                )
                db.execSQL("DROP TABLE THOUGHTS")
                db.execSQL("ALTER TABLE THOUGHTS_NEW RENAME TO THOUGHTS")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_THOUGHTS_domain_id ON THOUGHTS(domain_id)")
            }
        }
        // Rename table: GOAL_GUIDELINES -> GOAL_STEPS
        val MIGRATION_12_TO_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE GOAL_GUIDELINES RENAME TO GOAL_STEPS")
                db.execSQL("DROP INDEX IF EXISTS index_GOAL_GUIDELINES_goal_id")
                db.execSQL("DROP INDEX IF EXISTS index_GOAL_GUIDELINES_thought_id")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_GOAL_STEPS_goal_id ON GOAL_STEPS(goal_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_GOAL_STEPS_thought_id ON GOAL_STEPS(thought_id)")
            }
        }

    }
}
