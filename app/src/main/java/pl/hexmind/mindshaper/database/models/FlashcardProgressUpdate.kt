package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo
import java.time.Instant

/**
 * For Room Partial Update mechanism
 * It allows to store the repetition progress of a flashcard without touching its content
 */
data class FlashcardProgressUpdate(

    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "level")
    val level: Int,

    @ColumnInfo(name = "due_on")
    val dueOn: Long?,

    @ColumnInfo(name = "frozen_until")
    val frozenUntil: Instant?,

    @ColumnInfo(name = "introduced_on")
    val introducedOn: Long?
)
