package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Repetitions System - fixed scale of intervals instead of an algorithm (see "Requirements - Flashcards & Repetitions" doc).
 * Pure rules: the caller passes "today" / "now" and stores the result.
 *
 * - levels 1-5, next review = day of the actual review + interval of the level
 * - planned review:  GOOD = level +1 (at the top level = mastered), OK = level stays, BAD = frozen
 * - second chance:   GOOD = level stays, OK = level -1, BAD = level -1 and frozen again
 * - new flashcard:   GOOD / OK = level 1, BAD = level 1 and frozen
 * The only way up is GOOD at a planned review. Being late lowers nothing.
 */
object FlashcardsScheduler {

    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 5

    // Index = level - 1
    private val INTERVAL_DAYS = listOf(1L, 3L, 7L, 14L, 30L)

    val SECOND_CHANCE_DELAY: Duration = Duration.ofHours(1)

    fun intervalDays(level: Int): Long = INTERVAL_DAYS[level.coerceIn(MIN_LEVEL, MAX_LEVEL) - 1]

    /** Planned review due / second chance unlocked - NEW ones wait for the daily limit (FlashcardsReview) */
    fun isRepetitionDue(flashcard: FlashcardDTO, today: LocalDate, now: Instant): Boolean =
        when (flashcard.status) {
            FlashcardStatus.ACTIVE -> flashcard.dueOn?.let { dueOn -> !dueOn.isAfter(today) } ?: true
            // ! An unused second chance just waits - the next day it is still a second chance, no level lost
            FlashcardStatus.FROZEN -> flashcard.frozenUntil?.let { frozenUntil -> !frozenUntil.isAfter(now) } ?: true
            FlashcardStatus.NEW,
            FlashcardStatus.MASTERED -> false
        }

    fun rate(flashcard: FlashcardDTO, rating: FlashcardRating, today: LocalDate, now: Instant): FlashcardDTO =
        when (flashcard.status) {
            FlashcardStatus.NEW      -> rateNew(flashcard, rating, today, now)
            FlashcardStatus.ACTIVE   -> ratePlanned(flashcard, rating, today, now)
            FlashcardStatus.FROZEN   -> rateSecondChance(flashcard, rating, today, now)
            FlashcardStatus.MASTERED -> flashcard
        }

    private fun rateNew(flashcard: FlashcardDTO, rating: FlashcardRating, today: LocalDate, now: Instant): FlashcardDTO {
        val introduced = flashcard.copy(level = MIN_LEVEL, introducedOn = today)
        return when (rating) {
            FlashcardRating.GOOD,
            FlashcardRating.OK  -> introduced.planned(MIN_LEVEL, today)
            FlashcardRating.BAD -> introduced.frozen(now)
        }
    }

    private fun ratePlanned(flashcard: FlashcardDTO, rating: FlashcardRating, today: LocalDate, now: Instant): FlashcardDTO {
        val level = flashcard.level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        return when (rating) {
            FlashcardRating.GOOD ->
                if (level == MAX_LEVEL) flashcard.copy(status = FlashcardStatus.MASTERED, level = MAX_LEVEL, dueOn = null, frozenUntil = null)
                else                    flashcard.planned(level + 1, today)
            FlashcardRating.OK   -> flashcard.planned(level, today)
            // Level on hold - the second chance decides about it
            FlashcardRating.BAD  -> flashcard.copy(level = level).frozen(now)
        }
    }

    private fun rateSecondChance(flashcard: FlashcardDTO, rating: FlashcardRating, today: LocalDate, now: Instant): FlashcardDTO {
        val level = flashcard.level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        val lower = (level - 1).coerceAtLeast(MIN_LEVEL)
        return when (rating) {
            // A fix saves the level, but does not raise it
            FlashcardRating.GOOD -> flashcard.planned(level, today)
            FlashcardRating.OK   -> flashcard.planned(lower, today)
            FlashcardRating.BAD  -> flashcard.copy(level = lower).frozen(now)
        }
    }

    private fun FlashcardDTO.planned(level: Int, today: LocalDate): FlashcardDTO =
        copy(
            status      = FlashcardStatus.ACTIVE,
            level       = level,
            dueOn       = today.plusDays(intervalDays(level)),
            frozenUntil = null
        )

    private fun FlashcardDTO.frozen(now: Instant): FlashcardDTO =
        copy(
            status      = FlashcardStatus.FROZEN,
            dueOn       = null,
            frozenUntil = now.plus(SECOND_CHANCE_DELAY)
        )
}
