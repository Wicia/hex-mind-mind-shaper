package pl.hexmind.mindshaper.database.repositories

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import pl.hexmind.mindshaper.database.models.FlashcardEntity
import pl.hexmind.mindshaper.database.models.FlashcardSessionUpdate
import pl.hexmind.mindshaper.database.models.FlashcardSetEntity
import pl.hexmind.mindshaper.database.models.FlashcardSetWithCards

@Dao
interface FlashcardDAO {

// ========== SETS ==========

    // Most recently edited first - same rule as the Stream's default order
    @Transaction
    @Query("SELECT * FROM FLASHCARD_SETS ORDER BY updated_at DESC")
    fun getAllSetsLive(): LiveData<List<FlashcardSetWithCards>>

    @Transaction
    @Query("SELECT * FROM FLASHCARD_SETS WHERE id = :setId")
    fun getSetByIdLive(setId: Int): LiveData<FlashcardSetWithCards?>

    @Insert
    suspend fun insertSet(set: FlashcardSetEntity): Long

    @Query("UPDATE FLASHCARD_SETS SET name = :name, updated_at = :updatedAt WHERE id = :setId")
    suspend fun updateSetName(setId: Int, name: String, updatedAt: Long)

    @Query("UPDATE FLASHCARD_SETS SET updated_at = :updatedAt WHERE id = :setId")
    suspend fun touchSet(setId: Int, updatedAt: Long)

    @Query("DELETE FROM FLASHCARD_SETS WHERE id = :setId")
    suspend fun deleteSet(setId: Int)

// ========== FLASHCARDS ==========

    @Query("DELETE FROM FLASHCARDS WHERE set_id = :setId")
    suspend fun deleteBySetId(setId: Int)

    @Insert
    suspend fun insert(flashcard: FlashcardEntity): Long

    @Insert
    suspend fun insertAll(flashcards: List<FlashcardEntity>)

    @Query("DELETE FROM FLASHCARDS WHERE id = :id")
    suspend fun deleteFlashcard(id: Int)

    // -1 for an empty set - the first flashcard lands at position 0
    @Query("SELECT COALESCE(MAX(position), -1) FROM FLASHCARDS WHERE set_id = :setId")
    suspend fun getLastPosition(setId: Int): Int

    /** One flashcard edited on its own - its place in the set stays */
    @Query("""
        UPDATE FLASHCARDS
        SET front = :front, back = :back,
            correct_count = :correctCount, wrong_count = :wrongCount,
            session_state = :sessionState, session_order = :sessionOrder
        WHERE id = :id
    """)
    suspend fun updateContent(
        id: Int,
        front: String,
        back: String,
        correctCount: Int,
        wrongCount: Int,
        sessionState: String?,
        sessionOrder: Int?
    )

    /** The whole list is edited at once, so it is swapped at once - no per-card diffing. */
    @Transaction
    suspend fun replaceFlashcards(setId: Int, flashcards: List<FlashcardEntity>) {
        deleteBySetId(setId)
        if (flashcards.isNotEmpty()) insertAll(flashcards)
    }

    @Update(entity = FlashcardEntity::class)
    suspend fun updateSession(updates: List<FlashcardSessionUpdate>)

    // ===========================================
    //      Snapshot (backup / restore)
    // ===========================================

    @Query("SELECT * FROM FLASHCARD_SETS")
    suspend fun getAllSets(): List<FlashcardSetEntity>

    @Query("SELECT * FROM FLASHCARDS")
    suspend fun getAllFlashcards(): List<FlashcardEntity>

    @Query("DELETE FROM FLASHCARD_SETS")
    suspend fun clearAllSets()

    @Query("DELETE FROM FLASHCARDS")
    suspend fun clearAllFlashcards()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceSets(sets: List<FlashcardSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceFlashcards(flashcards: List<FlashcardEntity>)
}
