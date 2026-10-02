package pl.hexmind.mindshaper.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.room.withTransaction
import pl.hexmind.mindshaper.database.AppDatabase
import pl.hexmind.mindshaper.database.models.FlashcardEntity
import pl.hexmind.mindshaper.database.models.FlashcardSessionUpdate
import pl.hexmind.mindshaper.database.models.FlashcardSetEntity
import pl.hexmind.mindshaper.database.models.FlashcardSetWithCards
import pl.hexmind.mindshaper.database.repositories.FlashcardDAO
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardSessionState
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardsService @Inject constructor(
    private val database: AppDatabase,
    private val flashcardDAO: FlashcardDAO
) {

    fun getAllSetsLive(): LiveData<List<FlashcardSetDTO>> =
        flashcardDAO.getAllSetsLive().map { sets -> sets.map { set -> toDto(set) } }

    fun getSetByIdLive(setId: Int): LiveData<FlashcardSetDTO?> =
        flashcardDAO.getSetByIdLive(setId).map { set -> set?.let { toDto(it) } }

    /** @return id of the new set */
    suspend fun addSet(name: String, flashcards: List<FlashcardDTO>): Int =
        database.withTransaction {
            val setId = flashcardDAO.insertSet(FlashcardSetEntity(name = name)).toInt()
            flashcardDAO.replaceFlashcards(setId, toEntities(setId, flashcards))
            setId
        }

    /**
     * Name + the whole list at once.
     * ! Kept flashcards come back with their id - the answer counters and session state stay with them
     */
    suspend fun updateSet(setId: Int, name: String, flashcards: List<FlashcardDTO>) {
        database.withTransaction {
            flashcardDAO.updateSetName(setId, name, Instant.now().toEpochMilli())
            flashcardDAO.replaceFlashcards(setId, toEntities(setId, flashcards))
        }
    }

    /** Added at the end of the set - the order the user enters them in */
    suspend fun addFlashcard(setId: Int, flashcard: FlashcardDTO) {
        database.withTransaction {
            val position = flashcardDAO.getLastPosition(setId) + 1
            flashcardDAO.insert(toEntity(setId, position, flashcard))
            flashcardDAO.touchSet(setId, Instant.now().toEpochMilli())
        }
    }

    /**
     * One flashcard - the rest of the set stays untouched.
     * ! Counters and session state are taken from the dto - a changed question comes with them reset
     */
    suspend fun updateFlashcard(setId: Int, flashcard: FlashcardDTO) {
        val id = flashcard.id ?: return
        database.withTransaction {
            flashcardDAO.updateContent(
                id           = id,
                front        = flashcard.front,
                back         = flashcard.back,
                correctCount = flashcard.correctCount,
                wrongCount   = flashcard.wrongCount,
                sessionState = flashcard.sessionState?.name,
                sessionOrder = flashcard.sessionOrder
            )
            flashcardDAO.touchSet(setId, Instant.now().toEpochMilli())
        }
    }

    /** Gap in the positions is fine - they only order the set */
    suspend fun deleteFlashcard(setId: Int, flashcardId: Int) {
        database.withTransaction {
            flashcardDAO.deleteFlashcard(flashcardId)
            flashcardDAO.touchSet(setId, Instant.now().toEpochMilli())
        }
    }

    suspend fun deleteSet(setId: Int) {
        flashcardDAO.deleteSet(setId)
    }

    /**
     * Review progress only (counters + session state) - content and order of the flashcards stay.
     * ! Reviewing is not editing - updated_at stays, so the set does not jump in the list order
     */
    suspend fun updateSession(flashcards: List<FlashcardDTO>) {
        val updates = flashcards.mapNotNull { flashcard ->
            val id = flashcard.id ?: return@mapNotNull null
            FlashcardSessionUpdate(
                id           = id,
                correctCount = flashcard.correctCount,
                wrongCount   = flashcard.wrongCount,
                sessionState = flashcard.sessionState?.name,
                sessionOrder = flashcard.sessionOrder
            )
        }
        flashcardDAO.updateSession(updates)
    }

    private fun toDto(setWithCards: FlashcardSetWithCards): FlashcardSetDTO =
        FlashcardSetDTO(
            id        = setWithCards.set.id,
            name      = setWithCards.set.name,
            createdAt = setWithCards.set.createdAt,
            updatedAt = setWithCards.set.updatedAt,
            flashcards = setWithCards.flashcards
                .sortedBy { flashcard -> flashcard.position }
                .map { flashcard ->
                    FlashcardDTO(
                        id           = flashcard.id,
                        front        = flashcard.front,
                        back         = flashcard.back,
                        correctCount = flashcard.correctCount,
                        wrongCount   = flashcard.wrongCount,
                        sessionState = flashcard.sessionState?.let { state -> FlashcardSessionState.valueOf(state) },
                        sessionOrder = flashcard.sessionOrder
                    )
                }
        )

    private fun toEntities(setId: Int, flashcards: List<FlashcardDTO>): List<FlashcardEntity> =
        flashcards.mapIndexed { index, flashcard -> toEntity(setId, index, flashcard) }

    private fun toEntity(setId: Int, position: Int, flashcard: FlashcardDTO): FlashcardEntity =
        FlashcardEntity(
            id           = flashcard.id,
            setId        = setId,
            position     = position,
            front        = flashcard.front,
            back         = flashcard.back,
            correctCount = flashcard.correctCount,
            wrongCount   = flashcard.wrongCount,
            sessionState = flashcard.sessionState?.name,
            sessionOrder = flashcard.sessionOrder
        )
}
