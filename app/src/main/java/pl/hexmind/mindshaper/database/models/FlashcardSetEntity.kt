package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A named set of flashcards - its own thing, not a form of a thought.
 */
@Entity(tableName = "FLASHCARD_SETS")
data class FlashcardSetEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Instant.now(),

    // Editing the set (name / flashcards) - reviewing it does not count
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = Instant.now()
)
