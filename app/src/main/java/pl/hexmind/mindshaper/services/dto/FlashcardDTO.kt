package pl.hexmind.mindshaper.services.dto

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pl.hexmind.mindshaper.common.intent.InstantParceler
import java.time.Instant
import java.time.LocalDate

/**
 * One flashcard of a set - the list order is the order the user entered them in.
 * id == null - not saved yet (a row just added in the edit dialog)
 *
 * Repetition progress (see FlashcardsScheduler) - editing the content keeps it.
 */
@Parcelize
@TypeParceler<Instant?, InstantParceler>
data class FlashcardDTO(
    val id: Int? = null,
    val front: String,
    val back: String,
    val status: FlashcardStatus = FlashcardStatus.NEW,
    val level: Int = 0,                 // 1-5 once reviewed, 0 = NEW
    val dueOn: LocalDate? = null,       // ACTIVE: day of the next planned review
    val frozenUntil: Instant? = null,   // FROZEN: second chance not before this moment
    val introducedOn: LocalDate? = null // Day of the first review - counts towards the daily limit of new flashcards
) : Parcelable

enum class FlashcardStatus {
    NEW,      // Never reviewed yet - waits in the pool
    ACTIVE,   // Planned reviews in growing intervals
    FROZEN,   // Failed - waits for the second chance, its level on hold
    MASTERED  // Passed at the top level - not shown in reviews anymore
}

/** Answer after revealing the back */
enum class FlashcardRating {
    GOOD,
    OK,
    BAD
}
