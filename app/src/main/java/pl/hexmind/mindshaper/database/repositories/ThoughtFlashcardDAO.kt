package pl.hexmind.mindshaper.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import pl.hexmind.mindshaper.database.models.ThoughtFlashcardEntity

@Dao
interface ThoughtFlashcardDAO {

    @Query("DELETE FROM THOUGHT_FLASHCARDS WHERE thought_id = :thoughtId")
    suspend fun deleteByThoughtId(thoughtId: Int)

    @Insert
    suspend fun insertAll(flashcards: List<ThoughtFlashcardEntity>)

    /** The whole list is edited at once, so it is swapped at once - no per-card diffing. */
    @Transaction
    suspend fun replaceFlashcards(thoughtId: Int, flashcards: List<ThoughtFlashcardEntity>) {
        deleteByThoughtId(thoughtId)
        if (flashcards.isNotEmpty()) insertAll(flashcards)
    }

    // ===========================================
    //      Snapshot (backup / restore)
    // ===========================================

    @Query("SELECT * FROM THOUGHT_FLASHCARDS")
    suspend fun getAllFlashcards(): List<ThoughtFlashcardEntity>

    @Query("DELETE FROM THOUGHT_FLASHCARDS")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(flashcards: List<ThoughtFlashcardEntity>)
}
