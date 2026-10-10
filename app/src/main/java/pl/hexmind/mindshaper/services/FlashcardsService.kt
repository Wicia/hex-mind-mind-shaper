package pl.hexmind.mindshaper.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.room.withTransaction
import pl.hexmind.mindshaper.database.AppDatabase
import pl.hexmind.mindshaper.database.models.FlashcardEntity
import pl.hexmind.mindshaper.database.models.FlashcardProgressUpdate
import pl.hexmind.mindshaper.database.models.FlashcardSetEntity
import pl.hexmind.mindshaper.database.models.FlashcardSetWithCards
import pl.hexmind.mindshaper.database.repositories.FlashcardDAO
import pl.hexmind.mindshaper.services.dto.FlashcardDTO
import pl.hexmind.mindshaper.services.dto.FlashcardRating
import pl.hexmind.mindshaper.services.dto.FlashcardSetDTO
import pl.hexmind.mindshaper.services.dto.FlashcardStatus
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardsService @Inject constructor(
    private val database: AppDatabase,
    private val flashcardDAO: FlashcardDAO,
    private val appSettingsStorage: AppSettingsStorage
) {

    fun getAllSetsLive(): LiveData<List<FlashcardSetDTO>> =
        flashcardDAO.getAllSetsLive().map { sets -> sets.map { set -> toDto(set) } }

    fun getSetByIdLive(setId: Int): LiveData<FlashcardSetDTO?> =
        flashcardDAO.getSetByIdLive(setId).map { set -> set?.let { toDto(it) } }

    suspend fun getAllSets(): List<FlashcardSetDTO> =
        flashcardDAO.getAllSetsWithCards().map { set -> toDto(set) }

    /**
     * What is up for review right now - with the user's limits (Settings).
     * @param sets all the sets - the daily limit of new flashcards and the backlog count over all of them
     * @param setId null = one session over all the sets
     */
    fun planReview(sets: List<FlashcardSetDTO>, setId: Int? = null): FlashcardsReview.Plan =
        FlashcardsReview.plan(
            sets   = sets,
            params = FlashcardsReview.Params(
                newPerDay        = appSettingsStorage.getFlashcardsNewPerDay(),
                backlogThreshold = AppSettingsStorage.FLASHCARDS_BACKLOG_THRESHOLD
            ),
            today  = LocalDate.now(),
            setId  = setId
        )

    fun statsOf(set: FlashcardSetDTO): FlashcardsReview.SetStats =
        FlashcardsReview.statsOf(set, LocalDate.now())

    /** @return id of the new set */
    suspend fun addSet(name: String, flashcards: List<FlashcardDTO>): Int =
        database.withTransaction {
            val setId = flashcardDAO.insertSet(FlashcardSetEntity(name = name)).toInt()
            flashcardDAO.replaceFlashcards(setId, toEntities(setId, flashcards))
            setId
        }

    /**
     * Name + the whole list at once.
     * ! Kept flashcards come back with their id and progress - editing does not reset the repetitions
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

    /** Content only - the rest of the set and the flashcard's repetition progress stay untouched */
    suspend fun updateFlashcard(setId: Int, flashcard: FlashcardDTO) {
        val id = flashcard.id ?: return
        database.withTransaction {
            flashcardDAO.updateContent(id, flashcard.front, flashcard.back)
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

    /** First rating of the flashcard in a review session -> its next level / review date (FlashcardsScheduler) */
    suspend fun rate(flashcard: FlashcardDTO, rating: FlashcardRating) {
        updateProgress(FlashcardsScheduler.rate(flashcard, rating, LocalDate.now()))
    }

    /**
     * Repetition progress only - content and place of the flashcard stay.
     * ! Reviewing is not editing - updated_at stays, so the set does not jump in the list order
     */
    private suspend fun updateProgress(flashcard: FlashcardDTO) {
        val id = flashcard.id ?: return
        flashcardDAO.updateProgress(
            FlashcardProgressUpdate(
                id           = id,
                status       = flashcard.status.name,
                level        = flashcard.level,
                dueOn        = flashcard.dueOn?.toEpochDay(),
                introducedOn = flashcard.introducedOn?.toEpochDay()
            )
        )
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
                        status       = FlashcardStatus.valueOf(flashcard.status),
                        level        = flashcard.level,
                        dueOn        = flashcard.dueOn?.let { day -> LocalDate.ofEpochDay(day) },
                        introducedOn = flashcard.introducedOn?.let { day -> LocalDate.ofEpochDay(day) }
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
            status       = flashcard.status.name,
            level        = flashcard.level,
            dueOn        = flashcard.dueOn?.toEpochDay(),
            introducedOn = flashcard.introducedOn?.toEpochDay()
        )
}
