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

    // Repetitions System progress - FlashcardStatus name (NEW / ACTIVE / MASTERED)
    @ColumnInfo(name = "status")
    val status: String = STATUS_NEW,

    // 1-5 once reviewed, 0 = never reviewed
    @ColumnInfo(name = "level")
    val level: Int = 0,

    // ACTIVE: day of the next planned review (epoch day)
    @ColumnInfo(name = "due_on")
    val dueOn: Long? = null,

    // Day of the first review (epoch day) - counts towards the daily limit of new flashcards
    @ColumnInfo(name = "introduced_on")
    val introducedOn: Long? = null
) {
    companion object {
        const val STATUS_NEW = "NEW"
        const val STATUS_ACTIVE = "ACTIVE"
    }
}
