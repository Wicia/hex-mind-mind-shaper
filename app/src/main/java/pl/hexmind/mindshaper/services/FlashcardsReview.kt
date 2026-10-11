package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import pl.hexmind.mindshaper.services.dto.FlashcardsLibraryDTO
import java.time.LocalDate
import kotlin.random.Random

/**
 * What is up for review right now - one session over all the flashcards, sets + loose ones (or just one set).
 * Nothing is stored about the session itself: the queue comes from the flashcards' progress each time,
 * so leaving the session halfway loses nothing - the answered ones are already rescheduled.
 *
 * Order: planned reviews (lowest level first, random within a level) -> new flashcards.
 * New ones: at most [Params.newPerDay] a day, none while the due reviews exceed [Params.backlogThreshold].
 */
object FlashcardsReview {

    data class Params(
        val newPerDay: Int,
        val backlogThreshold: Int
    )

    /** Flashcard + its set (null = loose) - a set's own session and the order of new ones go by it */
    data class Card(
        val setId: Int?,
        val flashcard: FlashcardDTO
    )

    data class Plan(
        val queue: List<Card>,
        val repetitionsCount: Int, // Planned reviews in the queue
        val newCount: Int,         // New flashcards in the queue
        val newPausedBacklog: Int  // > 0 = new flashcards held back, this many reviews to catch up first
    ) {
        val isEmpty: Boolean get() = queue.isEmpty()
    }

    data class SetStats(
        val dueCount: Int,
        val masteredCount: Int
    )

    fun cardsOf(library: FlashcardsLibraryDTO): List<Card> =
        library.loose.map { flashcard -> Card(null, flashcard) } +
            library.sets.flatMap { set ->
                val setId = set.id ?: return@flatMap emptyList()
                set.flashcards.map { flashcard -> Card(setId, flashcard) }
            }

    /**
     * @param setId null = all the flashcards (sets + loose)
     * @param random order within a level - so the user does not learn what comes after what
     * ! The daily limit and the backlog are counted over all the flashcards - a set's own session follows the same rules
     */
    fun plan(
        library: FlashcardsLibraryDTO,
        params: Params,
        today: LocalDate,
        setId: Int? = null,
        random: Random = Random.Default
    ): Plan {
        val allCards = cardsOf(library)
        val cards = if (setId == null) allCards else allCards.filter { card -> card.setId == setId }

        // Shuffle first, then the stable sort keeps the random order within a level
        val planned = cards
            .filter { card -> isDue(card, today) }
            .shuffled(random)
            .sortedBy { card -> card.flashcard.level }

        val backlog = allCards.count { card -> isDue(card, today) }
        val introducedToday = allCards.count { card -> card.flashcard.introducedOn == today }
        val isBacklogged = backlog > params.backlogThreshold
        val newAllowed = if (isBacklogged) 0 else (params.newPerDay - introducedToday).coerceAtLeast(0)

        // Order of entering: loose ones first (jotted down on the go), then older sets first (the list is by last edit),
        // within a set its own order (stable sort)
        val newCards = cards
            .filter { card -> card.flashcard.status == FlashcardStatus.NEW }
            .sortedBy { card -> card.setId ?: Int.MIN_VALUE }
            .take(newAllowed)

        val hasNewWaiting = cards.any { card -> card.flashcard.status == FlashcardStatus.NEW }

        return Plan(
            queue            = planned + newCards,
            repetitionsCount = planned.size,
            newCount         = newCards.size,
            newPausedBacklog = if (isBacklogged && hasNewWaiting) backlog - params.backlogThreshold else 0
        )
    }

    fun statsOf(set: FlashcardSetDTO, today: LocalDate): SetStats =
        SetStats(
            dueCount      = set.flashcards.count { flashcard -> FlashcardsScheduler.isRepetitionDue(flashcard, today) },
            masteredCount = set.flashcards.count { flashcard -> flashcard.status == FlashcardStatus.MASTERED }
        )

    private fun isDue(card: Card, today: LocalDate): Boolean =
        FlashcardsScheduler.isRepetitionDue(card.flashcard, today)
}
