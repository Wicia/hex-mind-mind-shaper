package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One flashcard (front + back) of a thought. A thought holds an ordered list of them.
 */
@Entity(
    tableName = "THOUGHT_FLASHCARDS",
    foreignKeys = [
        ForeignKey(
            entity = ThoughtEntity::class,
            parentColumns = ["id"],
            childColumns = ["thought_id"],
            onDelete = ForeignKey.CASCADE // Flashcards live and die with their thought
        )
    ],
    indices = [Index(value = ["thought_id"])]
)
data class ThoughtFlashcardEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    @ColumnInfo(name = "thought_id")
    val thoughtId: Int,

    // Order in which the user entered the flashcards
    @ColumnInfo(name = "position")
    val position: Int,

    @ColumnInfo(name = "front")
    val front: String,

    @ColumnInfo(name = "back")
    val back: String
)
