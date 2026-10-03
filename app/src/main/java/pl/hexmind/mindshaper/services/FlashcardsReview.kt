package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import java.time.Instant
import java.time.LocalDate

/**
 * What is up for review right now - one session over all the sets (or just one of them).
 * Nothing is stored about the session itself: the queue comes from the flashcards' progress each time,
 * so leaving the session halfway loses nothing - the answered ones are already rescheduled.
 *
 * Order: second chances -> planned reviews (lowest level first) -> new flashcards.
 * New ones: at most [Params.newPerDay] a day, none while the due reviews exceed [Params.backlogThreshold].
 */
object FlashcardsReview {

    data class Params(
        val newPerDay: Int,
        val backlogThreshold: Int
    )

    /** Flashcard + its set - a set's own session and the order of new ones go by it */
    data class Card(
        val setId: Int,
        val flashcard: FlashcardDTO
    )

    data class Plan(
        val queue: List<Card>,
        val repetitionsCount: Int, // Second chances + planned reviews in the queue
        val newCount: Int,         // New flashcards in the queue
        val newPausedBacklog: Int  // > 0 = new flashcards held back, this many reviews to catch up first
    ) {
        val isEmpty: Boolean get() = queue.isEmpty()
    }

    data class SetStats(
        val dueCount: Int, // Second chances + planned reviews
        val newCount: Int,
        val masteredCount: Int
    )

    fun cardsOf(sets: List<FlashcardSetDTO>): List<Card> =
        sets.flatMap { set ->
            val setId = set.id ?: return@flatMap emptyList()
            set.flashcards.map { flashcard -> Card(setId, flashcard) }
        }

    /**
     * @param setId null = all the sets
     * ! The daily limit and the backlog are counted over all the sets - a set's own session follows the same rules
     */
    fun plan(sets: List<FlashcardSetDTO>, params: Params, today: LocalDate, now: Instant, setId: Int? = null): Plan {
        val allCards = cardsOf(sets)
        val cards = if (setId == null) allCards else allCards.filter { card -> card.setId == setId }

        val secondChances = cards
            .filter { card -> card.flashcard.status == FlashcardStatus.FROZEN && isDue(card, today, now) }
            .sortedBy { card -> card.flashcard.level }
        val planned = cards
            .filter { card -> card.flashcard.status == FlashcardStatus.ACTIVE && isDue(card, today, now) }
            .sortedWith(compareBy<Card> { card -> card.flashcard.level }.thenBy { card -> card.flashcard.dueOn })

        val backlog = allCards.count { card -> isDue(card, today, now) }
        val introducedToday = allCards.count { card -> card.flashcard.introducedOn == today }
        val isBacklogged = backlog > params.backlogThreshold
        val newAllowed = if (isBacklogged) 0 else (params.newPerDay - introducedToday).coerceAtLeast(0)

        // Order of entering: older sets first (the list is by last edit), within a set its own order (stable sort)
        val newCards = cards
            .filter { card -> card.flashcard.status == FlashcardStatus.NEW }
            .sortedBy { card -> card.setId }
            .take(newAllowed)

        val hasNewWaiting = cards.any { card -> card.flashcard.status == FlashcardStatus.NEW }

        return Plan(
            queue            = secondChances + planned + newCards,
            repetitionsCount = secondChances.size + planned.size,
            newCount         = newCards.size,
            newPausedBacklog = if (isBacklogged && hasNewWaiting) backlog - params.backlogThreshold else 0
        )
    }

    fun statsOf(set: FlashcardSetDTO, today: LocalDate, now: Instant): SetStats =
        SetStats(
            dueCount      = set.flashcards.count { flashcard -> FlashcardsScheduler.isRepetitionDue(flashcard, today, now) },
            newCount      = set.flashcards.count { flashcard -> flashcard.status == FlashcardStatus.NEW },
            masteredCount = set.flashcards.count { flashcard -> flashcard.status == FlashcardStatus.MASTERED }
        )

    private fun isDue(card: Card, today: LocalDate, now: Instant): Boolean =
        FlashcardsScheduler.isRepetitionDue(card.flashcard, today, now)
}
