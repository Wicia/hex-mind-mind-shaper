package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One flashcard (front + back) of a set. A set holds an ordered list of them.
 */
@Entity(
    tableName = "FLASHCARDS",
    foreignKeys = [
        ForeignKey(
            entity = FlashcardSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["set_id"],
            onDelete = ForeignKey.CASCADE // Flashcards live and die with their set
        )
    ],
    indices = [Index(value = ["set_id"])]
)
data class FlashcardEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "set_id")
    val setId: Int,

    // Order in which the user entered the flashcards
    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "front")
    val front: String,

    @ColumnInfo(name = "back")
    val back: String,

    // Answers given over all review sessions (thumb up / thumb down)
    @ColumnInfo(name = "correct_count", defaultValue = "0")
    val correctCount: Int = 0,

    @ColumnInfo(name = "wrong_count", defaultValue = "0")
    val wrongCount: Int = 0,

    // Review session: FlashcardSessionState name - null = not part of any session yet
    @ColumnInfo(name = "session_state")
    val sessionState: String? = null,

    // Review session: place in the queue - skipping a flashcard moves it to the end
    @ColumnInfo(name = "session_order")
    val sessionOrder: Int? = null
)
