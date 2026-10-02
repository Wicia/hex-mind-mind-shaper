package pl.hexmind.mindshaper.services

import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSessionState

/**
 * Review session of one set's flashcards - pure operations on the list, the caller stores the result.
 * The whole session state lives in the flashcards themselves (state + queue order), so it survives
 * leaving the screen: a revealed but unanswered flashcard is shown again, revealed, until answered.
 *
 * TODO: step C - start only with the flashcards due today (repetition schedule), not all of them
 */
object FlashcardsSession {

    fun isInProgress(flashcards: List<FlashcardDTO>): Boolean =
        flashcards.any { it.sessionState == FlashcardSessionState.QUEUED || it.sessionState == FlashcardSessionState.REVEALED }

    /** Index of the flashcard to show: the revealed one first, otherwise the head of the queue. */
    fun currentIndex(flashcards: List<FlashcardDTO>): Int? {
        val revealed = flashcards.indexOfFirst { it.sessionState == FlashcardSessionState.REVEALED }
        if (revealed >= 0) return revealed

        return flashcards.indices
            .filter { flashcards[it].sessionState == FlashcardSessionState.QUEUED }
            .minByOrNull { flashcards[it].sessionOrder ?: 0 }
    }

    fun queuedCount(flashcards: List<FlashcardDTO>): Int =
        flashcards.count { it.sessionState == FlashcardSessionState.QUEUED }

    /** Flashcards of the current (or the last) session. */
    fun sessionSize(flashcards: List<FlashcardDTO>): Int =
        flashcards.count { it.sessionState != null }

    fun answeredCount(flashcards: List<FlashcardDTO>): Int =
        flashcards.count { it.sessionState == FlashcardSessionState.PASSED || it.sessionState == FlashcardSessionState.FAILED }

    /** Passed out of all the session's flashcards - null when there was no session yet. */
    fun masteryPercent(flashcards: List<FlashcardDTO>): Int? {
        val size = sessionSize(flashcards)
        if (size == 0) return null

        val passed = flashcards.count { it.sessionState == FlashcardSessionState.PASSED }
        return passed * 100 / size
    }

    /** All flashcards, in random order - the previous session's results are dropped. */
    fun start(flashcards: List<FlashcardDTO>): List<FlashcardDTO> {
        val order = flashcards.indices.shuffled()
        return flashcards.mapIndexed { index, flashcard ->
            flashcard.copy(sessionState = FlashcardSessionState.QUEUED, sessionOrder = order[index])
        }
    }

    fun reveal(flashcards: List<FlashcardDTO>, index: Int): List<FlashcardDTO> =
        flashcards.replaceAt(index) { it.copy(sessionState = FlashcardSessionState.REVEALED) }

    /** Moves the flashcard to the end of the queue - it comes back once the others are done. */
    fun skip(flashcards: List<FlashcardDTO>, index: Int): List<FlashcardDTO> {
        val lastOrder = flashcards.maxOf { it.sessionOrder ?: 0 }
        return flashcards.replaceAt(index) { it.copy(sessionOrder = lastOrder + 1) }
    }

    fun answer(flashcards: List<FlashcardDTO>, index: Int, isCorrect: Boolean): List<FlashcardDTO> =
        flashcards.replaceAt(index) {
            if (isCorrect) it.copy(sessionState = FlashcardSessionState.PASSED, correctCount = it.correctCount + 1)
            else           it.copy(sessionState = FlashcardSessionState.FAILED, wrongCount = it.wrongCount + 1)
        }

    private fun List<FlashcardDTO>.replaceAt(index: Int, change: (FlashcardDTO) -> FlashcardDTO): List<FlashcardDTO> =
        mapIndexed { i, flashcard -> if (i == index) change(flashcard) else flashcard }
}
