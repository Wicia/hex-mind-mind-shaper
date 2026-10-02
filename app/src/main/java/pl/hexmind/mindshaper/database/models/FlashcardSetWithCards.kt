package pl.hexmind.mindshaper.database.models

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A flashcard set together with its flashcards, fetched in one go.
 */
data class FlashcardSetWithCards(

    @Embedded
    val set: FlashcardSetEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "set_id"
    )
    val flashcards: List<FlashcardEntity>
)
