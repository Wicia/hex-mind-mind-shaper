package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import java.time.LocalDate

/**
 * Repetitions System - fixed scale of intervals instead of an algorithm (see "Requirements - Flashcards & Repetitions" doc).
 * Pure rules: the caller passes "today" and stores the result.
 *
 * - levels 1-5, next review = day of the actual review + interval of the level the flashcard landed on
 * - GOOD = level +1 (at the top level = mastered), OK = level stays, BAD = level -1 (not below 1) and review tomorrow
 * - new flashcard: any rating = level 1, review tomorrow
 * Every rating means the same at every level. Being late lowers nothing.
 * ! Returns in a session after BAD are not rated here - only the first rating of the day counts (HexFlashcardView)
 */
object FlashcardsScheduler {

    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 5

    // Index = level - 1
    private val INTERVAL_DAYS = listOf(1L, 3L, 7L, 14L, 30L)

    fun intervalDays(level: Int): Long = INTERVAL_DAYS[level.coerceIn(MIN_LEVEL, MAX_LEVEL) - 1]

    /** Planned review due - NEW ones wait for the daily limit (FlashcardsReview) */
    fun isRepetitionDue(flashcard: FlashcardDTO, today: LocalDate): Boolean =
        when (flashcard.status) {
            FlashcardStatus.ACTIVE   -> flashcard.dueOn?.let { dueOn -> !dueOn.isAfter(today) } ?: true
            FlashcardStatus.NEW,
            FlashcardStatus.MASTERED -> false
        }

    fun rate(flashcard: FlashcardDTO, rating: FlashcardRating, today: LocalDate): FlashcardDTO =
        when (flashcard.status) {
            FlashcardStatus.NEW      -> flashcard.copy(introducedOn = today).planned(MIN_LEVEL, today.plusDays(1))
            FlashcardStatus.ACTIVE   -> ratePlanned(flashcard, rating, today)
            FlashcardStatus.MASTERED -> flashcard
        }

    private fun ratePlanned(flashcard: FlashcardDTO, rating: FlashcardRating, today: LocalDate): FlashcardDTO {
        val level = flashcard.level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        return when (rating) {
            FlashcardRating.GOOD ->
                if (level == MAX_LEVEL) flashcard.copy(status = FlashcardStatus.MASTERED, level = MAX_LEVEL, dueOn = null)
                else                    flashcard.planned(level + 1, today.plusDays(intervalDays(level + 1)))
            FlashcardRating.OK   -> flashcard.planned(level, today.plusDays(intervalDays(level)))
            // Always tomorrow - the interval of the lower level counts from the next review
            FlashcardRating.BAD  -> flashcard.planned((level - 1).coerceAtLeast(MIN_LEVEL), today.plusDays(1))
        }
    }

    private fun FlashcardDTO.planned(level: Int, dueOn: LocalDate): FlashcardDTO =
        copy(
            status = FlashcardStatus.ACTIVE,
            level  = level,
            dueOn  = dueOn
        )
}
