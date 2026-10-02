package pl.hexmind.mindshaper.services.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * One flashcard of a set - the list order is the order the user entered them in.
 * id == null - not saved yet (a row just added in the edit dialog)
 */
@Parcelize
data class FlashcardDTO(
    val id: Int? = null,
    val front: String,
    val back: String,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val sessionState: FlashcardSessionState? = null,
    val sessionOrder: Int? = null
) : Parcelable

/**
 * Where a flashcard is in the review session of its set.
 * PASSED / FAILED stay after the session ends - the start card shows the last result from them.
 */
enum class FlashcardSessionState {
    QUEUED,
    REVEALED, // Revealed, not answered yet - the session waits for this answer
    PASSED,
    FAILED
}
