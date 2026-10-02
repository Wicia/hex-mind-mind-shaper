package pl.hexmind.mindshaper.database.models

import androidx.room.ColumnInfo

/**
 * For Room Partial Update mechanism
 * It allows to store the review progress of a flashcard without touching its content
 */
data class FlashcardSessionUpdate(

    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "correct_count")
    val correctCount: Int,

    @ColumnInfo(name = "wrong_count")
    val wrongCount: Int,

    @ColumnInfo(name = "session_state")
    val sessionState: String?,

    @ColumnInfo(name = "session_order")
    val sessionOrder: Int?
)
