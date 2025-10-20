package pl.hexmind.mindshaper.database.initialization

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migrations {


    companion object {

        /**
         * Recreating THOUGHTS table without columns; PRIORITY, ESSENCE
         * ! it is only for backwards compatibility
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                //
                db.execSQL(
                    """
                CREATE TABLE THOUGHTS_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    domain_id INTEGER,
                    thread TEXT,
                    created_at INTEGER NOT NULL,
                    rich_text TEXT,
                    FOREIGN KEY(domain_id) REFERENCES DOMAINS(id) ON DELETE SET NULL
                )
            """
                )

                db.execSQL(
                    """
                INSERT INTO THOUGHTS_new (id, domain_id, thread, created_at, rich_text)
                SELECT id, domain_id, thread, created_at, rich_text 
                FROM THOUGHTS
            """
                )

                db.execSQL("DROP INDEX IF EXISTS index_THOUGHTS_domain_id")

                db.execSQL("DROP TABLE THOUGHTS")

                db.execSQL("ALTER TABLE THOUGHTS_new RENAME TO THOUGHTS")

                db.execSQL("CREATE INDEX index_THOUGHTS_domain_id ON THOUGHTS(domain_id)")
            }
        }

        /**
         * Introducing simplest solution for storing domains icons problem in DB
         *  - ref to res/drawable icon name instead of SVG stored as byte array
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
                    CREATE TABLE ICONS_NEW (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        drawable_name TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("DROP TABLE ICONS")

                db.execSQL("ALTER TABLE ICONS_NEW RENAME TO ICONS")
            }
        }
    }
}